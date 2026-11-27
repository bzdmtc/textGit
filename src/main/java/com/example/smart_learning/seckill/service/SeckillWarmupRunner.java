package com.example.smart_learning.seckill.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.seckill.entity.SeckillActivity;
import com.example.smart_learning.seckill.repository.SeckillActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 启动时把未结束活动的 DB 库存预热到 Redis（Redis 不可用时只告警，不影响主链路） */
@Component
public class SeckillWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeckillWarmupRunner.class);

    private final SeckillActivityRepository activityRepository;
    private final SeckillStockService stockService;

    public SeckillWarmupRunner(SeckillActivityRepository activityRepository,
                               SeckillStockService stockService) {
        this.activityRepository = activityRepository;
        this.stockService = stockService;
    }

    @Override
    @Master
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        var activities = activityRepository.findOnAndNotEnded(LocalDateTime.now(ZoneOffset.UTC));
        int warmed = 0;
        for (SeckillActivity activity : activities) {
            if (stockService.warmUp(activity)) {
                warmed++;
            }
        }
        log.info("秒杀库存预热完成：活动 {} 个，成功 {} 个", activities.size(), warmed);
    }
}
