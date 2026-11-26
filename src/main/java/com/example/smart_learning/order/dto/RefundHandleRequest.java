package com.example.smart_learning.order.dto;

import jakarta.validation.constraints.Size;

public record RefundHandleRequest(
        @Size(max = 500, message = "备注过长")
        String adminNote
) {
}
