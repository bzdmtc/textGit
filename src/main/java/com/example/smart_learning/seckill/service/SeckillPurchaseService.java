package com.example.smart_learning.seckill.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.security.SecurityUtils;
import com.example.smart_learning.seckill.config.SeckillKafkaConfig;
import com.example.smart_learning.seckill.dto.SeckillClaimResult;
import com.example.smart_learning.seckill.dto.SeckillOrderMessage;
import com.example.smart_learning.seckill.dto.SeckillResultDto;
import com.example.smart_learning.seckill.entity.SeckillActivity;
import com.example.smart_learning.seckill.entity.SeckillRecord;
import com.example.smart_learning.seckill.repository.SeckillActivityRepository;
import com.example.smart_learning.seckill.repository.SeckillRecordRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Master
public class SeckillPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(SeckillPurchaseService.class);
    private static final long SEND_TIMEOUT_SECONDS = 6;

    private final SeckillActivityRepository activityRepository;
    private final SeckillRecordRepository recordRepository;
    private final UserCourseRepository userCourseRepository;
    private final SeckillStockService stockService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SeckillPurchaseService(SeckillActivityRepository activityRepository,
                                  SeckillRecordRepository recordRepository,
                                  UserCourseRepository userCourseRepository,
                                  SeckillStockService stockService,
                                  @Qualifier("seckillKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.recordRepository = recordRepository;
        this.userCourseRepository = userCourseRepository;
        this.stockService = stockService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public SeckillClaimResult claim(Long activityId) {
        Long userId = requireUserId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        SeckillActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));

        if (!"ON".equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.SECKILL_STATUS_INVALID, "该秒杀活动已停用");
        }
        if (now.isBefore(activity.getStartAt())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndAt())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
        if (recordRepository.existsByUserIdAndActivityId(userId, activityId)) {
            throw new BusinessException(ErrorCode.SECKILL_ALREADY_JOINED);
        }
        if (userCourseRepository.existsByUserIdAndCourseId(userId, activity.getCourseId())) {
            throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
        }

        if (!stockService.stockExists(activityId)) {
            stockService.warmUp(activity);
        }
        long result = stockService.claim(activity, userId, now.toInstant(ZoneOffset.UTC).toEpochMilli());
        if (result == SeckillStockService.NOT_STARTED) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (result == SeckillStockService.ENDED) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
        if (result == SeckillStockService.LIMIT_REACHED) {
            throw new BusinessException(ErrorCode.SECKILL_LIMIT_REACHED);
        }
        if (result == SeckillStockService.SOLD_OUT) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
        }
        if (result == SeckillStockService.NOT_WARMED) {
            throw new BusinessException(ErrorCode.SECKILL_UNAVAILABLE);
        }

        String requestId = UUID.randomUUID().toString();
        SeckillOrderMessage message = new SeckillOrderMessage(activityId, userId, requestId);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (JacksonException ex) {
            stockService.restoreAll(activityId, userId);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        try {
            kafkaTemplate.send(SeckillKafkaConfig.ORDER_TOPIC, String.valueOf(activityId), payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("秒杀请求已入队 activity={} user={} request={}", activityId, userId, requestId);
            return new SeckillClaimResult(activityId, "QUEUED", "抢购成功，正在为您生成订单", now);
        } catch (ExecutionException ex) {
            // 明确发送失败：回补 Redis 库存与限购名额
            log.error("秒杀消息发送失败 activity={} user={}", activityId, userId, ex.getCause());
            stockService.restoreAll(activityId, userId);
            throw new BusinessException(ErrorCode.SECKILL_UNAVAILABLE);
        } catch (TimeoutException ex) {
            // 结果不确定：保留排队状态，让消费者/结果轮询兜底，避免重复扣减
            log.warn("秒杀消息发送超时，保持排队 activity={} user={} request={}", activityId, userId, requestId);
            return new SeckillClaimResult(activityId, "QUEUED", "抢购请求已提交，请稍候查看结果", now);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("秒杀消息发送被中断 activity={} user={}", activityId, userId);
            throw new BusinessException(ErrorCode.SECKILL_UNAVAILABLE);
        } catch (RuntimeException ex) {
            // 同步发送异常（如元数据获取失败）可视为确定未入队
            log.error("秒杀消息发送异常 activity={} user={}", activityId, userId, ex);
            stockService.restoreAll(activityId, userId);
            throw new BusinessException(ErrorCode.SECKILL_UNAVAILABLE);
        }
    }

    public SeckillResultDto result(Long activityId) {
        Long userId = requireUserId();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        SeckillActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));
        SeckillRecord record = recordRepository.findByUserIdAndActivityId(userId, activityId).orElse(null);
        String phase = SeckillPhase.of(activity, now);
        if (record != null) {
            return new SeckillResultDto(activityId, record.getStatus(), record.getOrderNo(),
                    record.getReason(), phase, now);
        }
        String status = stockService.hasClaimed(activityId, userId) ? "QUEUED" : "NOT_JOINED";
        return new SeckillResultDto(activityId, status, null, null, phase, now);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
