package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateDTO;
import com.qingfox.inmemory.manager.model.dto.ExecuteTemplateSaveDTO;
import com.qingfox.inmemory.manager.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping("/list")
    public ApiResponse<List<ExecuteTemplateDTO>> list() {
        return ApiResponse.success(templateService.list());
    }

    @GetMapping("/{id}/customData")
    public ApiResponse<String> customData(@PathVariable Integer id) {
        return ApiResponse.success(templateService.getCustomData(id));
    }

    @GetMapping("/{id}/inputData")
    public ApiResponse<String> inputData(@PathVariable Integer id) {
        return ApiResponse.success(templateService.getInputData(id));
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody ExecuteTemplateSaveDTO dto) {
        templateService.add(dto);
        return ApiResponse.success(null);
    }

    @PutMapping
    public ApiResponse<Void> update(@RequestBody ExecuteTemplateSaveDTO dto) {
        templateService.update(dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        templateService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<Map<String, Object>> execute(@PathVariable Integer id) {
        return templateService.execute(id);
    }
}
