package com.example.smart_learning.coupon.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.datasource.ReadOnly;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.coupon.dto.CouponDto;
import com.example.smart_learning.coupon.dto.UserCouponDto;
import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.entity.UserCoupon;
import com.example.smart_learning.coupon.repository.CouponRepository;
import com.example.smart_learning.coupon.repository.UserCouponRepository;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CouponUserService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final CourseRepository courseRepository;

    public CouponUserService(CouponRepository couponRepository,
                             UserCouponRepository userCouponRepository,
                             CourseRepository courseRepository) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.courseRepository = courseRepository;
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<CouponDto> availableForSubject(String subject) {
        return couponRepository.findAvailable(now(), subject).stream().map(CouponDto::of).toList();
    }

    @ReadOnly
    @Transactional(readOnly = true)
    public List<CouponDto> availableForCourse(Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || !"PUBLISHED".equals(course.getStatus())) {
            return List.of();
        }
        return availableForSubject(course.getSubject());
    }

    @Master
    @Transactional
    public UserCouponDto claim(Long couponId) {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Coupon coupon = couponRepository.findForUpdateById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        LocalDateTime now = now();
        if (!CouponCalculator.isClaimable(coupon, now)) {
            if (coupon.getRemainingQuantity() != null && coupon.getRemainingQuantity() <= 0) {
                throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
            }
            throw new BusinessException(ErrorCode.COUPON_NOT_CLAIMABLE);
        }
        long claimed = userCouponRepository.countByUserIdAndCouponId(userId, couponId);
        int limit = coupon.getPerUserLimit() == null ? 1 : coupon.getPerUserLimit();
        if (claimed >= limit) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_CLAIMED,
                    "每人最多领取 " + limit + " 张");
        }
        coupon.setRemainingQuantity(coupon.getRemainingQuantity() - 1);
        couponRepository.save(coupon);

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus("UNUSED");
        userCoupon.setReceivedAt(now);
        userCoupon = userCouponRepository.save(userCoupon);
        return UserCouponDto.of(userCoupon, coupon, now);
    }

    @Master
    @Transactional(readOnly = true)
    public List<UserCouponDto> myCoupons(String status) {
        Long userId = SecurityUtils.currentUserId();
        List<UserCoupon> coupons = status == null || status.isBlank()
                ? userCouponRepository.findByUserIdOrderByIdDesc(userId)
                : userCouponRepository.findByUserIdAndStatusOrderByIdDesc(userId, status);
        if (coupons.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = coupons.stream().map(UserCoupon::getCouponId).distinct().toList();
        Map<Long, Coupon> couponMap = couponRepository.findByIdIn(couponIds).stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        LocalDateTime now = now();
        return coupons.stream()
                .map(userCoupon -> {
                    Coupon coupon = couponMap.get(userCoupon.getCouponId());
                    return coupon == null ? null : UserCouponDto.of(userCoupon, coupon, now);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Master
    public void releaseForOrder(Order order) {
        if (order.getUserCouponId() == null) {
            return;
        }
        userCouponRepository.findByOrderId(order.getId()).ifPresent(userCoupon -> {
            if ("LOCKED".equals(userCoupon.getStatus())) {
                userCoupon.setStatus("UNUSED");
                userCoupon.setOrderId(null);
                userCouponRepository.save(userCoupon);
            }
        });
    }

    @Master
    public void markUsed(Order order) {
        if (order.getUserCouponId() == null) {
            return;
        }
        userCouponRepository.findByOrderId(order.getId()).ifPresent(userCoupon -> {
            userCoupon.setStatus("USED");
            userCoupon.setUsedAt(now());
            userCouponRepository.save(userCoupon);
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
