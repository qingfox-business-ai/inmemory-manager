package com.qingfox.inmemory.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingfox.inmemory.manager.entity.Task;
import com.qingfox.inmemory.manager.mapper.TaskMapper;
import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.ClientDTO;
import com.qingfox.inmemory.manager.model.dto.DashboardSummaryDTO;
import com.qingfox.inmemory.manager.service.RedisStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FMT_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final TaskMapper taskMapper;
    private final RedisStreamService redisStreamService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7);
        String startTime = startDate.atStartOfDay().format(FMT);
        String endTime = today.atTime(23, 59, 59).format(FMT);

        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .ge(Task::getStartTime, startTime));
        int waiting = 0, running = 0, success = 0, failed = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                Short s = t.getStatus();
                if (s == null) continue;
                switch (s.intValue()) {
                    case 0: waiting++; break;
                    case 1: running++; break;
                    case 2: success++; break;
                    case 3: failed++; break;
                }
            }
        }

        List<ClientDTO> clients = redisStreamService.getClients();
        int connected = (int) clients.stream().filter(c -> "online".equals(c.getStatus())).count();
        int disconnected = (int) clients.stream().filter(c -> "offline".equals(c.getStatus())).count();

        DashboardSummaryDTO data = DashboardSummaryDTO.builder()
                .client(DashboardSummaryDTO.ClientSummary.builder()
                        .connected(connected).disconnected(disconnected).build())
                .task(DashboardSummaryDTO.TaskSummary.builder()
                        .success(success).waiting(waiting).running(running).failed(failed)
                        .periodStart(startDate.format(FMT_DAY)).periodEnd(today.format(FMT_DAY)).build())
                .build();
        return ApiResponse.success(data);
    }
}
