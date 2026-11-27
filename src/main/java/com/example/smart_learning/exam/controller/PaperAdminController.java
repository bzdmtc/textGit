package com.example.smart_learning.exam.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.exam.dto.PaperDetailDto;
import com.example.smart_learning.exam.dto.PaperDto;
import com.example.smart_learning.exam.dto.PaperRequest;
import com.example.smart_learning.exam.dto.PaperStatusRequest;
import com.example.smart_learning.exam.service.PaperAdminService;
import com.example.smart_learning.security.RequiresPriv;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/papers")
public class PaperAdminController {

    private final PaperAdminService paperAdminService;

    public PaperAdminController(PaperAdminService paperAdminService) {
        this.paperAdminService = paperAdminService;
    }

    @GetMapping
    @RequiresPriv(module = "paper", method = "browse")
    public ApiResponse<PageResult<PaperDto>> page(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String subject,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(paperAdminService.page(keyword, type, subject, status, page, size));
    }

    @GetMapping("/{id}")
    @RequiresPriv(module = "paper", method = "browse")
    public ApiResponse<PaperDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.success(paperAdminService.detail(id));
    }

    @PostMapping
    @RequiresPriv(module = "paper", method = "create")
    public ApiResponse<PaperDetailDto> create(@Valid @RequestBody PaperRequest request) {
        return ApiResponse.success(paperAdminService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPriv(module = "paper", method = "edit")
    public ApiResponse<PaperDetailDto> update(@PathVariable Long id,
                                              @Valid @RequestBody PaperRequest request) {
        return ApiResponse.success(paperAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @RequiresPriv(module = "paper", method = "publish")
    public ApiResponse<PaperDto> status(@PathVariable Long id,
                                        @Valid @RequestBody PaperStatusRequest request) {
        return ApiResponse.success(paperAdminService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @RequiresPriv(module = "paper", method = "delete")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        paperAdminService.disable(id);
        return ApiResponse.success();
    }
}
