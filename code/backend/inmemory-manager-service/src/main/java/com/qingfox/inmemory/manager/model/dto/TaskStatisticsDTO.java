package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsDTO {

    private int allCount;
    private int doneCount;
    private int runningCount;
    private int errorCount;
    private int status;
    private String startTime;
    private String endTime;
    private String nowTime;
    private long duration;
    private List<String> errorStack;
}
