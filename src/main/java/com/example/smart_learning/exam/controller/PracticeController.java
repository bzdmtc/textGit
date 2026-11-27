package com.example.smart_learning.exam.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.exam.dto.ExamSessionDto;
import com.example.smart_learning.exam.dto.PracticeOverviewDto;
import com.example.smart_learning.exam.dto.StartPracticeRequest;
import com.example.smart_learning.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/practice")
public class PracticeController {

    private final ExamService examService;

    public PracticeController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping("/overview")
    public ApiResponse<PracticeOverviewDto> overview() {
        return ApiResponse.success(examService.overview());
    }

    @PostMapping("/sessions")
    public ApiResponse<ExamSessionDto> start(@Valid @RequestBody StartPracticeRequest request) {
        return ApiResponse.success(examService.startPractice(request));
    }
}
