package com.example.smart_learning.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SeckillStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ON|OFF", message = "状态只能为 ON 或 OFF")
        String status
) {
}
