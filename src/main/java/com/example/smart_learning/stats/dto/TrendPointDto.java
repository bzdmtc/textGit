package com.example.smart_learning.stats.dto;

import java.math.BigDecimal;

public record TrendPointDto(
        String date,
        BigDecimal sales,
        long orders,
        long newUsers,
        long enrollments
) {
}
