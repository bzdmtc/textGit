package com.example.smart_learning.seckill.service;

import com.example.smart_learning.seckill.config.SeckillKafkaConfig;
import com.example.smart_learning.seckill.dto.SeckillOrderMessage;
import com.example.smart_learning.seckill.repository.SeckillRecordRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SeckillKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillKafkaConsumer.class);

    private final SeckillOrderProcessor processor;
    private final SeckillRecordRepository recordRepository;
    private final SeckillStockService stockService;
    private final ObjectMapper objectMapper;

    public SeckillKafkaConsumer(SeckillOrderProcessor processor,
                                SeckillRecordRepository recordRepository,
                                SeckillStockService stockService,
                                ObjectMapper objectMapper) {
        this.processor = processor;
        this.recordRepository = recordRepository;
        this.stockService = stockService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = SeckillKafkaConfig.ORDER_TOPIC,
            groupId = SeckillKafkaConfig.CONSUMER_GROUP,
            containerFactory = "seckillKafkaListenerContainerFactory",
            autoStartup = "${app.seckill.consumer-enabled:true}")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        SeckillOrderMessage message;
        try {
            message = objectMapper.readValue(record.value(), SeckillOrderMessage.class);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("秒杀消息格式错误: " + record.value(), ex);
        }

        try {
            SeckillOrderProcessor.Result result = processor.process(message);
            if (result.outcome() == SeckillOrderProcessor.Outcome.FAILED
                    || result.outcome() == SeckillOrderProcessor.Outcome.NO_ACTIVITY) {
                // 建单失败：把 Redis 预扣的库存和限购名额还回去
                stockService.restoreAll(message.activityId(), message.userId());
            }
            ack.acknowledge();
        } catch (DataIntegrityViolationException ex) {
            // 唯一约束兜底：同一用户同一活动已有记录，说明重复消息，直接确认
            if (recordRepository.existsByUserIdAndActivityId(message.userId(), message.activityId())) {
                log.info("秒杀重复消息已忽略 activity={} user={} request={}",
                        message.activityId(), message.userId(), message.requestId());
                ack.acknowledge();
                return;
            }
            throw ex;
        }
    }
}
