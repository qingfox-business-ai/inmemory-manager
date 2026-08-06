package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.DashboardSummaryDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() {
        DashboardSummaryDTO data = DashboardSummaryDTO.builder()
                .client(DashboardSummaryDTO.ClientSummary.builder()
                        .connected(0)
                        .disconnected(0)
                        .build())
                .queue(DashboardSummaryDTO.QueueSummary.builder()
                        .completed(0)
                        .pending(0)
                        .deadLetter(0)
                        .build())
                .task(DashboardSummaryDTO.TaskSummary.builder()
                        .success(0)
                        .failed(0)
                        .periodStart("")
                        .periodEnd("")
                        .build())
                .build();
        return ApiResponse.success(data);
    }
}
