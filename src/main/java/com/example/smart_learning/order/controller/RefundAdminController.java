package com.example.smart_learning.order.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.order.dto.RefundDto;
import com.example.smart_learning.order.dto.RefundHandleRequest;
import com.example.smart_learning.order.service.OrderAdminService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/refunds")
public class RefundAdminController {

    private final OrderAdminService orderAdminService;

    public RefundAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "refund", method = "browse")
    public ApiResponse<PageResult<RefundDto>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderAdminService.refundPage(keyword, status, page, size));
    }

    @PostMapping("/{id}/approve")
    @RequiresPriv(module = "refund", method = "approve")
    public ApiResponse<RefundDto> approve(@PathVariable Long id,
                                          @Valid @RequestBody(required = false) RefundHandleRequest request) {
        return ApiResponse.success(orderAdminService.approve(id, request));
    }

    @PostMapping("/{id}/reject")
    @RequiresPriv(module = "refund", method = "approve")
    public ApiResponse<RefundDto> reject(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) RefundHandleRequest request) {
        return ApiResponse.success(orderAdminService.reject(id, request));
    }
}
