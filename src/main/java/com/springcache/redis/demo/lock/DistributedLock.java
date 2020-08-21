package com.springcache.redis.demo.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁
 */
@Component
@Slf4j
public class DistributedLock {
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 获取锁
     * 通过redis的setnx方式（不存在则设置）,往redis上设置一个带有过期时间的key
     * 如果设置成功，则获得了分布式锁
     * SET lockId content PX millisecond NX
     *
     * @param key
     * @param value
     * @param millisecond
     * @return
     */
    public boolean tryLock(String key, String value, long millisecond) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
                key,
                value,
                millisecond,
                TimeUnit.MILLISECONDS);

        if (success != null && success) {
            return true;
        }

        String currentValue = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(currentValue) && Long.parseLong(currentValue) < System.currentTimeMillis()) {
            //获取上一个锁的时间 高并发的情况可能会出现已经被修改的问题  所以多一次判断保证线程的安全
            String oldValue = redisTemplate.opsForValue().getAndSet(key, value);
            return StringUtils.isNotEmpty(oldValue) && oldValue.equals(currentValue);
        }

        return false;
    }

    /**
     * 释放锁
     * 当处理完业务逻辑后，需要手动的把锁释放掉
     *
     * @param key
     * @param value
     */
    public void unlock(String key, String value) {
        String currentValue = redisTemplate.opsForValue().get(key);
        try {
            if (StringUtils.isNotEmpty(currentValue) && currentValue.equals(value)) {
                redisTemplate.opsForValue().getOperations().delete(key);
                log.info("已成功释放锁!");
            }
        } catch (Exception e) {
            log.error("释放锁异常->" + e.getMessage());
        }
    }
}
