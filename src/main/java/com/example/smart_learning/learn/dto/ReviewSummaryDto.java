package com.example.smart_learning.learn.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReviewSummaryDto(
        Long courseId,
        BigDecimal averageRating,
        Long reviewCount,
        List<ReviewDto> items
) {
}
