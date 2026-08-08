package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTemplateDTO {

    private Integer id;
    private String templateName;
    private String resolverId;
    private String subResolverId;
    private String statisticsExecuteId;
    private String createTime;
    private String updateTime;
}
