package com.example.smart_learning.question.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionDto(Long id,
                          String subject,
                          String chapter,
                          String type,
                          String stem,
                          String analysis,
                          Integer difficulty,
                          String status,
                          Long ownerUserId,
                          String correctAnswer,
                          List<QuestionOptionDto> options,
                          LocalDateTime createdAt) {
}
