package com.example.smart_learning.payment.gateway;

import com.example.smart_learning.order.entity.Order;

/**
 * 支付渠道抽象。本期只提供模拟支付实现，后续接入支付宝/微信时新增实现即可，
 * 上层 PaymentService 与收银台不需要改动。
 */
public interface PaymentChannel {

    String code();

    String displayName();

    PaymentResult pay(Order order);
}
