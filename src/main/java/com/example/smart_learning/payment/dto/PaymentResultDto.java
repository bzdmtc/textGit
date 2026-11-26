package com.example.smart_learning.payment.dto;

import com.example.smart_learning.order.dto.OrderDto;

import java.time.LocalDateTime;

public record PaymentResultDto(
        OrderDto order,
        String channel,
        String transactionNo,
        LocalDateTime paidAt,
        String message
) {
}
