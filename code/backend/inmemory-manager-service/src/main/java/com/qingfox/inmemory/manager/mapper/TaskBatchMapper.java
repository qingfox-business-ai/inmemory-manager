package com.qingfox.inmemory.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingfox.inmemory.manager.entity.TaskBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskBatchMapper extends BaseMapper<TaskBatch> {

    List<Map<String, Object>> selectTaskSummary(@Param("taskId") String taskId,
                                                @Param("startTime") String startTime,
                                                @Param("endTime") String endTime);

    List<TaskBatch> selectBatchesByTaskId(@Param("taskId") String taskId);

    LocalDateTime selectDatabaseNow();

    List<Map<String, Object>> selectBatchStatusCountByTaskId(@Param("taskId") String taskId);
}
