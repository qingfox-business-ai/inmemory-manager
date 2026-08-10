package com.qingfox.inmemory.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inmemory_task", schema = "bill")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private Short status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime updateTime;
    private Long inputCount;
    private String outputCount;
    private String inputId;
    private String outputId;
    private String taskMark;
    private Long batchWait;
    private Long batchRun;
    private Long batchDone;
    private Long batchFailure;
    private Long queueWait;
    private Long queueRun;
    private Long queueDone;
    private Long queueFailure;
}
