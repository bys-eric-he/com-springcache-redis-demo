package com.springcache.redis.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * # @EnableConfigurationProperties注解给出了该配置类所需要的配置信息类，也就是JedisConfig类，
 * 这样spring容器才会去读取配置信息到SJedisConfig对象中。
 */
@Configuration
@EnableConfigurationProperties(JedisConfig.class)
public class JedisAutoConfig {

    private volatile JedisPool jedisPool = null;
    @Autowired
    private JedisConfig jedisConfig;

    /**
     * Jedis不支持单例，且非线程安全，每次获取实例时需要重新创建，否则会有异常！
     *
     * @return
     */
    @Bean
    public Jedis getJedis() {
        if (jedisPool == null) {
            //采用双锁机制，安全且在多线程情况下能保持高性能
            synchronized (JedisAutoConfig.class) {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(jedisConfig.getMaxTotal());
                config.setMaxIdle(jedisConfig.getMaxIdle());
                config.setMaxWaitMillis(jedisConfig.getMaxWait());
                config.setTestOnBorrow(jedisConfig.getTestOnBorrow());
                config.setTestOnReturn(jedisConfig.getTestOnReturn());
                jedisPool = new JedisPool(config, jedisConfig.getHost(), jedisConfig.getPort(),
                        jedisConfig.getTimeOut(), jedisConfig.getPassword());
            }
        }

        return jedisPool.getResource();
    }
}
