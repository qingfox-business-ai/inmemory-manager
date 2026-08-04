package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamMessageDTO {

    private String messageId;
    private String taskId;
    private String batchId;
    private String resolverId;
    private String status;
    private String consumerId;
}
