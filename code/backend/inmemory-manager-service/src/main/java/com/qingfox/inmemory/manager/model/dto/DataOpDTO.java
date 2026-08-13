package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataOpDTO {

    private Long id;
    private String opType;
    private String opParameter;
    private String opResult;
    private Integer status;
    private String startTime;
    private String endTime;
    private String errorStack;
}
