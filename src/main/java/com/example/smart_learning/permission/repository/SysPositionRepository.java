package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.entity.SysPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysPositionRepository extends JpaRepository<SysPosition, Long> {

    List<SysPosition> findAllByOrderBySortAscIdAsc();

    Optional<SysPosition> findByName(String name);

    Optional<SysPosition> findByNameAndStatus(String name, String status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
