package com.example.smart_learning.seckill.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台活动视图 */
public record SeckillActivityDto(
        Long id,
        Long courseId,
        String courseTitle,
        String courseCover,
        BigDecimal coursePrice,
        BigDecimal seckillPrice,
        Integer totalStock,
        Integer availableStock,
        Integer redisStock,
        Long soldCount,
        Integer perUserLimit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String status,
        String phase,
        LocalDateTime createdAt
) {
}
