package com.example.smart_learning.learn.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        Long courseId,
        String courseTitle,
        Long userId,
        String userAccount,
        String userNickname,
        Long orderId,
        Integer rating,
        String content,
        String status,
        String adminNote,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
}
