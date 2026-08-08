package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private String clientId;
    private String address;
    private String status;
    private Long lastHeartbeat;
    private Long firstConnect;
    private Integer consumerIndex;
    private Boolean leader;
}
