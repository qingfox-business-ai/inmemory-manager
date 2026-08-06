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
public class ExecuteStatisticsDTO {

    private String executeId;
    private String executeType;
    private int runCount;
    private int errorCount;
    private int inCount;
    private int outCount;
    private int duration;
    private int avgIn;
    private int avgOut;
    private int outPerSecond;
    private String createTime;
    private String updateTime;
    private List<String> errorStack;
}
