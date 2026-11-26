package com.example.smart_learning.order.dto;

import com.example.smart_learning.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long courseId,
        String courseTitle,
        String coverUrl,
        String subject,
        BigDecimal price
) {

    public static OrderItemDto of(OrderItem item) {
        return new OrderItemDto(item.getId(), item.getCourseId(), item.getCourseTitleSnapshot(),
                item.getCoverSnapshot(), item.getSubjectSnapshot(), item.getPriceSnapshot());
    }
}
