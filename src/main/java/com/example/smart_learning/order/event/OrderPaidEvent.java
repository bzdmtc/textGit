package com.example.smart_learning.order.event;

/** 订单支付成功后发布；秒杀模块据此把抢购记录标记为已支付 */
public record OrderPaidEvent(String orderNo) {
}
