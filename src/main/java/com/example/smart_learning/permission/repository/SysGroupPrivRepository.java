package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysGroupPriv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SysGroupPrivRepository extends JpaRepository<SysGroupPriv, Long> {

    List<SysGroupPriv> findByGroupId(Long groupId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteByGroupId(Long groupId);

    @Query("select gp.privId from SysGroupPriv gp where gp.groupId = :groupId order by gp.privId asc")
    List<Long> findPrivIdsByGroupId(@Param("groupId") Long groupId);

    @Query("select gp.privId from SysGroupPriv gp where gp.groupId in :groupIds")
    List<Long> findPrivIdsByGroupIds(@Param("groupIds") List<Long> groupIds);
}
