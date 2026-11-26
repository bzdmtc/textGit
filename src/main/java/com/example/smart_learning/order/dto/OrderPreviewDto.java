package com.example.smart_learning.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderPreviewDto(
        List<OrderItemDto> items,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal payAmount,
        Long userCouponId,
        String couponName,
        String couponDescription
) {
}
