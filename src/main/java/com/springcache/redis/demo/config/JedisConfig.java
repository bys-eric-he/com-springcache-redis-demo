package com.springcache.redis.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@PropertySource("classpath:jedis.properties")
@ConfigurationProperties(ignoreUnknownFields = false,
        prefix = "jedis")
public class JedisConfig {
    private String host;
    private Integer port;
    private String password;

    private int timeOut;
    private int maxTotal;
    private int maxIdle;
    private int maxWait;

    private Boolean testOnBorrow;
    private Boolean testOnReturn;
}
