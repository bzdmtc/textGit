package com.example.smart_learning.seckill.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.entity.OrderItem;
import com.example.smart_learning.order.repository.OrderItemRepository;
import com.example.smart_learning.order.repository.OrderRepository;
import com.example.smart_learning.order.service.OrderNumbers;
import com.example.smart_learning.seckill.dto.SeckillOrderMessage;
import com.example.smart_learning.seckill.entity.SeckillActivity;
import com.example.smart_learning.seckill.entity.SeckillRecord;
import com.example.smart_learning.seckill.repository.SeckillActivityRepository;
import com.example.smart_learning.seckill.repository.SeckillRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 秒杀建单消费者核心逻辑。Kafka 至少一次投递 + DB 唯一约束 + 本方法前置存在性判断，
 * 保证同一用户同一活动只会生成一笔订单。
 */
@Service
@Master
public class SeckillOrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderProcessor.class);
    private static final int PAY_WINDOW_MINUTES = 15;

    public enum Outcome {
        CREATED,
        FAILED,
        DUPLICATE,
        NO_ACTIVITY
    }

    public record Result(Outcome outcome, String orderNo, Long activityId, Long userId) {
    }

    private final SeckillActivityRepository activityRepository;
    private final SeckillRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderNumbers orderNumbers;

    public SeckillOrderProcessor(SeckillActivityRepository activityRepository,
                                 SeckillRecordRepository recordRepository,
                                 CourseRepository courseRepository,
                                 UserCourseRepository userCourseRepository,
                                 OrderRepository orderRepository,
                                 OrderItemRepository orderItemRepository,
                                 OrderNumbers orderNumbers) {
        this.activityRepository = activityRepository;
        this.recordRepository = recordRepository;
        this.courseRepository = courseRepository;
        this.userCourseRepository = userCourseRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderNumbers = orderNumbers;
    }

    @Transactional
    public Result process(SeckillOrderMessage message) {
        Long activityId = message.activityId();
        Long userId = message.userId();
        if (activityId == null || userId == null) {
            throw new IllegalArgumentException("秒杀消息缺少 activityId/userId");
        }
        if (recordRepository.existsByUserIdAndActivityId(userId, activityId)) {
            log.info("秒杀消息幂等跳过 activity={} user={} request={}",
                    activityId, userId, message.requestId());
            return new Result(Outcome.DUPLICATE, null, activityId, userId);
        }
        SeckillActivity activity = activityRepository.findById(activityId).orElse(null);
        if (activity == null) {
            log.warn("秒杀活动不存在 activity={} user={}", activityId, userId);
            return new Result(Outcome.NO_ACTIVITY, null, activityId, userId);
        }

        Course course = courseRepository.findById(activity.getCourseId()).orElse(null);
        if (course == null || !"PUBLISHED".equals(course.getStatus())) {
            return fail(activity, userId, "秒杀课程不存在或已下架");
        }
        if (userCourseRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            return fail(activity, userId, "您已拥有该课程");
        }
        if (!orderRepository.findPendingOrderNos(userId, course.getId()).isEmpty()) {
            return fail(activity, userId, "该课程已有待支付订单");
        }

        int updated = activityRepository.decreaseStock(activityId);
        if (updated == 0) {
            return fail(activity, userId, "秒杀名额已抢光");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Order order = new Order();
        order.setOrderNo(orderNumbers.next());
        order.setUserId(userId);
        order.setTotalAmount(activity.getSeckillPrice());
        order.setDiscountAmount(java.math.BigDecimal.ZERO);
        order.setPayAmount(activity.getSeckillPrice());
        order.setOrderSource("SECKILL");
        order.setSeckillActivityId(activityId);
        order.setStatus("PENDING_PAYMENT");
        order.setExpireAt(now.plusMinutes(PAY_WINDOW_MINUTES));
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setCourseId(course.getId());
        item.setCourseTitleSnapshot(course.getTitle());
        item.setCoverSnapshot(course.getCoverUrl());
        item.setSubjectSnapshot(course.getSubject());
        item.setPriceSnapshot(activity.getSeckillPrice());
        orderItemRepository.save(item);

        SeckillRecord record = new SeckillRecord();
        record.setUserId(userId);
        record.setActivityId(activityId);
        record.setOrderNo(order.getOrderNo());
        record.setStatus("CREATED");
        recordRepository.save(record);

        log.info("秒杀建单成功 activity={} user={} order={} request={}",
                activityId, userId, order.getOrderNo(), message.requestId());
        return new Result(Outcome.CREATED, order.getOrderNo(), activityId, userId);
    }

    private Result fail(SeckillActivity activity, Long userId, String reason) {
        SeckillRecord record = new SeckillRecord();
        record.setUserId(userId);
        record.setActivityId(activity.getId());
        record.setStatus("FAILED");
        record.setReason(reason);
        recordRepository.save(record);
        log.info("秒杀建单失败 activity={} user={} reason={}", activity.getId(), userId, reason);
        return new Result(Outcome.FAILED, null, activity.getId(), userId);
    }
}
