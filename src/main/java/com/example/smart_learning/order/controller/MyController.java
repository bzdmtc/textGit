package com.example.smart_learning.order.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.coupon.dto.UserCouponDto;
import com.example.smart_learning.coupon.service.CouponUserService;
import com.example.smart_learning.enrollment.dto.MyCourseDto;
import com.example.smart_learning.enrollment.service.EnrollmentService;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.dto.PurchaseStatusDto;
import com.example.smart_learning.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/my")
public class MyController {

    private final OrderService orderService;
    private final CouponUserService couponUserService;
    private final EnrollmentService enrollmentService;

    public MyController(OrderService orderService,
                        CouponUserService couponUserService,
                        EnrollmentService enrollmentService) {
        this.orderService = orderService;
        this.couponUserService = couponUserService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/orders")
    public ApiResponse<PageResult<OrderDto>> orders(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.myOrders(status, page, size));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<OrderDto> order(@PathVariable String orderNo) {
        return ApiResponse.success(orderService.myOrder(orderNo));
    }

    @GetMapping("/purchase-status")
    public ApiResponse<PurchaseStatusDto> purchaseStatus(@RequestParam Long courseId) {
        return ApiResponse.success(orderService.purchaseStatus(courseId));
    }

    @GetMapping("/coupons")
    public ApiResponse<List<UserCouponDto>> coupons(@RequestParam(required = false) String status) {
        return ApiResponse.success(couponUserService.myCoupons(status));
    }

    @GetMapping("/courses")
    public ApiResponse<List<MyCourseDto>> courses() {
        return ApiResponse.success(enrollmentService.myCourses());
    }
}
