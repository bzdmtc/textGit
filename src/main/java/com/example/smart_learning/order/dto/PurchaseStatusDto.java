package com.example.smart_learning.order.dto;

public record PurchaseStatusDto(
        Long courseId,
        boolean enrolled,
        String pendingOrderNo,
        String paidOrderNo
) {
}
