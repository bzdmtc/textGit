package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.NotBlank;

public record PositionStatusRequest(@NotBlank String status) {
}
