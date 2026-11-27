package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;

public record PracticeStatsDto(int attemptCount,
                               BigDecimal averagePercent,
                               BigDecimal correctRate,
                               long wrongCount,
                               long masteredCount) {
}
