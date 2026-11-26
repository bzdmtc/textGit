package com.example.smart_learning.coupon.repository;

import com.example.smart_learning.coupon.entity.UserCoupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select uc from UserCoupon uc where uc.id = :id")
    Optional<UserCoupon> findForUpdateById(@Param("id") Long id);

    long countByUserIdAndCouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserIdOrderByIdDesc(Long userId);

    List<UserCoupon> findByUserIdAndStatusOrderByIdDesc(Long userId, String status);

    Optional<UserCoupon> findByOrderId(Long orderId);

    List<UserCoupon> findByOrderIdIn(List<Long> orderIds);

    @Query("select uc from UserCoupon uc, Coupon c " +
            "where c.id = uc.couponId and uc.status = 'UNUSED' and c.endAt < :now")
    List<UserCoupon> findExpiredUnused(@Param("now") LocalDateTime now);
}
