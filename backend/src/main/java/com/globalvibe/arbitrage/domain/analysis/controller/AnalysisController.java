package com.globalvibe.arbitrage.domain.analysis.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisResultVO;
import com.globalvibe.arbitrage.domain.analysis.service.AnalysisQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisQueryService analysisQueryService;

    public AnalysisController(AnalysisQueryService analysisQueryService) {
        this.analysisQueryService = analysisQueryService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AnalysisResultVO> detail(@PathVariable String taskId) {
        return ApiResponse.success(analysisQueryService.getByTaskId(taskId));
    }
}
