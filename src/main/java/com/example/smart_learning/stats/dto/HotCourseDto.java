package com.example.smart_learning.stats.dto;

import java.math.BigDecimal;

public record HotCourseDto(
        Long courseId,
        String title,
        String subject,
        long salesCount,
        BigDecimal salesAmount
) {
}
