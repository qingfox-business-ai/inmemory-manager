package com.qingfox.inmemory.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingfox.inmemory.manager.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
