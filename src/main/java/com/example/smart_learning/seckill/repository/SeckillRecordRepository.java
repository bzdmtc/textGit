package com.example.smart_learning.seckill.repository;

import com.example.smart_learning.seckill.entity.SeckillRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeckillRecordRepository extends JpaRepository<SeckillRecord, Long> {

    Optional<SeckillRecord> findByUserIdAndActivityId(Long userId, Long activityId);

    boolean existsByUserIdAndActivityId(Long userId, Long activityId);

    Optional<SeckillRecord> findByOrderNo(String orderNo);

    long countByActivityIdAndStatusIn(Long activityId, List<String> statuses);

    long countByActivityIdAndStatus(Long activityId, String status);

    Page<SeckillRecord> findByActivityIdOrderByIdDesc(Long activityId, Pageable pageable);

    @Query("select r.status, count(r) from SeckillRecord r where r.activityId = :activityId group by r.status")
    List<Object[]> countGroupByStatus(@Param("activityId") Long activityId);
}
