package com.example.smart_learning.order.repository;

import com.example.smart_learning.order.entity.Order;
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

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByOrderNoAndUserId(String orderNo, Long userId);

    boolean existsByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderNo = :orderNo")
    Optional<Order> findForUpdateByOrderNo(@Param("orderNo") String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findForUpdateById(@Param("id") Long id);

    @Query("select o from Order o where o.userId = :userId " +
            "and (:status is null or :status = '' or o.status = :status) " +
            "order by o.id desc")
    Page<Order> searchByUser(@Param("userId") Long userId,
                             @Param("status") String status,
                             Pageable pageable);

    @Query(value = "select o from Order o, SysUser u where u.id = o.userId " +
            "and (:status is null or :status = '' or o.status = :status) " +
            "and (:keyword is null or :keyword = '' " +
            "     or lower(o.orderNo) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
            "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
            "     or lower(coalesce(u.realname, '')) like lower(concat('%', :keyword, '%'))) " +
            "order by o.id desc",
            countQuery = "select count(o) from Order o, SysUser u where u.id = o.userId " +
                    "and (:status is null or :status = '' or o.status = :status) " +
                    "and (:keyword is null or :keyword = '' " +
                    "     or lower(o.orderNo) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(coalesce(u.realname, '')) like lower(concat('%', :keyword, '%')))")
    Page<Order> searchAdmin(@Param("keyword") String keyword,
                            @Param("status") String status,
                            Pageable pageable);

    @Query("select distinct o.orderNo from Order o, OrderItem i " +
            "where i.orderId = o.id and o.userId = :userId and i.courseId = :courseId " +
            "and o.status = 'PENDING_PAYMENT'")
    List<String> findPendingOrderNos(@Param("userId") Long userId, @Param("courseId") Long courseId);

    List<Order> findByStatusAndExpireAtBefore(String status, LocalDateTime time);

    List<Order> findByStatusAndPaidAtBefore(String status, LocalDateTime time);
}
