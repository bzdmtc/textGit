package com.example.smart_learning.exam.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.exam.dto.AnswerFeedbackDto;
import com.example.smart_learning.exam.dto.ExamRecordDto;
import com.example.smart_learning.exam.dto.ExamResultDto;
import com.example.smart_learning.exam.dto.ExamSessionDto;
import com.example.smart_learning.exam.dto.SaveAnswerRequest;
import com.example.smart_learning.exam.dto.StartExamRequest;
import com.example.smart_learning.exam.service.ExamService;
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
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    public ApiResponse<ExamSessionDto> start(@Valid @RequestBody StartExamRequest request) {
        return ApiResponse.success(examService.startExam(request));
    }

    @GetMapping("/records")
    public ApiResponse<PageResult<ExamRecordDto>> records(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(examService.records(page, size));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<ExamSessionDto> session(@PathVariable Long recordId) {
        return ApiResponse.success(examService.session(recordId));
    }

    @PutMapping("/{recordId}/answers")
    public ApiResponse<AnswerFeedbackDto> answer(@PathVariable Long recordId,
                                                 @Valid @RequestBody SaveAnswerRequest request) {
        return ApiResponse.success(examService.saveAnswer(recordId, request));
    }

    @PostMapping("/{recordId}/submit")
    public ApiResponse<ExamResultDto> submit(@PathVariable Long recordId) {
        return ApiResponse.success(examService.submit(recordId));
    }

    @GetMapping("/{recordId}/result")
    public ApiResponse<ExamResultDto> result(@PathVariable Long recordId) {
        return ApiResponse.success(examService.result(recordId));
    }
}
