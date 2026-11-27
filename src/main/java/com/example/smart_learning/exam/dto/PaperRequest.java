package com.example.smart_learning.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PaperRequest(@NotBlank @Size(max = 200) String title,
                           @NotBlank String type,
                           @NotBlank @Size(max = 32) String subject,
                           @Size(max = 100) String chapter,
                           @Size(max = 1000) String description,
                           @NotNull @Min(1) @Max(600) Integer durationMin,
                           @NotNull @Size(min = 1, max = 200) List<@Valid PaperQuestionRequest> questions) {
}
