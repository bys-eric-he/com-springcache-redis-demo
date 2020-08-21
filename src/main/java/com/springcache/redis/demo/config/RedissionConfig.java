package com.springcache.redis.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@PropertySource("classpath:redission.properties")
@ConfigurationProperties(ignoreUnknownFields = false,
        prefix = "redission")
public class RedissionConfig {
    private String host;
    private String password;
}
