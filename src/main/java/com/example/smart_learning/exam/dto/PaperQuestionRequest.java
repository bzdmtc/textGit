package com.example.smart_learning.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaperQuestionRequest(@NotNull Long questionId,
                                   @NotNull @DecimalMin("0.5") @DecimalMax("100") BigDecimal score) {
}
