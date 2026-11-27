package com.example.smart_learning.seckill.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.order.event.OrderCanceledEvent;
import com.example.smart_learning.order.event.OrderPaidEvent;
import com.example.smart_learning.seckill.entity.SeckillRecord;
import com.example.smart_learning.seckill.repository.SeckillActivityRepository;
import com.example.smart_learning.seckill.repository.SeckillRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 交易事件 → 秒杀记录同步：
 * - 支付成功：CREATED → PAID（库存已在建单时扣减，不再变动）
 * - 取消/超时：CREATED → CANCELED，并回补 DB 与 Redis 库存
 */
@Component
public class SeckillOrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderEventListener.class);

    private final SeckillRecordRepository recordRepository;
    private final SeckillActivityRepository activityRepository;
    private final SeckillStockService stockService;

    public SeckillOrderEventListener(SeckillRecordRepository recordRepository,
                                     SeckillActivityRepository activityRepository,
                                     SeckillStockService stockService) {
        this.recordRepository = recordRepository;
        this.activityRepository = activityRepository;
        this.stockService = stockService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Master
    public void onPaid(OrderPaidEvent event) {
        try {
            recordRepository.findByOrderNo(event.orderNo()).ifPresent(record -> {
                if ("CREATED".equals(record.getStatus())) {
                    record.setStatus("PAID");
                    recordRepository.save(record);
                    log.info("秒杀记录已支付 activity={} user={} order={}",
                            record.getActivityId(), record.getUserId(), record.getOrderNo());
                }
            });
        } catch (RuntimeException ex) {
            log.error("秒杀记录支付状态同步失败 order={}", event.orderNo(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Master
    public void onCanceled(OrderCanceledEvent event) {
        try {
            SeckillRecord record = recordRepository.findByOrderNo(event.orderNo()).orElse(null);
            if (record == null || !"CREATED".equals(record.getStatus())) {
                return;
            }
            record.setStatus("CANCELED");
            record.setReason("订单取消/超时，名额已释放");
            recordRepository.save(record);
            activityRepository.increaseStock(record.getActivityId());
            Long activityId = record.getActivityId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stockService.restoreStock(activityId);
                }
            });
            log.info("秒杀订单取消，库存已回补 activity={} user={} order={}",
                    record.getActivityId(), record.getUserId(), record.getOrderNo());
        } catch (RuntimeException ex) {
            log.error("秒杀订单取消回补失败 order={}", event.orderNo(), ex);
        }
    }
}
