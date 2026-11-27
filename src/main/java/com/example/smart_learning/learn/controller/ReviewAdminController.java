package com.example.smart_learning.learn.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.learn.dto.ReviewDto;
import com.example.smart_learning.learn.dto.ReviewStatusRequest;
import com.example.smart_learning.learn.service.ReviewService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class ReviewAdminController {

    private final ReviewService reviewService;

    public ReviewAdminController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @RequiresPriv(module = "review", method = "browse")
    public ApiResponse<PageResult<ReviewDto>> page(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(reviewService.page(keyword, status, page, size));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "review", method = "approve")
    public ApiResponse<ReviewDto> moderate(@PathVariable Long id,
                                           @Valid @RequestBody ReviewStatusRequest request) {
        return ApiResponse.success(reviewService.moderate(id, request));
    }
}
