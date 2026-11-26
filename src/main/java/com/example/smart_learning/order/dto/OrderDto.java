package com.example.smart_learning.order.dto;

import com.example.smart_learning.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        String orderNo,
        Long userId,
        String userAccount,
        String nickname,
        String status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        Long userCouponId,
        String couponName,
        String orderSource,
        String payMethod,
        LocalDateTime paidAt,
        LocalDateTime expireAt,
        LocalDateTime canceledAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<OrderItemDto> items,
        String refundStatus,
        String refundReason
) {

    public static OrderDto of(Order order, List<OrderItemDto> items, String couponName,
                               String refundStatus, String refundReason) {
        return new OrderDto(order.getId(), order.getOrderNo(), order.getUserId(), null, null,
                order.getStatus(), order.getTotalAmount(), order.getDiscountAmount(), order.getPayAmount(),
                order.getUserCouponId(), couponName, order.getOrderSource(), order.getPayMethod(),
                order.getPaidAt(), order.getExpireAt(), order.getCanceledAt(), order.getCompletedAt(),
                order.getCreatedAt(), items, refundStatus, refundReason);
    }

    public OrderDto withUser(String account, String nickname) {
        return new OrderDto(id, orderNo, userId, account, nickname, status, totalAmount, discountAmount,
                payAmount, userCouponId, couponName, orderSource, payMethod, paidAt, expireAt,
                canceledAt, completedAt, createdAt, items, refundStatus, refundReason);
    }
}
