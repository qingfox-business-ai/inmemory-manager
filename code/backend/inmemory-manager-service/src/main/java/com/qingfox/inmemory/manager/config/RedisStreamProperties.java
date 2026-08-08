package com.qingfox.inmemory.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "inmemory.job")
public class RedisStreamProperties {

    private String redisStream;
    private String consumerGroup;
}
