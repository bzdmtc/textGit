package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamResultDto(ExamRecordDto record,
                            BigDecimal score,
                            BigDecimal totalScore,
                            int correctCount,
                            int wrongCount,
                            int unansweredCount,
                            Long durationSeconds,
                            List<ExamResultQuestionDto> questions) {
}
