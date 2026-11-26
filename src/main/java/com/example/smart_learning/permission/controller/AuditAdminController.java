package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.permission.entity.SysAuditLog;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditAdminController {

    private final AuditService auditService;

    public AuditAdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @RequiresPriv(module = "audit", method = "browse")
    public ApiResponse<PageResult<SysAuditLog>> page(@RequestParam(required = false) String targetType,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Page<SysAuditLog> result = auditService.page(targetType,
                PageRequest.of(Math.max(page, 1) - 1, size));
        return ApiResponse.success(PageResult.of(result.getContent(), result.getTotalElements(), page, size));
    }
}
