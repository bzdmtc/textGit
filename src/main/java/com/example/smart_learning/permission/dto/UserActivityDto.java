package com.example.smart_learning.permission.dto;

import java.math.BigDecimal;

public record UserActivityDto(
        Long userId,
        long orderCount,
        long paidOrderCount,
        BigDecimal paidAmount,
        long courseCount,
        long practiceCount,
        long wrongCount
) {
}
