package com.springcache.redis.demo.cache;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CacheTemplate<T> {

    /**
     * RedisTemplate中定义了5种数据结构操作
     * 1. redisTemplate.opsForValue();　　//操作字符串
     * 2. redisTemplate.opsForHash();　　 //操作hash
     * 3. redisTemplate.opsForList();　　 //操作list
     * 4. redisTemplate.opsForSet();　　  //操作set
     * 5. redisTemplate.opsForZSet();　 　//操作有序set
     */
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //加锁的KEY前缀,加锁的KEY不能和存储数据的KEY使用一样的KEY，否则会导致因KEY相同缓存中数据被重置。
    private static final String LOCK_KEY = "LOCK_KEY_";

    /**
     * 加锁从缓存中获取数据, 防止缓存被击穿
     *
     * @param key
     * @param expire
     * @param timeUnit
     * @param cacheLoadable
     * @param clazz
     * @return
     */
    public T getCacheData(String key, long expire, TimeUnit timeUnit, CacheLoadable<T> cacheLoadable, Class<T> clazz) {

        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        // 获取key键对应的值
        Object value = valueOperations.get(key);

        // 命中缓存
        if (value != null) {
            return JSON.parseObject(JSON.toJSONString(JSON.parse(value.toString())), clazz);
        }

        // 获取锁对象
        RLock lock = redissonClient.getLock(LOCK_KEY + key);
        try {
            // 加锁，并且设置锁过期时间，防止死锁的产生
            boolean lockFlag = lock.tryLock(30, 60, TimeUnit.SECONDS);
            if (lockFlag) {
                //加锁成功后 再次尝试读取缓存中是否有值,高并发情况下
                value = valueOperations.get(key);
                // 命中缓存
                if (value != null) {
                    return JSON.parseObject(JSON.toJSONString(JSON.parse(value.toString())), clazz);
                }

                T result = cacheLoadable.load();
                if (result != null) {
                    log.error("从DB中获取到数据并存入缓存!->" + JSON.toJSONString(result));
                    //加入缓存
                    valueOperations.set(key, JSON.toJSONString(result), expire, timeUnit);
                    return result;
                }
            }
        } catch (Exception ex) {
            log.error("获取缓存操作异常!->" + ex.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }
}
