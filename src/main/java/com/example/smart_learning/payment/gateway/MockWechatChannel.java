package com.example.smart_learning.payment.gateway;

import org.springframework.stereotype.Component;

@Component
public class MockWechatChannel extends AbstractMockPaymentChannel {

    @Override
    public String code() {
        return "MOCK_WECHAT";
    }

    @Override
    public String displayName() {
        return "模拟微信支付";
    }
}
