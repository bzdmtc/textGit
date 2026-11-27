package com.example.smart_learning.seckill.dto;

import java.time.LocalDateTime;

public record SeckillClaimResult(
        Long activityId,
        String status,
        String message,
        LocalDateTime serverTime
) {
}
