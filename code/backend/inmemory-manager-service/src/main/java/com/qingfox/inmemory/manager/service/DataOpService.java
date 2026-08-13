package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingfox.inmemory.manager.entity.DataOp;
import com.qingfox.inmemory.manager.mapper.DataOpMapper;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.model.dto.DataOpDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataOpService {

    private final DataOpMapper dataOpMapper;
    private final TaskBatchMapper taskBatchMapper;

    public List<DataOpDTO> list() {
        List<DataOp> ops = dataOpMapper.selectList(new LambdaQueryWrapper<DataOp>()
                .orderByDesc(DataOp::getStartTime));
        return ops.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void execute(String opType, String opParameter) {
        DataOp op = new DataOp();
        op.setOpType(opType);
        op.setOpParameter(opParameter);
        op.setStatus((short) 0);
        LocalDateTime dbNow = taskBatchMapper.selectDatabaseNow();
        op.setStartTime(dbNow);
        op.setUpdateTime(dbNow);
        dataOpMapper.insert(op);
    }

    private DataOpDTO toDTO(DataOp op) {
        return DataOpDTO.builder()
                .id(op.getId())
                .opType(op.getOpType())
                .opParameter(op.getOpParameter())
                .opResult(op.getOpResult())
                .status(op.getStatus() != null ? op.getStatus().intValue() : null)
                .startTime(formatTime(op.getStartTime()))
                .endTime(formatTime(op.getEndTime()))
                .errorStack(op.getErrorStack())
                .build();
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return null;
        return time.toString().replace('T', ' ').substring(0, 19);
    }
}
