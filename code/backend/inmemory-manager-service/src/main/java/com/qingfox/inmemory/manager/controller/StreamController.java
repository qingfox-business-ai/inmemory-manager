package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.StreamInfoDTO;
import com.qingfox.inmemory.manager.model.dto.PageResult;
import com.qingfox.inmemory.manager.model.dto.StreamMessageDTO;
import com.qingfox.inmemory.manager.service.RedisStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
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

    @GetMapping("/messages")
    @Operation(summary = "Get polled stream messages", description = "Return in-memory list of polled stream messages (attribute, id, ack)")
    public ApiResponse<PageResult<StreamMessageDTO>> messages(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "taskId", required = false) String taskId) {
        PageResult<StreamMessageDTO> data = redisStreamService.getStreamMessagesPage(page, size, status, search, taskId);
        log.info("access stream messages api, page={}, size={}, total={}", page, size, data.getTotal());
        return ApiResponse.success(data);
    }
}
