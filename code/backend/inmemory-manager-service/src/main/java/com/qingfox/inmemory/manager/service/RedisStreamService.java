package com.qingfox.inmemory.manager.service;

import com.qingfox.inmemory.manager.config.RedisStreamProperties;
import com.qingfox.inmemory.manager.model.dto.StreamInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStreamService {

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamProperties properties;

    public StreamInfoDTO getStreamInfo() {
        String streamKey = properties.getStream();

        return StreamInfoDTO.builder()
                .streamName(streamKey)
                .totalMessageCount(0)
                .messages(Collections.emptyList())
                .build();
    }
}
