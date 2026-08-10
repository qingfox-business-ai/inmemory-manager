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
    private Long inputCount;
    private String outputCount;
    private String startTime;
    private String endTime;
}
