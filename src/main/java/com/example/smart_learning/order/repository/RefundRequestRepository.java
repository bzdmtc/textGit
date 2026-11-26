package com.example.smart_learning.order.repository;

import com.example.smart_learning.order.entity.RefundRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    boolean existsByOrderIdAndStatus(Long orderId, String status);

    List<RefundRequest> findByOrderIdInOrderByIdDesc(Collection<Long> orderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundRequest r where r.id = :id")
    Optional<RefundRequest> findForUpdateById(@Param("id") Long id);

    @Query(value = "select r from RefundRequest r, Order o, SysUser u " +
            "where o.id = r.orderId and u.id = r.userId " +
            "and (:status is null or :status = '' or r.status = :status) " +
            "and (:keyword is null or :keyword = '' " +
            "     or lower(r.orderNo) like lower(concat('%', :keyword, '%')) " +
            "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
            "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
            "     or lower(coalesce(u.realname, '')) like lower(concat('%', :keyword, '%'))) " +
            "order by r.id desc",
            countQuery = "select count(r) from RefundRequest r, Order o, SysUser u " +
                    "where o.id = r.orderId and u.id = r.userId " +
                    "and (:status is null or :status = '' or r.status = :status) " +
                    "and (:keyword is null or :keyword = '' " +
                    "     or lower(r.orderNo) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(u.account) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')) " +
                    "     or lower(coalesce(u.realname, '')) like lower(concat('%', :keyword, '%')))")
    Page<RefundRequest> searchAdmin(@Param("keyword") String keyword,
                                    @Param("status") String status,
                                    Pageable pageable);
}
