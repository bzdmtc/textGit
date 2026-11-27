package com.example.smart_learning.exam.dto;

import com.example.smart_learning.question.dto.QuestionOptionDto;

import java.time.LocalDateTime;
import java.util.List;

public record WrongQuestionDto(Long id,
                               Long questionId,
                               String subject,
                               String chapter,
                               String type,
                               String stem,
                               String analysis,
                               String correctAnswer,
                               Integer wrongCount,
                               LocalDateTime lastWrongAt,
                               Boolean mastered,
                               List<QuestionOptionDto> options) {
}
