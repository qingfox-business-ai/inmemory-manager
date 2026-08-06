package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBatchDTO {

    private String batchId;
    private String clientId;
    private String status;
    private String startTime;
    private String endTime;
    private String error;
}
