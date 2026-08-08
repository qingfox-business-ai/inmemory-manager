package com.qingfox.inmemory.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "inmemory_execute_template", schema = "bill")
public class ExecuteTemplate {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String templateName;
    private String resolverId;
    private String subResolverId;
    private String statisticsExecuteId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String customData;
    private String inputData;
}
