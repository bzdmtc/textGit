package com.example.smart_learning.learn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteDto(
        Long id,
        Long courseId,
        String courseTitle,
        String coverUrl,
        String subject,
        String stage,
        BigDecimal price,
        BigDecimal rating,
        Integer studentCount,
        Boolean enrolled,
        BigDecimal progressPercent,
        LocalDateTime favoritedAt
) {
}
