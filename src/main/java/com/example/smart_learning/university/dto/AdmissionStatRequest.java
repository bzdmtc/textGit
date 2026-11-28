package com.example.smart_learning.university.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdmissionStatRequest(
        @NotNull Long majorId,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @Min(0) Integer applicants,
        @NotNull @Min(0) Integer admitted,
        @Min(0) Integer minScore
) {
}
