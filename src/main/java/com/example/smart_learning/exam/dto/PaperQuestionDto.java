package com.example.smart_learning.exam.dto;

import com.example.smart_learning.question.dto.QuestionOptionDto;

import java.math.BigDecimal;
import java.util.List;

public record PaperQuestionDto(Long questionId,
                               String subject,
                               String chapter,
                               String type,
                               String stem,
                               String analysis,
                               String correctAnswer,
                               BigDecimal score,
                               Integer sort,
                               List<QuestionOptionDto> options) {
}
