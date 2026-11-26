package com.example.smart_learning.coupon.repository;

import com.example.smart_learning.coupon.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findForUpdateById(@Param("id") Long id);

    @Query("select c from Coupon c where (:keyword is null or :keyword = '' " +
            "or lower(c.name) like lower(concat('%', :keyword, '%'))) " +
            "and (:status is null or :status = '' or c.status = :status) " +
            "order by c.id desc")
    Page<Coupon> search(@Param("keyword") String keyword,
                        @Param("status") String status,
                        Pageable pageable);

    @Query("select c from Coupon c where c.status = 'ON' " +
            "and c.startAt <= :now and c.endAt >= :now and c.remainingQuantity > 0 " +
            "and (:subject is null or :subject = '' or c.subject is null or c.subject = '' or c.subject = :subject) " +
            "order by c.id desc")
    List<Coupon> findAvailable(@Param("now") LocalDateTime now, @Param("subject") String subject);

    List<Coupon> findByIdIn(List<Long> ids);
}
