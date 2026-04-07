package com.globalvibe.arbitrage.domain.analysis.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisRunRequest;
import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisRunResponse;
import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisResultVO;
import com.globalvibe.arbitrage.domain.analysis.service.AnalysisService;
import com.globalvibe.arbitrage.domain.analysis.service.AnalysisQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisQueryService analysisQueryService;
    private final AnalysisService analysisService;

    public AnalysisController(
            AnalysisQueryService analysisQueryService,
            AnalysisService analysisService
    ) {
        this.analysisQueryService = analysisQueryService;
        this.analysisService = analysisService;
    }

    @PostMapping("/run")
    public ApiResponse<AnalysisRunResponse> run(@Valid @RequestBody AnalysisRunRequest request) {
        return ApiResponse.success(analysisService.runAnalysis(request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AnalysisResultVO> detail(@PathVariable String taskId) {
        return ApiResponse.success(analysisQueryService.getByTaskId(taskId));
    }
}
