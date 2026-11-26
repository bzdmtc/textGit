package com.example.smart_learning.coupon.dto;

import com.example.smart_learning.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponDto(
        Long id,
        String name,
        String type,
        BigDecimal value,
        BigDecimal minAmount,
        Integer totalQuantity,
        Integer remainingQuantity,
        Integer perUserLimit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String subject,
        String description,
        String status
) {

    public static CouponDto of(Coupon coupon) {
        return new CouponDto(coupon.getId(), coupon.getName(), coupon.getType(), coupon.getValueAmount(),
                coupon.getMinAmount(), coupon.getTotalQuantity(), coupon.getRemainingQuantity(),
                coupon.getPerUserLimit(), coupon.getStartAt(), coupon.getEndAt(), coupon.getSubject(),
                coupon.getDescription(), coupon.getStatus());
    }
}
