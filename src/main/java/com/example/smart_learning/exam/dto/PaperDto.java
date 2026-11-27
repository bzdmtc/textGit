package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaperDto(Long id,
                       String title,
                       String type,
                       String subject,
                       String chapter,
                       String description,
                       BigDecimal totalScore,
                       Integer durationMin,
                       String status,
                       Long ownerUserId,
                       Integer questionCount,
                       LocalDateTime createdAt) {
}
