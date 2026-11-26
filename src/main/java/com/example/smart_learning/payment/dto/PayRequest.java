package com.example.smart_learning.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PayRequest(
        @NotBlank(message = "请选择支付渠道")
        String channel
) {
}
