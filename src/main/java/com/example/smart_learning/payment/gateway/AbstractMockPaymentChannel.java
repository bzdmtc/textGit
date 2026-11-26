package com.example.smart_learning.payment.gateway;

import com.example.smart_learning.order.entity.Order;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractMockPaymentChannel implements PaymentChannel {

    @Override
    public PaymentResult pay(Order order) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String transactionNo = "MOCK" + code().replace("_", "") + now.toLocalDate().toString().replace("-", "")
                + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        return new PaymentResult(true, transactionNo, now, "模拟支付成功");
    }
}
