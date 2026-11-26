package com.example.smart_learning.order.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.service.OrderAdminService;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "order", method = "browse")
    public ApiResponse<PageResult<OrderDto>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderAdminService.page(keyword, status, page, size));
    }

    @GetMapping("/{orderNo}")
    @RequiresPriv(module = "order", method = "browse")
    public ApiResponse<OrderDto> detail(@PathVariable String orderNo) {
        return ApiResponse.success(orderAdminService.detail(orderNo));
    }
}
