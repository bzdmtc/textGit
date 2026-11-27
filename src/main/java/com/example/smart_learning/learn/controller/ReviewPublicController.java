package com.example.smart_learning.learn.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.learn.dto.ReviewSummaryDto;
import com.example.smart_learning.learn.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/courses")
public class ReviewPublicController {

    private final ReviewService reviewService;

    public ReviewPublicController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{courseId}/reviews")
    public ApiResponse<ReviewSummaryDto> reviews(@PathVariable Long courseId) {
        return ApiResponse.success(reviewService.publicSummary(courseId));
    }
}
