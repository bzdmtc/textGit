package com.example.smart_learning.seckill.repository;

import com.example.smart_learning.seckill.entity.SeckillActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeckillActivityRepository extends JpaRepository<SeckillActivity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SeckillActivity a where a.id = :id")
    Optional<SeckillActivity> findForUpdateById(@Param("id") Long id);

    @Query("select a from SeckillActivity a where a.status = 'ON' and a.endAt > :now order by a.startAt asc, a.id asc")
    List<SeckillActivity> findVisible(@Param("now") LocalDateTime now);

    @Query("select a from SeckillActivity a where a.status = 'ON' and a.endAt > :now")
    List<SeckillActivity> findOnAndNotEnded(@Param("now") LocalDateTime now);

    @Query("select a from SeckillActivity a " +
            "where (:status is null or :status = '' or a.status = :status) " +
            "order by a.id desc")
    Page<SeckillActivity> searchAdmin(@Param("status") String status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeckillActivity a set a.availableStock = a.availableStock - 1 " +
            "where a.id = :id and a.availableStock > 0")
    int decreaseStock(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SeckillActivity a set a.availableStock = case when a.availableStock < a.totalStock " +
            "then a.availableStock + 1 else a.availableStock end where a.id = :id")
    int increaseStock(@Param("id") Long id);

    boolean existsByCourseIdAndStatusAndEndAtAfter(Long courseId, String status, LocalDateTime now);
}
