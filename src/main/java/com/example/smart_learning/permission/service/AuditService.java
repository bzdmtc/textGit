package com.example.smart_learning.permission.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.permission.entity.SysAuditLog;
import com.example.smart_learning.permission.repository.SysAuditLogRepository;
import com.example.smart_learning.security.SecurityUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Master
public class AuditService {

    private final SysAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(SysAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String action, String targetType, Object targetId, Object before, Object after) {
        SysAuditLog log = new SysAuditLog();
        log.setUserId(SecurityUtils.currentUserId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));
        log.setBeforeJson(toJson(before));
        log.setAfterJson(toJson(after));
        log.setIp(currentIp());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<SysAuditLog> page(String targetType, Pageable pageable) {
        if (targetType == null || targetType.isBlank()) {
            return auditLogRepository.findAllByOrderByIdDesc(pageable);
        }
        return auditLogRepository.findByTargetTypeOrderByIdDesc(targetType, pageable);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException ex) {
            return String.valueOf(value);
        }
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
