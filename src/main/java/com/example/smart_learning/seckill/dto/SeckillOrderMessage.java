package com.example.smart_learning.seckill.dto;

/** Kafka seckill-order 消息体 */
public record SeckillOrderMessage(Long activityId, Long userId, String requestId) {
}
