package com.example.smart_learning.coupon.dto;

import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserCouponDto(
        Long id,
        Long couponId,
        String name,
        String type,
        BigDecimal value,
        BigDecimal minAmount,
        String subject,
        String description,
        String status,
        LocalDateTime receivedAt,
        LocalDateTime usedAt,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long orderId
) {

    public static UserCouponDto of(UserCoupon userCoupon, Coupon coupon, LocalDateTime now) {
        String status = userCoupon.getStatus();
        if ("UNUSED".equals(status) && coupon.getEndAt().isBefore(now)) {
            status = "EXPIRED";
        }
        return new UserCouponDto(userCoupon.getId(), coupon.getId(), coupon.getName(), coupon.getType(),
                coupon.getValueAmount(), coupon.getMinAmount(), coupon.getSubject(), coupon.getDescription(),
                status, userCoupon.getReceivedAt(), userCoupon.getUsedAt(), coupon.getStartAt(),
                coupon.getEndAt(), userCoupon.getOrderId());
    }
}
