package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SysAuditLogRepository extends JpaRepository<SysAuditLog, Long> {

    @Query(value = """
            select a from SysAuditLog a
            where (:targetType is null or a.targetType = :targetType)
              and (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (:userId is null or a.userId = :userId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            order by a.id desc
            """,
            countQuery = """
            select count(a) from SysAuditLog a
            where (:targetType is null or a.targetType = :targetType)
              and (:action is null or lower(a.action) like lower(concat('%', :action, '%')))
              and (:userId is null or a.userId = :userId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
            """)
    Page<SysAuditLog> search(@Param("targetType") String targetType,
                             @Param("action") String action,
                             @Param("userId") Long userId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);

    @Query("select distinct a.targetType from SysAuditLog a where a.targetType is not null order by a.targetType")
    List<String> distinctTargetTypes();
}
