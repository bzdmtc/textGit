package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysGroupRepository extends JpaRepository<SysGroup, Long> {

    Optional<SysGroup> findByCode(String code);

    boolean existsByCode(String code);

    List<SysGroup> findAllByOrderBySortAscIdAsc();

    List<SysGroup> findByStatusOrderBySortAscIdAsc(String status);
}
