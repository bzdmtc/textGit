package com.example.smart_learning.order.entity;

import com.example.smart_learning.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40, unique = true)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "pay_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Column(name = "user_coupon_id")
    private Long userCouponId;

    /** NORMAL / SECKILL */
    @Column(name = "order_source", nullable = false, length = 16)
    private String orderSource = "NORMAL";

    @Column(name = "seckill_activity_id")
    private Long seckillActivityId;

    @Column(nullable = false, length = 32)
    private String status = "PENDING_PAYMENT";

    @Column(name = "pay_method", length = 32)
    private String payMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
