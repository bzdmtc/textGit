package com.example.smart_learning.exam.dto;

import jakarta.validation.constraints.NotBlank;

public record PaperStatusRequest(@NotBlank String status) {
}
