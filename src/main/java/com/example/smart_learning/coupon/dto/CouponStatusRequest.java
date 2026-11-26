package com.example.smart_learning.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponStatusRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
