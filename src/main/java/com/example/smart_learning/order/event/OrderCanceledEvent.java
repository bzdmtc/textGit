package com.example.smart_learning.order.event;

/** 订单取消（含超时关单）后发布；秒杀模块据此回补库存 */
public record OrderCanceledEvent(String orderNo) {
}
