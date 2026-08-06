package com.qingfox.inmemory.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingfox.inmemory.manager.entity.TaskExecute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskExecuteMapper extends BaseMapper<TaskExecute> {

    List<TaskExecute> selectExecutesByTaskId(@Param("taskId") String taskId);
}
