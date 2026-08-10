package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qingfox.inmemory.manager.entity.Task;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusSyncService {

    private final TaskMapper taskMapper;
    private final TaskBatchMapper taskBatchMapper;

    @Scheduled(fixedDelay = 10000)
    public void syncTaskStatus() {
        try {
            List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                    .notIn(Task::getStatus, (short) 2, (short) 3));
            if (tasks == null || tasks.isEmpty()) {
                return;
            }
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
                int newStatus;
                if (run + wait > 0) {
                    newStatus = 1;
                } else if (failure > 0) {
                    newStatus = 3;
                } else {
                    newStatus = 2;
                }
                taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                        .eq(Task::getId, task.getId())
                        .set(Task::getBatchWait, wait)
                        .set(Task::getBatchRun, run)
                        .set(Task::getBatchDone, done)
                        .set(Task::getBatchFailure, failure)
                        .set(Task::getStatus, (short) newStatus));
            }
            log.info("synced {} tasks status", tasks.size());
        } catch (Exception e) {
            log.error("sync task status failed", e);
        }
    }
}
