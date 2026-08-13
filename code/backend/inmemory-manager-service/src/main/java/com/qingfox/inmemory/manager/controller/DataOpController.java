package com.qingfox.inmemory.manager.controller;

import com.qingfox.inmemory.manager.model.ApiResponse;
import com.qingfox.inmemory.manager.model.dto.DataOpDTO;
import com.qingfox.inmemory.manager.service.DataOpService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dataop")
@RequiredArgsConstructor
public class DataOpController {

    private final DataOpService dataOpService;

    @GetMapping("/list")
    public ApiResponse<List<DataOpDTO>> list() {
        return ApiResponse.success(dataOpService.list());
    }

    @PostMapping("/execute")
    public ApiResponse<Void> execute(@RequestBody Map<String, String> body) {
        dataOpService.execute(body.get("opType"), body.get("opParameter"));
        return ApiResponse.success(null);
    }
}
