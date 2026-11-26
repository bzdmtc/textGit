package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysAuditLogRepository extends JpaRepository<SysAuditLog, Long> {

    Page<SysAuditLog> findByTargetTypeOrderByIdDesc(String targetType, Pageable pageable);

    Page<SysAuditLog> findAllByOrderByIdDesc(Pageable pageable);
}
