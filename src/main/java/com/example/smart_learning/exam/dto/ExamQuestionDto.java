package com.example.smart_learning.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamQuestionDto(Long questionId,
                              String type,
                              String stem,
                              List<ExamOptionDto> options,
                              BigDecimal score,
                              Integer sort) {
}
