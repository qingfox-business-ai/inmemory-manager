package com.qingfox.inmemory.manager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.entity.TaskBatch;
import com.qingfox.inmemory.manager.entity.TaskExecute;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.mapper.TaskExecuteMapper;
import com.qingfox.inmemory.manager.model.dto.ExecuteStatisticsDTO;
import com.qingfox.inmemory.manager.model.dto.TaskDTO;
import com.qingfox.inmemory.manager.model.dto.TaskMonitorDTO;
import com.qingfox.inmemory.manager.model.dto.TaskStatisticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskMonitorService {

    private final TaskBatchMapper taskBatchMapper;
    private final TaskExecuteMapper taskExecuteMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ERRORS = 50;

    public List<TaskDTO> getTaskList(String taskId) {
        List<Map<String, Object>> rows = taskBatchMapper.selectTaskSummary(taskId);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            int totalBatch = toInt(row.get("totalBatch"));
            int completedBatch = toInt(row.get("completedBatch"));
            String status = completedBatch < totalBatch ? "running" : "success";
            return TaskDTO.builder()
                    .taskId((String) row.get("taskId"))
                    .completedBatch(completedBatch)
                    .totalBatch(totalBatch)
                    .status(status)
                    .startTime(formatTimestamp(row.get("startTime")))
                    .endTime(formatTimestamp(row.get("endTime")))
                    .build();
        }).collect(Collectors.toList());
    }

    public TaskMonitorDTO getTaskMonitor(String taskId) {
        List<TaskBatch> batches = taskBatchMapper.selectBatchesByTaskId(taskId);
        List<TaskExecute> executes = taskExecuteMapper.selectExecutesByTaskId(taskId);
        LocalDateTime dbNow = taskBatchMapper.selectDatabaseNow();

        TaskStatisticsDTO statistics = buildStatistics(batches, dbNow);
        List<ExecuteStatisticsDTO> executeStats = buildExecuteStatistics(executes);

        return TaskMonitorDTO.builder()
                .taskId(taskId)
                .statistics(statistics)
                .executes(executeStats)
                .build();
    }

    private TaskStatisticsDTO buildStatistics(List<TaskBatch> batches, LocalDateTime dbNow) {
        if (batches == null || batches.isEmpty()) {
            return TaskStatisticsDTO.builder()
                    .allCount(0).doneCount(0).runningCount(0).errorCount(0)
                    .status(2).startTime("").endTime("").nowTime(formatTimestamp(dbNow))
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

        return TaskStatisticsDTO.builder()
                .allCount(allCount)
                .doneCount(doneCount)
                .runningCount(runningCount)
                .errorCount(errorCount)
                .status(runningCount > 0 ? 1 : 2)
                .startTime(formatTimestamp(minStart))
                .endTime(formatTimestamp(maxEnd))
                .nowTime(formatTimestamp(dbNow))
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
                    .outPerSecond(safeDiv(outCount * 1000, duration))
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
