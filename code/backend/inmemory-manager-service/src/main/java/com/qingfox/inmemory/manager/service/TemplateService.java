package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.entity.ExecuteTemplate;
import com.qingfox.inmemory.manager.mapper.ExecuteTemplateMapper;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateDTO;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ExecuteTemplateMapper executeTemplateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ExecuteTemplateDTO> list() {
        List<ExecuteTemplate> templates = executeTemplateMapper.selectList(
                new LambdaQueryWrapper<ExecuteTemplate>()
                        .orderByDesc(ExecuteTemplate::getUpdateTime));
        return templates.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public String getCustomData(Integer id) {
        ExecuteTemplate template = executeTemplateMapper.selectById(id);
        return template != null ? template.getCustomData() : null;
    }

    public String getInputData(Integer id) {
        ExecuteTemplate template = executeTemplateMapper.selectById(id);
        return template != null ? template.getInputData() : null;
    }

    public void add(ExecuteTemplateSaveDTO dto) {
        validateJson(dto.getCustomData(), "定制数据");
        validateJson(dto.getInputData(), "输入数据");
        ExecuteTemplate template = new ExecuteTemplate();
        template.setTemplateName(dto.getTemplateName());
        template.setResolverId(dto.getResolverId());
        template.setSubResolverId(dto.getSubResolverId());
        template.setStatisticsExecuteId(dto.getStatisticsExecuteId());
        template.setCustomData(dto.getCustomData());
        template.setInputData(dto.getInputData());
        LocalDateTime now = LocalDateTime.now();
        template.setCreateTime(now);
        template.setUpdateTime(now);
        executeTemplateMapper.insert(template);
    }

    public void update(ExecuteTemplateSaveDTO dto) {
        validateJson(dto.getCustomData(), "定制数据");
        validateJson(dto.getInputData(), "输入数据");
        ExecuteTemplate template = new ExecuteTemplate();
        template.setId(dto.getId());
        template.setTemplateName(dto.getTemplateName());
        template.setResolverId(dto.getResolverId());
        template.setSubResolverId(dto.getSubResolverId());
        template.setStatisticsExecuteId(dto.getStatisticsExecuteId());
        template.setCustomData(dto.getCustomData());
        template.setInputData(dto.getInputData());
        template.setUpdateTime(LocalDateTime.now());
        executeTemplateMapper.updateById(template);
    }

    public void delete(Integer id) {
        executeTemplateMapper.deleteById(id);
    }

    private void validateJson(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            objectMapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(field + "不是合法的JSON格式");
        }
    }

    private ExecuteTemplateDTO toDTO(ExecuteTemplate template) {
        return ExecuteTemplateDTO.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .resolverId(template.getResolverId())
                .subResolverId(template.getSubResolverId())
                .statisticsExecuteId(template.getStatisticsExecuteId())
                .createTime(formatTime(template.getCreateTime()))
                .updateTime(formatTime(template.getUpdateTime()))
                .build();
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.toString().replace('T', ' ').substring(0, 19);
    }
}
