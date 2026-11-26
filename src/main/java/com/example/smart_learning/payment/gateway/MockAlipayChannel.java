package com.example.smart_learning.payment.gateway;

import org.springframework.stereotype.Component;

@Component
public class MockAlipayChannel extends AbstractMockPaymentChannel {

    @Override
    public String code() {
        return "MOCK_ALIPAY";
    }

    @Override
    public String displayName() {
        return "模拟支付宝";
    }
}
