package com.example.smart_learning.coupon.service;

import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.course.entity.Course;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 优惠券纯计算规则，便于单元测试。
 */
public final class CouponCalculator {

    private CouponCalculator() {
    }

    public static boolean isClaimable(Coupon coupon, LocalDateTime now) {
        return "ON".equals(coupon.getStatus())
                && !now.isBefore(coupon.getStartAt())
                && !now.isAfter(coupon.getEndAt())
                && coupon.getRemainingQuantity() != null
                && coupon.getRemainingQuantity() > 0;
    }

    public static boolean isInValidPeriod(Coupon coupon, LocalDateTime now) {
        return !now.isBefore(coupon.getStartAt()) && !now.isAfter(coupon.getEndAt());
    }

    public static boolean matchesSubject(Coupon coupon, Collection<Course> courses) {
        String subject = coupon.getSubject();
        if (subject == null || subject.isBlank()) {
            return true;
        }
        return !courses.isEmpty() && courses.stream().allMatch(course -> subject.equals(course.getSubject()));
    }

    /**
     * FIXED：立减 value 元；PERCENT：value=90 表示支付原价的 90%（9 折）。
     * 优惠金额不会超过订单总额，最低应付 0 元。
     */
    public static BigDecimal discount(Coupon coupon, BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discount;
        if ("PERCENT".equals(coupon.getType())) {
            BigDecimal percent = coupon.getValueAmount();
            if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) >= 0) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            discount = totalAmount.multiply(BigDecimal.valueOf(100).subtract(percent))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getValueAmount() == null ? BigDecimal.ZERO : coupon.getValueAmount();
        }
        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(totalAmount) > 0) {
            discount = totalAmount;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }
}
