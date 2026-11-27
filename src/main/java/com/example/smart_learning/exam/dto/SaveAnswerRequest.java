package com.example.smart_learning.exam.dto;

import jakarta.validation.constraints.NotNull;

public record SaveAnswerRequest(@NotNull Long questionId, String answer) {
}
