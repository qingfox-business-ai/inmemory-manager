package com.qingfox.inmemory.manager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {

    private ClientSummary client;
    private QueueSummary queue;
    private TaskSummary task;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSummary {
        private int connected;
        private int disconnected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueSummary {
        private int completed;
        private int pending;
        private int deadLetter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSummary {
        private int success;
        private int waiting;
        private int running;
        private int failed;
        private String periodStart;
        private String periodEnd;
    }
}
