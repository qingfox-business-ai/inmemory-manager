package com.qingfox.inmemory.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingfox.inmemory.manager.entity.ExecuteTemplate;
import com.qingfox.inmemory.manager.entity.Task;
import com.qingfox.inmemory.manager.mapper.ExecuteTemplateMapper;
import com.qingfox.inmemory.manager.mapper.TaskBatchMapper;
import com.qingfox.inmemory.manager.mapper.TaskMapper;
import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateDTO;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateSaveDTO;
import com.qingfox.inmemory.manager.model.dto.TemplateExecuteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ExecuteTemplateMapper executeTemplateMapper;
    private final TaskMapper taskMapper;
    private final TaskBatchMapper taskBatchMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inmemory.execute.service:}")
    private String executeServiceUrl;

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
        template.setInputId(dto.getInputId());
        template.setOutputId(dto.getOutputId());
        template.setCustomData(dto.getCustomData());
        template.setInputData(dto.getInputData());
        LocalDateTime now = taskBatchMapper.selectDatabaseNow();
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
        template.setInputId(dto.getInputId());
        template.setOutputId(dto.getOutputId());
        template.setCustomData(dto.getCustomData());
        template.setInputData(dto.getInputData());
        template.setUpdateTime(taskBatchMapper.selectDatabaseNow());
        executeTemplateMapper.updateById(template);
    }

    public void delete(Integer id) {
        executeTemplateMapper.deleteById(id);
    }

    public ApiResponse<Map<String, Object>> execute(Integer id) {
        ExecuteTemplate template = executeTemplateMapper.selectById(id);
        if (template == null) {
            return ApiResponse.error(404, "模板不存在");
        }
        return doExecute(template.getTemplateName(), template.getResolverId(),
                template.getSubResolverId(), template.getInputId(), template.getOutputId(),
                template.getCustomData(), template.getInputData());
    }

    public ApiResponse<Map<String, Object>> executeWithParams(TemplateExecuteDTO dto) {
        return doExecute(dto.getTemplateName(), dto.getResolverId(), dto.getSubResolverId(),
                dto.getInputId(), dto.getOutputId(), dto.getCustomData(), dto.getInputData());
    }

    @SuppressWarnings("unchecked")
    private ApiResponse<Map<String, Object>> doExecute(String templateName, String resolverId,
            String subResolverId, String inputId, String outputId, String customData, String inputData) {
        String taskId = "-";
        String errorMsg = null;
        boolean success = false;

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("resolverId", resolverId);
            List<String> subResolverIdList = (subResolverId != null && !subResolverId.trim().isEmpty())
                    ? Arrays.asList(subResolverId.split(","))
                    : Collections.emptyList();
            body.put("subResolverId", subResolverIdList);
            body.put("subResolverIdList", subResolverIdList);
            body.put("async", true);
            body.put("inputDataListMap", parseJsonObject(inputData));
            body.put("customData", parseJsonObject(customData));

            String url = executeServiceUrl + "/api/inmemory/executeResolver";
            Map<String, Object> resp = restTemplate.postForObject(url, body, Map.class);

            Object code = resp != null ? resp.get("code") : null;
            success = "0".equals(String.valueOf(code));
            if (success) {
                Object result = resp.get("result");
                taskId = (result instanceof Map) ? String.valueOf(((Map<?, ?>) result).get("taskId")) : "-";
            } else {
                Object message = resp != null ? resp.get("message") : null;
                errorMsg = message != null ? String.valueOf(message) : "执行失败";
            }
        } catch (Exception e) {
            errorMsg = getStackTrace(e);
        }

        Task task = new Task();
        task.setTaskId(taskId);
        task.setInputId(inputId);
        task.setOutputId(outputId);
        task.setTaskMark(templateName);
        task.setStatus(success ? (short) 0 : (short) 3);
        task.setErrorStack(!success ? errorMsg : null);
        LocalDateTime dbNow = taskBatchMapper.selectDatabaseNow();
        task.setStartTime(dbNow);
        task.setEndTime(dbNow);
        task.setUpdateTime(dbNow);
        taskMapper.insert(task);

        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            return ApiResponse.success(data);
        }
        return ApiResponse.error(500, errorMsg);
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
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
                .inputId(template.getInputId())
                .outputId(template.getOutputId())
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
