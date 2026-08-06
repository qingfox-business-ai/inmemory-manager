package com.qingfox.inmemory.manager.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inmemory_task_execute", schema = "bill")
public class TaskExecute {

    @TableId
    private Long id;
    private String taskId;
    private String batchId;
    private String executeId;
    private String executeType;
    private Integer runCount;
    private Integer errorCount;
    private Integer inCount;
    private Integer outCount;
    private Integer duration;
    private String errorStack;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
