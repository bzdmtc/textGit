package com.example.smart_learning.permission.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.permission.entity.SysAuditLog;
import com.example.smart_learning.permission.service.AuditService;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditAdminController {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");

    private final AuditService auditService;

    public AuditAdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @RequiresPriv(module = "audit", method = "browse")
    public ApiResponse<PageResult<SysAuditLog>> page(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SysAuditLog> result = auditService.page(targetType, action, userId,
                startOfDayUtc(from), startOfDayUtc(to == null ? null : to.plusDays(1)),
                PageRequest.of(Math.max(page, 1) - 1, size));
        return ApiResponse.success(PageResult.of(result.getContent(), result.getTotalElements(), page, size));
    }

    @GetMapping("/target-types")
    @RequiresPriv(module = "audit", method = "browse")
    public ApiResponse<List<String>> targetTypes() {
        return ApiResponse.success(auditService.targetTypes());
    }

    private LocalDateTime startOfDayUtc(LocalDate date) {
        return date == null ? null
                : date.atStartOfDay(BEIJING).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
