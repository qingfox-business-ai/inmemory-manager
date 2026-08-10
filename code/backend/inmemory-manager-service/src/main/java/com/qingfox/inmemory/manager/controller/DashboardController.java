package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.ClientDTO;
import com.qingfox.inmemory.manager.model.dto.StreamMessageDTO;
import com.qingfox.inmemory.manager.model.dto.DashboardSummaryDTO;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.service.RedisStreamService;
import com.qingfox.inmemory.manager.service.TaskMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final TaskMonitorService taskMonitorService;
    private final RedisStreamService redisStreamService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() {
        String startTime = LocalDateTime.now().minusDays(7).format(FMT);
        String endTime = LocalDateTime.now().format(FMT);
        List<TaskDTO> tasks = taskMonitorService.getTaskList(null, null, startTime, null);

        int success = 0;
        int waiting = 0;
        int failed = 0;
        for (TaskDTO t : tasks) {
            if ("success".equals(t.getStatus())) {
                success++;
            } else if ("waiting".equals(t.getStatus())) {
                waiting++;
            } else if ("failed".equals(t.getStatus())) {
                failed++;
            }
        }

        List<ClientDTO> clients = redisStreamService.getClients();
        int connected = (int) clients.stream().filter(c -> "online".equals(c.getStatus())).count();
        int disconnected = (int) clients.stream().filter(c -> "offline".equals(c.getStatus())).count();

        List<StreamMessageDTO> messages = redisStreamService.getStreamMessages();
        int queueCompleted = (int) messages.stream().filter(m -> "done".equals(m.getStatus())).count();
        int queuePending = (int) messages.stream().filter(m -> "pending".equals(m.getStatus())).count();
        int queueDeadLetter = (int) messages.stream().filter(m -> "failed".equals(m.getStatus())).count();

        DashboardSummaryDTO data = DashboardSummaryDTO.builder()
                .client(DashboardSummaryDTO.ClientSummary.builder()
                        .connected(connected).disconnected(disconnected).build())
                .queue(DashboardSummaryDTO.QueueSummary.builder()
                        .completed(queueCompleted).pending(queuePending).deadLetter(queueDeadLetter).build())
                .task(DashboardSummaryDTO.TaskSummary.builder()
                        .success(success).waiting(waiting).failed(failed)
                        .periodStart(startTime).periodEnd(endTime).build())
                .build();
        return ApiResponse.success(data);
    }
}
