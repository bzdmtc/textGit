package com.example.smart_learning.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OptionRequest(@NotBlank @Size(max = 8) String label,
                            @NotBlank @Size(max = 1000) String content,
                            Boolean isCorrect,
                            Integer sort) {
}
