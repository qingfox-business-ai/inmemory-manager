package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.PageResult;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.model.dto.BatchDTO;
import com.qingfox.inmemory.manager.model.dto.TaskListDTO;
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
    public ApiResponse<PageResult<TaskListDTO>> list(
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "status", required = false) Short status,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.success(taskMonitorService.getTaskListPageFromTask(taskId, status, startTime, endTime, page, size));
    }

    @GetMapping("/{taskId}/monitor")
    public ApiResponse<TaskMonitorDTO> monitor(@PathVariable String taskId) {
        return ApiResponse.success(taskMonitorService.getTaskMonitor(taskId));
    }

    @GetMapping("/{taskId}/batches")
    public ApiResponse<List<BatchDTO>> batches(@PathVariable String taskId) {
        return ApiResponse.success(taskMonitorService.getTaskBatches(taskId));
    }
}
