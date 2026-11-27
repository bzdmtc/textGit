package com.example.smart_learning.seckill.service;

import com.example.smart_learning.seckill.entity.SeckillActivity;

import java.time.LocalDateTime;

/** 秒杀活动展示阶段：OFF / UPCOMING / ACTIVE / ENDED */
public final class SeckillPhase {

    public static final String OFF = "OFF";
    public static final String UPCOMING = "UPCOMING";
    public static final String ACTIVE = "ACTIVE";
    public static final String ENDED = "ENDED";

    private SeckillPhase() {
    }

    public static String of(SeckillActivity activity, LocalDateTime now) {
        if (!"ON".equals(activity.getStatus())) {
            return OFF;
        }
        if (now.isBefore(activity.getStartAt())) {
            return UPCOMING;
        }
        if (now.isAfter(activity.getEndAt())) {
            return ENDED;
        }
        return ACTIVE;
    }
}
