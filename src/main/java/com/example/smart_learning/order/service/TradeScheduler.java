package com.example.smart_learning.order.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 交易定时任务入口：只负责触发，实际事务逻辑在 TradeSweepService 中执行。
 * 避免调度线程直接调用本类事务方法时绕过代理导致数据源路由错误。
 */
@Component
public class TradeScheduler {

    private final TradeSweepService tradeSweepService;

    public TradeScheduler(TradeSweepService tradeSweepService) {
        this.tradeSweepService = tradeSweepService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void run() {
        tradeSweepService.sweep();
    }
}
