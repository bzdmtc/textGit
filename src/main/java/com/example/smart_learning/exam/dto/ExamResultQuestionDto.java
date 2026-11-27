package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamResultQuestionDto(Long questionId,
                                    String subject,
                                    String chapter,
                                    String type,
                                    String stem,
                                    List<ExamOptionDto> options,
                                    String correctAnswer,
                                    String myAnswer,
                                    Boolean isCorrect,
                                    BigDecimal score,
                                    BigDecimal earnedScore,
                                    String analysis) {
}
