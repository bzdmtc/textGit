package com.example.smart_learning.payment.entity;

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
@Table(name = "payment_record")
public class PaymentRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(name = "transaction_no", nullable = false, length = 64, unique = true)
    private String transactionNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** SUCCESS / REFUNDED */
    @Column(nullable = false, length = 16)
    private String status = "SUCCESS";

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;
}
