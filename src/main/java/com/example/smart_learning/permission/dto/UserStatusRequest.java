package com.example.smart_learning.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ACTIVE|DISABLED", message = "状态只能是 ACTIVE/DISABLED")
        String status
) {
}
