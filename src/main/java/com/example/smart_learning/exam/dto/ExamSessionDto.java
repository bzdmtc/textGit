package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExamSessionDto(Long recordId,
                             String title,
                             String mode,
                             String source,
                             String status,
                             BigDecimal totalScore,
                             LocalDateTime startedAt,
                             LocalDateTime deadlineAt,
                             Long remainingSeconds,
                             List<ExamQuestionDto> questions,
                             List<ExamAnswerStateDto> answers) {
}
