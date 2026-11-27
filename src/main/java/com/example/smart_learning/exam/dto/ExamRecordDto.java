package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamRecordDto(Long id,
                            Long paperId,
                            String title,
                            String mode,
                            String source,
                            String subject,
                            String chapter,
                            String status,
                            BigDecimal score,
                            BigDecimal totalScore,
                            Integer correctCount,
                            Integer wrongCount,
                            Integer questionCount,
                            LocalDateTime startedAt,
                            LocalDateTime submittedAt,
                            Long durationSeconds) {
}
