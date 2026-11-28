package com.example.smart_learning.auth.service;

import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证接口的 IP 级限流：防止换账号/换手机号绕过单账号锁定后继续爆破。
 * 使用固定窗口计数，窗口内首次访问时设置过期时间。
 */
@Service
public class AuthThrottleService {

    private final StringRedisTemplate redis;

    public AuthThrottleService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String scope, String ip, int limit, Duration window) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        String key = "auth:throttle:" + scope + ":" + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
        }
    }
}
