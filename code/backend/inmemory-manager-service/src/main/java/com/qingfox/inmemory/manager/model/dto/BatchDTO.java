package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDTO {

    private String batchId;
    private Integer status;
    private String updateTime;
    private String errorStack;
    private String startTime;
    private String endTime;
}
