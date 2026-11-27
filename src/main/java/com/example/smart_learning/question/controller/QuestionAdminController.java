package com.example.smart_learning.question.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.question.dto.QuestionDto;
import com.example.smart_learning.question.dto.QuestionRequest;
import com.example.smart_learning.question.dto.QuestionStatusRequest;
import com.example.smart_learning.question.service.QuestionAdminService;
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
@RequestMapping("/api/v1/admin/questions")
public class QuestionAdminController {

    private final QuestionAdminService questionAdminService;

    public QuestionAdminController(QuestionAdminService questionAdminService) {
        this.questionAdminService = questionAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "question", method = "browse")
    public ApiResponse<PageResult<QuestionDto>> page(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String subject,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(questionAdminService.page(keyword, subject, type, status, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPriv(module = "question", method = "browse")
    public ApiResponse<QuestionDto> detail(@PathVariable Long id) {
        return ApiResponse.success(questionAdminService.detail(id));
    }

    @PostMapping
    @RequiresPriv(module = "question", method = "create")
    public ApiResponse<QuestionDto> create(@Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success(questionAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "question", method = "edit")
    public ApiResponse<QuestionDto> update(@PathVariable Long id,
                                           @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success(questionAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "question", method = "delete")
    public ApiResponse<QuestionDto> status(@PathVariable Long id,
                                           @Valid @RequestBody QuestionStatusRequest request) {
        return ApiResponse.success(questionAdminService.changeStatus(id, request.status()));
    }
}
