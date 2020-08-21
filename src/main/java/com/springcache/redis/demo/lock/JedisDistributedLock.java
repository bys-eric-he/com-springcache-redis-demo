package com.springcache.redis.demo.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

/**
 * jedis(3.x)分布式锁
 */
@Component
@Slf4j
public class JedisDistributedLock {

    //加锁成功标志
    private static final String LOCK_SUCCESS = "OK";
    //解锁成功标志
    private static final String RELEASE_SUCCESS = "1";
    //加锁标志
    private static final String SET_IF_NOT_EXIST = "NX";
    //给锁设置过期时间的标志
    private static final String SET_WITH_PX_TIME = "PX";

    /**
     * 尝试获得锁
     *
     * @param jedis     redis实例
     * @param key       具体的锁
     * @param value     锁的值，解锁的时候用
     * @param lockMills 锁定毫秒值
     * @return 加锁成功标志
     */
    public boolean getDistributedLock(Jedis jedis, String key, String value, int lockMills) {
        boolean isLocked = false;
        try {
            long beginMills = System.currentTimeMillis();
            long tryMills = lockMills + 100;//尝试获取分布式锁的时间
            SetParams setParams = new SetParams();
            //只在键不存在时才进行设置操作
            setParams.nx();
            while (System.currentTimeMillis() - beginMills < tryMills) {
                String result = jedis.set(key, value, setParams.px(lockMills));
                if (LOCK_SUCCESS.equals(result)) {
                    isLocked = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("----getDistributedLock error------", e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return isLocked;
    }

    /**
     * 尝试解锁(删除key)
     *
     * @param jedis redis实例
     * @param key   具体的锁
     * @param value
     * @return
     */
    public boolean releaseDistributedLock(Jedis jedis, String key, String value) {
        boolean isUnlocked = false;
        try {
            //获取锁对应的value值，检查是否与requestId相等，如果相等则删除锁（解锁);eval()方法执行Lua脚本是原子性的
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(key), Collections.singletonList(value));
            if (RELEASE_SUCCESS.equals(result.toString())) {
                isUnlocked = true;
            }
        } catch (Exception e) {
            log.error("------releaseDistributedLock error------", e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return isUnlocked;
    }
}
