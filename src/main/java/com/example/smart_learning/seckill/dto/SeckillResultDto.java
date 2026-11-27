package com.example.smart_learning.seckill.dto;

import java.time.LocalDateTime;

/** NOT_JOINED / QUEUED / CREATED / PAID / CANCELED / FAILED */
public record SeckillResultDto(
        Long activityId,
        String status,
        String orderNo,
        String reason,
        String phase,
        LocalDateTime serverTime
) {
}
