package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListDTO {

    private String taskId;
    private String taskMark;
    private Integer status;
    private String errorStack;
    private Long inputCount;
    private Long outputCount;
    private String startTime;
    private String endTime;
    private Long batchWait;
    private Long batchRun;
    private Long batchDone;
    private Long batchFailure;
    private Long queueWait;
    private Long queueRun;
    private Long queueDone;
    private Long queueFailure;
}
