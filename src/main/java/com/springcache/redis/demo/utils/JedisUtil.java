package com.springcache.redis.demo.utils;

import com.springcache.redis.demo.config.JedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
@EnableConfigurationProperties(JedisConfig.class)
public class JedisUtil {
    @Autowired
    private JedisConfig jedisConfig;

    private JedisPool jedisPool = null;

    /**
     * 获取 Jedis 实例
     *
     * @return
     */
    public Jedis getJedis() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(jedisConfig.getMaxTotal());
            config.setMaxIdle(jedisConfig.getMaxIdle());
            config.setMaxWaitMillis(jedisConfig.getMaxWait());
            config.setTestOnBorrow(jedisConfig.getTestOnBorrow());
            config.setTestOnReturn(jedisConfig.getTestOnReturn());
            jedisPool = new JedisPool(config, jedisConfig.getHost(), jedisConfig.getPort(),
                    jedisConfig.getTimeOut(), jedisConfig.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jedisPool != null) {
            return jedisPool.getResource();
        }
        return null;
    }
}
