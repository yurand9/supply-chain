package com.b2b.supplychain.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis 分布式锁工具。
 * 用途：防止客户网络卡顿导致的重复下单。
 */
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试加锁。
     *
     * @param key           锁的 key（例如订单号）
     * @param value         锁的 value（一般用唯一标识，释放时校验，防止误删别人的锁）
     * @param expireSeconds 锁的过期时间（秒）
     * @return true=加锁成功，false=已被别人持有
     */
    public boolean tryLock(String key, String value, long expireSeconds) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放锁。用 Lua 脚本保证「只有 value 匹配才删除」，避免误删别人的锁。
     *
     * @param key   锁的 key
     * @param value 加锁时设置的 value
     */
    public void unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        redisTemplate.execute(redisScript, Collections.singletonList(key), value);
    }
}
