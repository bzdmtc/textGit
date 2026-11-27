package com.example.smart_learning.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "APPROVED|REJECTED", message = "状态只能为 APPROVED 或 REJECTED")
        String status,
        @Size(max = 500, message = "审核备注过长")
        String adminNote
) {
}
