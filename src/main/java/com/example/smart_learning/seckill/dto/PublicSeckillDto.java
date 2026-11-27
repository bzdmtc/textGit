package com.example.smart_learning.seckill.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 前台秒杀专区视图 */
public record PublicSeckillDto(
        Long id,
        Long courseId,
        String courseTitle,
        String courseSubtitle,
        String courseCover,
        String subject,
        BigDecimal coursePrice,
        BigDecimal seckillPrice,
        Integer totalStock,
        Integer remainingStock,
        Long soldCount,
        Integer perUserLimit,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String phase,
        LocalDateTime serverTime
) {
}
