package com.example.smart_learning.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundApplyRequest(
        @NotBlank(message = "请填写退款原因")
        @Size(max = 500, message = "退款原因过长")
        String reason
) {
}
