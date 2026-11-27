package com.example.smart_learning.seckill.dto;

import java.time.LocalDateTime;

public record SeckillRecordDto(
        Long id,
        Long userId,
        String userAccount,
        String orderNo,
        String status,
        String reason,
        LocalDateTime createdAt
) {
}
