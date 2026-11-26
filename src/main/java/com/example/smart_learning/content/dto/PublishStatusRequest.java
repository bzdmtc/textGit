package com.example.smart_learning.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PublishStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "PUBLISHED|DRAFT", message = "状态只能是 PUBLISHED/DRAFT")
        String status
) {
}
