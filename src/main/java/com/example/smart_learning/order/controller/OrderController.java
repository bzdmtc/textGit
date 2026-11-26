package com.example.smart_learning.order.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.order.dto.CreateOrderRequest;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.dto.OrderPreviewDto;
import com.example.smart_learning.order.dto.RefundApplyRequest;
import com.example.smart_learning.order.dto.RefundDto;
import com.example.smart_learning.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewDto> preview(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.preview(request));
    }

    @PostMapping
    public ApiResponse<OrderDto> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.create(request));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderDto> cancel(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.cancel(orderNo));
    }

    @PostMapping("/{orderNo}/refund")
    public ApiResponse<RefundDto> refund(@PathVariable String orderNo,
                                         @Valid @RequestBody RefundApplyRequest request) {
        return ApiResponse.success(orderService.applyRefund(orderNo, request));
    }
}
