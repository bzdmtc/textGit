package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;

public record ExamAnswerStateDto(Long questionId,
                                 String answer,
                                 Boolean isCorrect,
                                 BigDecimal earnedScore,
                                 String correctAnswer,
                                 String analysis) {
}
