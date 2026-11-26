package com.example.smart_learning.payment.gateway;

import java.time.LocalDateTime;

public record PaymentResult(
        boolean success,
        String transactionNo,
        LocalDateTime paidAt,
        String message
) {
}
