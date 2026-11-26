package com.example.smart_learning.payment.repository;

import com.example.smart_learning.payment.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findFirstByOrderNoAndStatusOrderByIdDesc(String orderNo, String status);

    List<PaymentRecord> findByOrderNoOrderByIdDesc(String orderNo);

    List<PaymentRecord> findByOrderNoIn(List<String> orderNos);
}
