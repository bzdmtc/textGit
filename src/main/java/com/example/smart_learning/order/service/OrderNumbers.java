package com.example.smart_learning.order.service;

import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.order.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OrderNumbers {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;

    public OrderNumbers(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String next() {
        String base = "SL" + LocalDateTime.now(ZoneOffset.UTC).format(FORMAT);
        for (int i = 0; i < 5; i++) {
            String candidate = base + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (!orderRepository.existsByOrderNo(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }
}
