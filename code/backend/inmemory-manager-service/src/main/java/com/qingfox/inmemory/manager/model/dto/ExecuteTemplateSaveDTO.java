package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTemplateSaveDTO {

    private Integer id;
    private String templateName;
    private String resolverId;
    private String subResolverId;
    private String inputId;
    private String outputId;
    private String customData;
    private String inputData;
}
