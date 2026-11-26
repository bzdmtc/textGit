package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysUserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SysUserGroupRepository extends JpaRepository<SysUserGroup, Long> {

    List<SysUserGroup> findByUserId(Long userId);

    List<SysUserGroup> findByGroupId(Long groupId);

    long countByGroupId(Long groupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    @Query("select g.id from SysGroup g join SysUserGroup ug on ug.groupId = g.id " +
            "where ug.userId = :userId and g.status = 'ACTIVE' order by g.sort asc, g.id asc")
    List<Long> findActiveGroupIdsByUserId(@Param("userId") Long userId);
}
