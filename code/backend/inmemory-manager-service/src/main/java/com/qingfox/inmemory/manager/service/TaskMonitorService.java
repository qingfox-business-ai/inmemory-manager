package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.entity.Task;
import com.qingfox.inmemory.manager.entity.TaskBatch;
import com.qingfox.inmemory.manager.entity.TaskExecute;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.mapper.TaskExecuteMapper;
import com.qingfox.inmemory.manager.mapper.TaskMapper;
import com.qingfox.inmemory.manager.model.dto.ExecuteStatisticsDTO;
import com.qingfox.inmemory.manager.model.dto.PageResult;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.model.dto.BatchDTO;
import com.qingfox.inmemory.manager.model.dto.TaskListDTO;
import com.qingfox.inmemory.manager.model.dto.TaskMonitorDTO;
import com.qingfox.inmemory.manager.model.dto.TaskStatisticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskMonitorService {

    private final TaskBatchMapper taskBatchMapper;
    private final TaskExecuteMapper taskExecuteMapper;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ERRORS = 50;

    private static final java.time.format.DateTimeFormatter FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public List<TaskDTO> getTaskList(String taskId, String status, String startTime, String endTime) {
        String effectiveStart = (startTime == null || startTime.isEmpty())
                ? LocalDateTime.now().minusDays(7).format(FMT) : startTime;
        List<Map<String, Object>> rows = taskBatchMapper.selectTaskSummary(taskId, effectiveStart, endTime);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            int totalBatch = toInt(row.get("totalBatch"));
            int completedBatch = toInt(row.get("completedBatch"));
            int errorBatch = toInt(row.get("errorBatch"));
            int waitingBatch = toInt(row.get("waitingBatch"));
            String taskStatus;
            if (errorBatch > 0) {
                taskStatus = "failed";
            } else if (completedBatch >= totalBatch) {
                taskStatus = "success";
            } else if (waitingBatch >= totalBatch) {
                taskStatus = "waiting";
            } else {
                taskStatus = "running";
            }
            return TaskDTO.builder()
                    .taskId((String) row.get("taskId"))
                    .completedBatch(completedBatch)
                    .totalBatch(totalBatch)
                    .status(taskStatus)
                    .startTime(formatTimestamp(row.get("startTime")))
                    .endTime(formatTimestamp(row.get("endTime")))
                    .build();
        })
        .filter(dto -> status == null || status.isEmpty() || status.equals(dto.getStatus()))
        .collect(Collectors.toList());
    }

    public PageResult<TaskDTO> getTaskListPage(String taskId, String status, String startTime, String endTime, int page, int size) {
        List<TaskDTO> allForStats = getTaskList(taskId, null, startTime, endTime);
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("waiting", allForStats.stream().filter(t -> "waiting".equals(t.getStatus())).count());
        stats.put("running", allForStats.stream().filter(t -> "running".equals(t.getStatus())).count());
        stats.put("success", allForStats.stream().filter(t -> "success".equals(t.getStatus())).count());
        stats.put("failed", allForStats.stream().filter(t -> "failed".equals(t.getStatus())).count());

        List<TaskDTO> filtered = getTaskList(taskId, status, startTime, endTime);
        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<TaskDTO> list = from < to ? new ArrayList<>(filtered.subList(from, to)) : Collections.emptyList();
        return PageResult.<TaskDTO>builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .statusStats(stats)
                .build();
    }

    public PageResult<TaskListDTO> getTaskListPageFromTask(String taskId, Short status, String startTime, String endTime, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .like(taskId != null && !taskId.isEmpty(), Task::getTaskId, taskId)
                .eq(status != null, Task::getStatus, status)
                .ge(startTime != null && !startTime.isEmpty(), Task::getStartTime, startTime)
                .le(endTime != null && !endTime.isEmpty(), Task::getStartTime, endTime)
                .orderByDesc(Task::getStartTime);
        List<Task> tasks = taskMapper.selectList(wrapper);
        List<TaskListDTO> dtos = tasks.stream().map(this::toTaskListDTO).collect(Collectors.toList());
        int total = dtos.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<TaskListDTO> list = from < to ? new ArrayList<>(dtos.subList(from, to)) : Collections.emptyList();
        return PageResult.<TaskListDTO>builder()
                .list(list).total(total).page(page).size(size).build();
    }

    private TaskListDTO toTaskListDTO(Task t) {
        return TaskListDTO.builder()
                .taskId(t.getTaskId())
                .taskMark(t.getTaskMark())
                .status(t.getStatus() != null ? t.getStatus().intValue() : null)
                .inputCount(t.getInputCount())
                .outputCount(t.getOutputCount())
                .startTime(formatTimestamp(t.getStartTime()))
                .endTime(formatTimestamp(t.getEndTime()))
                .batchWait(t.getBatchWait())
                .batchRun(t.getBatchRun())
                .batchDone(t.getBatchDone())
                .batchFailure(t.getBatchFailure())
                .queueWait(t.getQueueWait())
                .queueRun(t.getQueueRun())
                .queueDone(t.getQueueDone())
                .queueFailure(t.getQueueFailure())
                .build();
    }

    public List<BatchDTO> getTaskBatches(String taskId) {
        List<TaskBatch> batches = taskBatchMapper.selectBatchesByTaskId(taskId);
        if (batches == null || batches.isEmpty()) {
            return Collections.emptyList();
        }
        return batches.stream().map(b -> BatchDTO.builder()
                .batchId(b.getBatchId())
                .status(b.getStatus() != null ? b.getStatus().intValue() : null)
                .updateTime(formatTimestamp(b.getUpdateTime()))
                .errorStack(b.getErrorStack())
                .startTime(formatTimestamp(b.getStartTime()))
                .endTime(formatTimestamp(b.getEndTime()))
                .build()).collect(Collectors.toList());
    }

    public TaskMonitorDTO getTaskMonitor(String taskId) {
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>().eq(Task::getTaskId, taskId));
        List<TaskExecute> executes = taskExecuteMapper.selectExecutesByTaskId(taskId);

        LocalDateTime dbNow = taskBatchMapper.selectDatabaseNow();
        TaskStatisticsDTO statistics = buildStatisticsFromTask(task, dbNow);
        List<ExecuteStatisticsDTO> executeStats = buildExecuteStatistics(executes);

        return TaskMonitorDTO.builder()
                .taskId(taskId)
                .statistics(statistics)
                .executes(executeStats)
                .build();
    }

    private TaskStatisticsDTO buildStatisticsFromTask(Task t, LocalDateTime dbNow) {
        if (t == null) {
            return TaskStatisticsDTO.builder()
                    .allCount(0).doneCount(0).runningCount(0).errorCount(0)
                    .status(0).startTime("").endTime("").nowTime("")
                    .duration(0)
                    .errorStack(Collections.emptyList())
                    .build();
        }
        long wait = t.getBatchWait() != null ? t.getBatchWait() : 0;
        long run = t.getBatchRun() != null ? t.getBatchRun() : 0;
        long done = t.getBatchDone() != null ? t.getBatchDone() : 0;
        long failure = t.getBatchFailure() != null ? t.getBatchFailure() : 0;
        LocalDateTime start = t.getStartTime();
        LocalDateTime end = t.getEndTime() != null ? t.getEndTime() : dbNow;
        long duration = (start != null && end != null) ? Math.max(0, Duration.between(start, end).toMillis()) : 0;
        return TaskStatisticsDTO.builder()
                .allCount((int) wait)
                .doneCount((int) done)
                .runningCount((int) run)
                .errorCount((int) failure)
                .status(t.getStatus() != null ? t.getStatus().intValue() : 0)
                .startTime(formatTimestamp(t.getStartTime()))
                .endTime(formatTimestamp(t.getEndTime()))
                .nowTime(formatTimestamp(t.getUpdateTime()))
                .duration(duration)
                .errorStack(Collections.emptyList())
                .build();
    }

    private TaskStatisticsDTO buildStatistics(List<TaskBatch> batches, LocalDateTime dbNow) {
        if (batches == null || batches.isEmpty()) {
            return TaskStatisticsDTO.builder()
                    .allCount(0).doneCount(0).runningCount(0).errorCount(0)
                    .status(2).startTime("").endTime("").nowTime(formatTimestamp(dbNow))
                    .duration(0)
                    .errorStack(Collections.emptyList())
                    .build();
        }

        int allCount = batches.size();
        int doneCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;

        for (TaskBatch b : batches) {
            if (b.getStatus() != null && b.getStatus() == 2) {
                doneCount++;
            }
            if (b.getErrorStack() != null && !b.getErrorStack().trim().isEmpty()) {
                errorCount++;
                if (errors.size() < MAX_ERRORS) {
                    String prefix = "批次 " + b.getBatchId() + " 异常: ";
                    collectErrors(errors, prefix, b.getErrorStack());
                }
            }
            if (b.getStartTime() != null && (minStart == null || b.getStartTime().isBefore(minStart))) {
                minStart = b.getStartTime();
            }
            if (b.getEndTime() != null && (maxEnd == null || b.getEndTime().isAfter(maxEnd))) {
                maxEnd = b.getEndTime();
            }
        }

        int runningCount = allCount - doneCount;

        LocalDateTime durEnd = maxEnd != null ? maxEnd : dbNow;
        long duration = (minStart != null) ? Math.max(0, Duration.between(minStart, durEnd).toMillis()) : 0;

        return TaskStatisticsDTO.builder()
                .allCount(allCount)
                .doneCount(doneCount)
                .runningCount(runningCount)
                .errorCount(errorCount)
                .status(runningCount > 0 ? 1 : 2)
                .startTime(formatTimestamp(minStart))
                .endTime(formatTimestamp(maxEnd))
                .nowTime(formatTimestamp(dbNow))
                .duration(duration)
                .errorStack(errors)
                .build();
    }

    private List<ExecuteStatisticsDTO> buildExecuteStatistics(List<TaskExecute> executes) {
        if (executes == null || executes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<TaskExecute>> grouped = executes.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getExecuteId() != null ? e.getExecuteId() : "unknown",
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ExecuteStatisticsDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<TaskExecute>> entry : grouped.entrySet()) {
            List<TaskExecute> group = entry.getValue();
            int runCount = sumInt(group, TaskExecute::getRunCount);
            int errorCount = sumInt(group, TaskExecute::getErrorCount);
            int inCount = sumInt(group, TaskExecute::getInCount);
            int outCount = sumInt(group, TaskExecute::getOutCount);
            int duration = sumInt(group, TaskExecute::getDuration);

            String executeType = group.get(0).getExecuteType();
            LocalDateTime minCreate = null;
            LocalDateTime maxUpdate = null;
            List<String> errors = new ArrayList<>();

            for (TaskExecute e : group) {
                if (e.getCreateTime() != null && (minCreate == null || e.getCreateTime().isBefore(minCreate))) {
                    minCreate = e.getCreateTime();
                }
                if (e.getUpdateTime() != null && (maxUpdate == null || e.getUpdateTime().isAfter(maxUpdate))) {
                    maxUpdate = e.getUpdateTime();
                }
                if (e.getErrorStack() != null && !e.getErrorStack().trim().isEmpty() && errors.size() < MAX_ERRORS) {
                    String prefix = "批次 " + e.getBatchId() + " 异常: ";
                    collectErrors(errors, prefix, e.getErrorStack());
                }
            }

            result.add(ExecuteStatisticsDTO.builder()
                    .executeId(entry.getKey())
                    .executeType(executeType)
                    .runCount(runCount)
                    .errorCount(errorCount)
                    .inCount(inCount)
                    .outCount(outCount)
                    .duration(duration)
                    .avgIn(safeDiv(inCount, runCount))
                    .avgOut(safeDiv(outCount, runCount))
                    .outPerSecond(duration == 0 ? 0 : (int) ((long) outCount * 1000L / duration))
                    .createTime(formatTimestamp(minCreate))
                    .updateTime(formatTimestamp(maxUpdate))
                    .errorStack(errors)
                    .build());
        }

        return result;
    }

    private void collectErrors(List<String> target, String prefix, String errorStackJson) {
        try {
            if (errorStackJson.trim().startsWith("[")) {
                List<String> parsed = objectMapper.readValue(errorStackJson, List.class);
                for (String err : parsed) {
                    if (target.size() >= MAX_ERRORS) break;
                    target.add(prefix + err);
                }
            } else {
                if (target.size() < MAX_ERRORS) {
                    target.add(prefix + errorStackJson);
                }
            }
        } catch (Exception e) {
            if (target.size() < MAX_ERRORS) {
                target.add(prefix + errorStackJson);
            }
        }
    }

    private int sumInt(List<TaskExecute> list, java.util.function.Function<TaskExecute, Integer> getter) {
        return list.stream().mapToInt(e -> getter.apply(e) != null ? getter.apply(e) : 0).sum();
    }

    private int safeDiv(int a, int b) {
        if (b == 0) return 0;
        return a / b;
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private String formatTimestamp(Object val) {
        if (val == null) return "";
        if (val instanceof LocalDateTime) return val.toString().replace('T', ' ').substring(0, 19);
        if (val instanceof Timestamp) return val.toString().substring(0, 19);
        return val.toString();
    }
}
