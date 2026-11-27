package com.example.smart_learning.question.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionStatusRequest(@NotBlank String status) {
}
