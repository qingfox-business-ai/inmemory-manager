package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.StreamInfoDTO;
import com.qingfox.inmemory.manager.service.RedisStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stream")
@Tag(name = "Redis Stream", description = "Redis Stream cache task distribution")
@RequiredArgsConstructor
public class StreamController {

    private final RedisStreamService redisStreamService;

    @GetMapping("/info")
    @Operation(summary = "Get Redis Stream info", description = "Query stream total message count, each message attributes and status")
    public ApiResponse<StreamInfoDTO> getStreamInfo() {
        return ApiResponse.success(redisStreamService.getStreamInfo());
    }
}
