package com.example.smart_learning.coupon.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.coupon.dto.UserCouponDto;
import com.example.smart_learning.coupon.service.CouponUserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponUserController {

    private final CouponUserService couponUserService;

    public CouponUserController(CouponUserService couponUserService) {
        this.couponUserService = couponUserService;
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<UserCouponDto> claim(@PathVariable Long id) {
        return ApiResponse.success(couponUserService.claim(id));
    }
}
