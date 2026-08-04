package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.model.dto.TaskMonitorDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @GetMapping("/list")
    public ApiResponse<List<TaskDTO>> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return ApiResponse.success(Collections.emptyList());
    }

    @GetMapping("/{taskId}/monitor")
    public ApiResponse<TaskMonitorDTO> monitor(@PathVariable String taskId) {
        return ApiResponse.success(TaskMonitorDTO.builder()
                .taskId(taskId)
                .batches(Collections.emptyList())
                .build());
    }
}
