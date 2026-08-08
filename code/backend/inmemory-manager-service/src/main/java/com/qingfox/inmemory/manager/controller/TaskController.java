package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.model.dto.TaskMonitorDTO;
import com.qingfox.inmemory.manager.service.TaskMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskMonitorService taskMonitorService;

    @GetMapping("/list")
    public ApiResponse<List<TaskDTO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return ApiResponse.success(taskMonitorService.getTaskList(taskId, status, startTime, endTime));
    }

    @GetMapping("/{taskId}/monitor")
    public ApiResponse<TaskMonitorDTO> monitor(@PathVariable String taskId) {
        return ApiResponse.success(taskMonitorService.getTaskMonitor(taskId));
    }
}
