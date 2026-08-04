package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.MessageConfigDTO;
import com.qingfox.inmemory.manager.model.dto.MessageDataDTO;
import com.qingfox.inmemory.manager.model.dto.MessageDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @GetMapping("/list")
    public ApiResponse<List<MessageDTO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(Collections.emptyList());
    }

    @GetMapping("/{messageId}/config")
    public ApiResponse<MessageConfigDTO> config(@PathVariable String messageId) {
        return ApiResponse.success(MessageConfigDTO.builder().build());
    }

    @GetMapping("/{messageId}/data")
    public ApiResponse<MessageDataDTO> data(@PathVariable String messageId) {
        return ApiResponse.success(MessageDataDTO.builder().build());
    }
}
