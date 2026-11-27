package com.example.smart_learning.learn.dto;

import java.math.BigDecimal;

public record ProgressDto(
        Long lessonId,
        Integer watchedSec,
        Boolean completed,
        BigDecimal courseProgressPercent
) {
}
