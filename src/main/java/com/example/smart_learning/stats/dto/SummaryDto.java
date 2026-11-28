package com.example.smart_learning.stats.dto;

import java.math.BigDecimal;

public record SummaryDto(
        BigDecimal todaySales,
        long todayOrders,
        long todayNewUsers,
        long todayEnrollments,
        BigDecimal totalSales,
        long totalOrders,
        long paidOrders,
        long totalUsers,
        long totalEnrollments,
        long pendingRefunds,
        BigDecimal avgOrderAmount
) {
}
