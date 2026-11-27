package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;

public record AnswerFeedbackDto(Long questionId,
                                String answer,
                                Boolean isCorrect,
                                BigDecimal earnedScore,
                                String correctAnswer,
                                String analysis) {
}
