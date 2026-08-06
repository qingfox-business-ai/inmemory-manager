package com.qingfox.inmemory.manager.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inmemory_task_batch", schema = "bill")
public class TaskBatch {

    @TableId
    private Long id;
    private String taskId;
    private String batchId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String taskType;
    private Short status;
    private String errorStack;
    private LocalDateTime updateTime;
    private String resolverId;
    private String subResolverId;
}
