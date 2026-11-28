package com.example.smart_learning.university.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScoreLineRequest(
        @NotNull Long majorId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @Min(0) Integer political,
        @Min(0) Integer english,
        @Min(0) Integer math,
        @Min(0) Integer majorScore,
        @NotNull @Min(0) Integer total
) {
}
