package com.example.smart_learning.seckill.service;

import com.example.smart_learning.seckill.entity.SeckillActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 秒杀库存的 Redis 侧操作：预热、Lua 原子扣减、回补。
 *
 * 返回码约定（claim）：
 *  >= 0 抢购成功，值为扣减后的剩余库存
 *  -1 活动未开始
 *  -2 活动已结束
 *  -3 超过每人限购
 *  -4 库存已抢光
 *  -5 库存未预热
 */
@Service
public class SeckillStockService {

    public static final long NOT_STARTED = -1;
    public static final long ENDED = -2;
    public static final long LIMIT_REACHED = -3;
    public static final long SOLD_OUT = -4;
    public static final long NOT_WARMED = -5;

    private static final Logger log = LoggerFactory.getLogger(SeckillStockService.class);

    private static final String CLAIM_LUA = """
            local now = tonumber(ARGV[1])
            if now < tonumber(ARGV[2]) then return -1 end
            if now > tonumber(ARGV[3]) then return -2 end
            local stock = redis.call('GET', KEYS[1])
            if not stock then return -5 end
            local bought = tonumber(redis.call('HGET', KEYS[2], ARGV[4]) or '0')
            if bought >= tonumber(ARGV[5]) then return -3 end
            if tonumber(stock) <= 0 then return -4 end
            redis.call('DECR', KEYS[1])
            redis.call('HINCRBY', KEYS[2], ARGV[4], 1)
            redis.call('EXPIRE', KEYS[1], ARGV[6])
            redis.call('EXPIRE', KEYS[2], ARGV[6])
            return tonumber(stock) - 1
            """;

    private static final String RESTORE_STOCK_LUA = """
            local stock = redis.call('GET', KEYS[1])
            if stock then redis.call('INCR', KEYS[1]) end
            return 1
            """;

    /** 回补库存并撤销用户限购占用（消息发送失败、建单失败时使用） */
    private static final String RESTORE_ALL_LUA = """
            local stock = redis.call('GET', KEYS[1])
            if stock then redis.call('INCR', KEYS[1]) end
            local bought = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
            if bought > 1 then
                redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
            elseif bought == 1 then
                redis.call('HDEL', KEYS[2], ARGV[1])
            end
            return 1
            """;

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT =
            new DefaultRedisScript<>(CLAIM_LUA, Long.class);
    private static final DefaultRedisScript<Long> RESTORE_STOCK_SCRIPT =
            new DefaultRedisScript<>(RESTORE_STOCK_LUA, Long.class);
    private static final DefaultRedisScript<Long> RESTORE_ALL_SCRIPT =
            new DefaultRedisScript<>(RESTORE_ALL_LUA, Long.class);

    private final StringRedisTemplate redis;

    public SeckillStockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 从 DB 的 available_stock 预热 Redis 库存（不重置用户限购记录） */
    public boolean warmUp(SeckillActivity activity) {
        try {
            redis.opsForValue().set(stockKey(activity.getId()),
                    String.valueOf(activity.getAvailableStock()), ttl(activity, true));
            return true;
        } catch (DataAccessException ex) {
            log.warn("秒杀库存预热失败 activity={}: {}", activity.getId(), ex.getMessage());
            return false;
        }
    }

    public long claim(SeckillActivity activity, Long userId, long nowMillis) {
        try {
            String userIdArg = String.valueOf(userId);
            Long result = redis.execute(CLAIM_SCRIPT,
                    List.of(stockKey(activity.getId()), usersKey(activity.getId())),
                    String.valueOf(nowMillis),
                    String.valueOf(toMillis(activity.getStartAt())),
                    String.valueOf(toMillis(activity.getEndAt())),
                    userIdArg,
                    String.valueOf(activity.getPerUserLimit()),
                    String.valueOf(ttl(activity, false).toSeconds()));
            return result == null ? NOT_WARMED : result;
        } catch (DataAccessException ex) {
            log.warn("秒杀 Lua 扣减失败 activity={} user={}: {}",
                    activity.getId(), userId, ex.getMessage());
            return NOT_WARMED;
        }
    }

    public Integer remaining(Long activityId) {
        try {
            String value = redis.opsForValue().get(stockKey(activityId));
            return value == null ? null : Integer.parseInt(value);
        } catch (DataAccessException ex) {
            return null;
        }
    }

    public boolean hasClaimed(Long activityId, Long userId) {
        try {
            Object value = redis.opsForHash().get(usersKey(activityId), String.valueOf(userId));
            return value != null && Long.parseLong(value.toString()) > 0;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    /** 取消/超时：只回补库存，保留用户限购资格，避免取消后反复抢购 */
    public void restoreStock(Long activityId) {
        try {
            redis.execute(RESTORE_STOCK_SCRIPT, List.of(stockKey(activityId)));
        } catch (DataAccessException ex) {
            log.warn("秒杀库存回补失败 activity={}: {}", activityId, ex.getMessage());
        }
    }

    /** 建单失败：回补库存并释放用户限购名额 */
    public void restoreAll(Long activityId, Long userId) {
        try {
            redis.execute(RESTORE_ALL_SCRIPT,
                    List.of(stockKey(activityId), usersKey(activityId)),
                    String.valueOf(userId));
        } catch (DataAccessException ex) {
            log.warn("秒杀库存/资格回补失败 activity={} user={}: {}",
                    activityId, userId, ex.getMessage());
        }
    }

    /** 管理端调整总名额时同步增减 Redis 库存（仅当库存键存在） */
    public void adjustStock(Long activityId, int delta) {
        try {
            redis.opsForValue().increment(stockKey(activityId), delta);
        } catch (DataAccessException ex) {
            log.warn("秒杀库存调整失败 activity={}: {}", activityId, ex.getMessage());
        }
    }

    public boolean stockExists(Long activityId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(stockKey(activityId)));
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public void clearStock(Long activityId) {
        try {
            redis.delete(stockKey(activityId));
        } catch (DataAccessException ex) {
            log.warn("秒杀库存清理失败 activity={}: {}", activityId, ex.getMessage());
        }
    }

    public void clear(Long activityId) {
        try {
            redis.delete(List.of(stockKey(activityId), usersKey(activityId)));
        } catch (DataAccessException ex) {
            log.warn("秒杀缓存清理失败 activity={}: {}", activityId, ex.getMessage());
        }
    }

    public static String stockKey(Long activityId) {
        return "seckill:stock:" + activityId;
    }

    public static String usersKey(Long activityId) {
        return "seckill:users:" + activityId;
    }

    private Duration ttl(SeckillActivity activity, boolean forceAtLeastOneDay) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Duration remaining = Duration.between(now, activity.getEndAt()).plusHours(24);
        if (remaining.isNegative() || remaining.isZero()) {
            remaining = Duration.ofHours(1);
        }
        if (forceAtLeastOneDay && remaining.compareTo(Duration.ofDays(1)) < 0) {
            remaining = Duration.ofDays(1);
        }
        return remaining;
    }

    private long toMillis(LocalDateTime time) {
        return time.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }
}
