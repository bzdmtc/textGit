package com.example.smart_learning.coupon.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.coupon.dto.CouponDto;
import com.example.smart_learning.coupon.dto.CouponRequest;
import com.example.smart_learning.coupon.dto.CouponStatusRequest;
import com.example.smart_learning.coupon.service.CouponAdminService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/coupons")
public class CouponAdminController {

    private final CouponAdminService couponAdminService;

    public CouponAdminController(CouponAdminService couponAdminService) {
        this.couponAdminService = couponAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "coupon", method = "browse")
    public ApiResponse<PageResult<CouponDto>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(couponAdminService.page(keyword, status, page, size));
    }

    @PostMapping
    @RequiresPriv(module = "coupon", method = "create")
    public ApiResponse<CouponDto> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.success(couponAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "coupon", method = "edit")
    public ApiResponse<CouponDto> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.success(couponAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "coupon", method = "delete")
    public ApiResponse<CouponDto> status(@PathVariable Long id,
                                         @Valid @RequestBody CouponStatusRequest request) {
        return ApiResponse.success(couponAdminService.changeStatus(id, request.status()));
    }
}
