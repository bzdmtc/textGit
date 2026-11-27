package com.example.smart_learning.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record QuestionRequest(@NotBlank @Size(max = 32) String subject,
                              @NotBlank @Size(max = 100) String chapter,
                              @NotBlank String type,
                              @NotBlank @Size(max = 2000) String stem,
                              @Size(max = 4000) String analysis,
                              @Min(1) @Max(5) Integer difficulty,
                              @NotNull @Size(min = 2, max = 26) List<@Valid OptionRequest> options) {
}
