package com.example.smart_learning.order.dto;

import com.example.smart_learning.order.entity.RefundRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundDto(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String userAccount,
        String nickname,
        String reason,
        BigDecimal amount,
        String status,
        String adminNote,
        Long handledBy,
        LocalDateTime handledAt,
        LocalDateTime createdAt,
        String orderStatus
) {

    public static RefundDto of(RefundRequest refund, String account, String nickname, String orderStatus) {
        return new RefundDto(refund.getId(), refund.getOrderId(), refund.getOrderNo(), refund.getUserId(),
                account, nickname, refund.getReason(), refund.getAmount(), refund.getStatus(),
                refund.getAdminNote(), refund.getHandledBy(), refund.getHandledAt(),
                refund.getCreatedAt(), orderStatus);
    }
}
