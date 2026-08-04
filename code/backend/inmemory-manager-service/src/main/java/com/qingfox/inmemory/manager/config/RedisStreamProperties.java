package com.qingfox.inmemory.manager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "inmemory.job.redis-stream")
public class RedisStreamProperties {

    private String stream;
    private String consumerGroup;
}
