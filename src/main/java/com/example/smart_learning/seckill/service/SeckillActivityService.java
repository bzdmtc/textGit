package com.example.smart_learning.seckill.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.seckill.dto.PublicSeckillDto;
import com.example.smart_learning.seckill.dto.SeckillActivityDto;
import com.example.smart_learning.seckill.dto.SeckillActivityRequest;
import com.example.smart_learning.seckill.dto.SeckillRecordDto;
import com.example.smart_learning.seckill.entity.SeckillActivity;
import com.example.smart_learning.seckill.entity.SeckillRecord;
import com.example.smart_learning.seckill.repository.SeckillActivityRepository;
import com.example.smart_learning.seckill.repository.SeckillRecordRepository;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Master
public class SeckillActivityService {

    private final SeckillActivityRepository activityRepository;
    private final SeckillRecordRepository recordRepository;
    private final CourseRepository courseRepository;
    private final SysUserRepository userRepository;
    private final SeckillStockService stockService;

    public SeckillActivityService(SeckillActivityRepository activityRepository,
                                  SeckillRecordRepository recordRepository,
                                  CourseRepository courseRepository,
                                  SysUserRepository userRepository,
                                  SeckillStockService stockService) {
        this.activityRepository = activityRepository;
        this.recordRepository = recordRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.stockService = stockService;
    }

    @Transactional(readOnly = true)
    public PageResult<SeckillActivityDto> page(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<SeckillActivity> result = activityRepository.searchAdmin(status, pageable);
        Map<Long, Course> courses = loadCourses(result.getContent());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<SeckillActivityDto> items = result.getContent().stream()
                .map(activity -> toDto(activity, courses.get(activity.getCourseId()), now))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public List<PublicSeckillDto> visible() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<SeckillActivity> activities = activityRepository.findVisible(now);
        Map<Long, Course> courses = loadCourses(activities);
        return activities.stream().map(activity -> {
            Course course = courses.get(activity.getCourseId());
            Integer redisStock = stockService.remaining(activity.getId());
            int remaining = redisStock == null ? activity.getAvailableStock() : redisStock;
            long sold = Math.max(0, activity.getTotalStock() - remaining);
            return new PublicSeckillDto(
                    activity.getId(), activity.getCourseId(),
                    course == null ? "课程已下架" : course.getTitle(),
                    course == null ? null : course.getSubtitle(),
                    course == null ? null : course.getCoverUrl(),
                    course == null ? null : course.getSubject(),
                    course == null ? null : course.getPrice(),
                    activity.getSeckillPrice(),
                    activity.getTotalStock(), remaining, sold,
                    activity.getPerUserLimit(),
                    activity.getStartAt(), activity.getEndAt(),
                    SeckillPhase.of(activity, now), now);
        }).toList();
    }

    @Transactional
    public SeckillActivityDto create(SeckillActivityRequest request) {
        Course course = validateRequest(request);
        SeckillActivity activity = new SeckillActivity();
        activity.setCourseId(course.getId());
        activity.setSeckillPrice(request.seckillPrice());
        activity.setTotalStock(request.totalStock());
        activity.setAvailableStock(request.totalStock());
        activity.setPerUserLimit(1);
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setStatus(request.status() == null ? "ON" : request.status());
        activity = activityRepository.save(activity);
        if ("ON".equals(activity.getStatus())) {
            stockService.warmUp(activity);
        }
        return toDto(activity, course, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public SeckillActivityDto update(Long id, SeckillActivityRequest request) {
        SeckillActivity activity = activityRepository.findForUpdateById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));
        Course course = validateRequest(request);

        int sold = activity.getTotalStock() - activity.getAvailableStock();
        if (request.totalStock() < sold) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_INVALID,
                    "总名额不能小于已售数量 " + sold);
        }
        int delta = request.totalStock() - activity.getTotalStock();
        int newAvailable = activity.getAvailableStock() + delta;
        if (newAvailable < 0) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_INVALID);
        }

        activity.setCourseId(course.getId());
        activity.setSeckillPrice(request.seckillPrice());
        activity.setTotalStock(request.totalStock());
        activity.setAvailableStock(newAvailable);
        activity.setPerUserLimit(1);
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        if (request.status() != null) {
            activity.setStatus(request.status());
        }
        activity = activityRepository.save(activity);

        if ("ON".equals(activity.getStatus())) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            boolean active = SeckillPhase.ACTIVE.equals(SeckillPhase.of(activity, now));
            if (active && delta != 0 && stockService.stockExists(id)) {
                stockService.adjustStock(id, delta);
            } else {
                stockService.warmUp(activity);
            }
        } else {
            stockService.clearStock(id);
        }
        return toDto(activity, course, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public SeckillActivityDto setStatus(Long id, String status) {
        SeckillActivity activity = activityRepository.findForUpdateById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));
        activity.setStatus(status);
        activity = activityRepository.save(activity);
        if ("ON".equals(status)) {
            stockService.warmUp(activity);
        } else {
            stockService.clearStock(id);
        }
        Course course = courseRepository.findById(activity.getCourseId()).orElse(null);
        return toDto(activity, course, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void delete(Long id) {
        SeckillActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));
        long records = recordRepository.countByActivityIdAndStatusIn(id, List.of("CREATED", "PAID", "CANCELED", "FAILED"));
        if (records > 0) {
            throw new BusinessException(ErrorCode.SECKILL_STATUS_INVALID, "该活动已有抢购记录，不能删除，只能停用");
        }
        activityRepository.delete(activity);
        stockService.clear(id);
    }

    /** 按 DB 有效订单重算并覆盖 DB/Redis 库存，用于活动库存对账 */
    @Transactional
    public SeckillActivityDto reconcile(Long id) {
        SeckillActivity activity = activityRepository.findForUpdateById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND));
        long active = recordRepository.countByActivityIdAndStatusIn(id, List.of("CREATED", "PAID"));
        int expected = Math.max(0, activity.getTotalStock() - (int) active);
        activity.setAvailableStock(expected);
        activity = activityRepository.save(activity);
        stockService.warmUp(activity);
        Course course = courseRepository.findById(activity.getCourseId()).orElse(null);
        return toDto(activity, course, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public PageResult<SeckillRecordDto> records(Long id, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<SeckillRecord> result = recordRepository.findByActivityIdOrderByIdDesc(id, pageable);
        List<Long> userIds = result.getContent().stream().map(SeckillRecord::getUserId).distinct().toList();
        Map<Long, String> accounts = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getAccount));
        List<SeckillRecordDto> items = result.getContent().stream()
                .map(record -> new SeckillRecordDto(record.getId(), record.getUserId(),
                        accounts.get(record.getUserId()), record.getOrderNo(), record.getStatus(),
                        record.getReason(), record.getCreatedAt()))
                .toList();
        return PageResult.of(items, result.getTotalElements(), page, size);
    }

    private Course validateRequest(SeckillActivityRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECKILL_COURSE_INVALID));
        if (!"PUBLISHED".equals(course.getStatus())) {
            throw new BusinessException(ErrorCode.SECKILL_COURSE_INVALID);
        }
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (request.endAt().isBefore(now)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "结束时间不能早于当前时间");
        }
        if (request.seckillPrice().compareTo(course.getPrice()) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "秒杀价不能高于课程原价");
        }
        if (request.perUserLimit() == null || request.perUserLimit() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "当前版本每人限购固定为 1 份（seckill_record 唯一约束保证不重复下单）");
        }
        return course;
    }

    private Map<Long, Course> loadCourses(List<SeckillActivity> activities) {
        List<Long> courseIds = activities.stream().map(SeckillActivity::getCourseId).distinct().toList();
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Course> map = new HashMap<>();
        courseRepository.findAllById(courseIds).forEach(course -> map.put(course.getId(), course));
        return map;
    }

    private SeckillActivityDto toDto(SeckillActivity activity, Course course, LocalDateTime now) {
        Integer redisStock = stockService.remaining(activity.getId());
        long sold = Math.max(0, activity.getTotalStock() - activity.getAvailableStock());
        return new SeckillActivityDto(
                activity.getId(), activity.getCourseId(),
                course == null ? "课程已删除" : course.getTitle(),
                course == null ? null : course.getCoverUrl(),
                course == null ? BigDecimal.ZERO : course.getPrice(),
                activity.getSeckillPrice(),
                activity.getTotalStock(), activity.getAvailableStock(), redisStock, sold,
                activity.getPerUserLimit(),
                activity.getStartAt(), activity.getEndAt(), activity.getStatus(),
                SeckillPhase.of(activity, now), activity.getCreatedAt());
    }
}
