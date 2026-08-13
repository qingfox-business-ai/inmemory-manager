package com.qingfox.inmemory.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inmemory_data_op", schema = "bill")
public class DataOp {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String opType;
    private String opParameter;
    private String opResult;
    private Short status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime updateTime;
    private String errorStack;
}
