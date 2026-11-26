package com.example.smart_learning.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "请选择要购买的课程")
        @Size(max = 10, message = "单笔订单最多购买 10 门课程")
        List<Long> courseIds,
        Long userCouponId
) {
}
