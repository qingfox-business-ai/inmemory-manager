package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.ClientDTO;
import com.qingfox.inmemory.manager.service.RedisStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
public class ClientController {

    private final RedisStreamService redisStreamService;

    @GetMapping("/list")
    public ApiResponse<List<ClientDTO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(redisStreamService.getClients());
    }
}
