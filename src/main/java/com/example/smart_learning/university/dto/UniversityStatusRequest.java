package com.example.smart_learning.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UniversityStatusRequest(
        @NotBlank @Pattern(regexp = "ACTIVE|DISABLED", message = "状态只能是 ACTIVE/DISABLED") String status
) {
}
