package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qingfox.inmemory.manager.entity.Task;
import com.qingfox.inmemory.manager.entity.TaskExecute;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.mapper.TaskExecuteMapper;
import com.qingfox.inmemory.manager.mapper.TaskMapper;
import com.qingfox.inmemory.manager.model.dto.StreamMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusSyncService {

    private final TaskMapper taskMapper;
    private final TaskBatchMapper taskBatchMapper;
    private final TaskExecuteMapper taskExecuteMapper;
    private final RedisStreamService redisStreamService;

    @Scheduled(fixedDelay = 10000)
    public void syncTaskStatus() {
        try {
            List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                    .ge(Task::getStartTime, LocalDate.now().minusDays(30).atStartOfDay()));
            if (tasks == null || tasks.isEmpty()) {
                return;
            }
            Map<String, long[]> queueStats = buildQueueStats();
            LocalDateTime dbNow = taskBatchMapper.selectDatabaseNow();
            for (Task task : tasks) {
                List<Map<String, Object>> counts = taskBatchMapper.selectBatchStatusCountByTaskId(task.getTaskId());
                long wait = 0, run = 0, done = 0, failure = 0;
                if (counts != null) {
                    for (Map<String, Object> row : counts) {
                        Number status = (Number) row.get("status");
                        Number cnt = (Number) row.get("cnt");
                        long c = cnt != null ? cnt.longValue() : 0;
                        int s = status != null ? status.intValue() : -1;
                        if (s == 0) wait = c;
                        else if (s == 1) run = c;
                        else if (s == 2) done = c;
                        else if (s == 3) failure = c;
                    }
                }
                List<TaskExecute> executes = taskExecuteMapper.selectExecutesByTaskId(task.getTaskId());
                long inputTotal = 0;
                long outputTotal = 0;
                if (executes != null) {
                    for (TaskExecute e : executes) {
                        if (task.getInputId() != null && task.getInputId().equals(e.getExecuteId())) {
                            inputTotal += e.getInCount() != null ? e.getInCount() : 0;
                        }
                        if (task.getOutputId() != null && task.getOutputId().equals(e.getExecuteId())) {
                            outputTotal += e.getOutCount() != null ? e.getOutCount() : 0;
                        }
                    }
                }
                long totalBatches = wait + run + done + failure;
                String errorStack = null;
                boolean timedOut = false;
                int newStatus;
                if (totalBatches == 0) {
                    if (task.getStartTime() != null && dbNow != null
                            && Duration.between(task.getStartTime(), dbNow).getSeconds() > 30) {
                        newStatus = 3;
                        errorStack = "Task timeout exception: no batches created within 30 seconds";
                        timedOut = true;
                    } else {
                        newStatus = 0;
                    }
                } else if (run + wait > 0) {
                    newStatus = 1;
                } else if (failure > 0) {
                    newStatus = 3;
                } else {
                    newStatus = 2;
                }
                Short curStatus = task.getStatus();
                boolean isFinal = curStatus != null && (curStatus == 2 || curStatus == 3);
                int finalStatus = isFinal ? curStatus.intValue() : newStatus;
                long[] qs = queueStats.getOrDefault(task.getTaskId(), new long[4]);
                boolean interrupted = !isFinal && qs[3] >= 0 && (qs[0] + qs[1] + qs[2]) == 0;
                if (interrupted) {
                    finalStatus = 3;
                    errorStack = "Task interrupted";
                }
                LambdaUpdateWrapper<Task> update = new LambdaUpdateWrapper<Task>()
                        .eq(Task::getId, task.getId())
                        .set(Task::getBatchWait, wait)
                        .set(Task::getBatchRun, run)
                        .set(Task::getBatchDone, done)
                        .set(Task::getBatchFailure, failure)
                        .set(Task::getQueueWait, qs[0])
                        .set(Task::getQueueRun, qs[1])
                        .set(Task::getQueueDone, qs[2])
                        .set(Task::getQueueFailure, qs[3])
                        .set(Task::getInputCount, inputTotal)
                        .set(Task::getOutputCount, outputTotal)
                        .set(Task::getStatus, (short) finalStatus);
                if (errorStack != null) {
                    update.set(Task::getErrorStack, errorStack);
                }
                if ((finalStatus == 2 || finalStatus == 3) && task.getEndTime() == null) {
                    if (timedOut || interrupted) {
                        update.set(Task::getEndTime, dbNow);
                    } else {
                        java.time.LocalDateTime maxEnd = taskBatchMapper.selectMaxEndTimeByTaskId(task.getTaskId());
                        if (maxEnd != null) {
                            update.set(Task::getEndTime, maxEnd);
                        }
                    }
                }
                taskMapper.update(null, update);
            }
            log.info("synced {} tasks status", tasks.size());
        } catch (Exception e) {
            log.error("sync task status failed", e);
        }
    }

    private Map<String, long[]> buildQueueStats() {
        Map<String, long[]> stats = new HashMap<>();
        List<StreamMessageDTO> messages = redisStreamService.getStreamMessages();
        if (messages == null || messages.isEmpty()) {
            return stats;
        }
        for (StreamMessageDTO m : messages) {
            Object tidObj = m.getAttribute() != null ? m.getAttribute().get("taskId") : null;
            if (tidObj == null) {
                continue;
            }
            String tid = tidObj.toString();
            long[] arr = stats.computeIfAbsent(tid, k -> new long[4]);
            String st = m.getStatus();
            if ("waiting".equals(st)) arr[0]++;
            else if ("running".equals(st)) arr[1]++;
            else if ("done".equals(st)) arr[2]++;
            else if ("failed".equals(st)) arr[3]++;
        }
        return stats;
    }
}
