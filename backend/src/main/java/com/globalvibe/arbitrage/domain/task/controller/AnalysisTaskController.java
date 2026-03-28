package com.globalvibe.arbitrage.domain.task.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.candidate.dto.CandidateListResponse;
import com.globalvibe.arbitrage.domain.candidate.service.CandidateQueryService;
import com.globalvibe.arbitrage.domain.task.dto.CreateAnalysisTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.dto.SelectCandidateRequest;
import com.globalvibe.arbitrage.domain.task.dto.TaskHistoryResponse;
import com.globalvibe.arbitrage.domain.task.service.AnalysisTaskHistoryQueryService;
import com.globalvibe.arbitrage.domain.task.service.AnalysisTaskStatusQueryService;
import com.globalvibe.arbitrage.domain.task.service.Phase1TaskApplicationService;
import com.globalvibe.arbitrage.domain.task.service.Phase2TaskApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/analysis/tasks")
public class AnalysisTaskController {

    private final Phase1TaskApplicationService phase1TaskApplicationService;
    private final Phase2TaskApplicationService phase2TaskApplicationService;
    private final AnalysisTaskStatusQueryService analysisTaskStatusQueryService;
    private final AnalysisTaskHistoryQueryService analysisTaskHistoryQueryService;
    private final CandidateQueryService candidateQueryService;

    public AnalysisTaskController(
            Phase1TaskApplicationService phase1TaskApplicationService,
            Phase2TaskApplicationService phase2TaskApplicationService,
            AnalysisTaskStatusQueryService analysisTaskStatusQueryService,
            AnalysisTaskHistoryQueryService analysisTaskHistoryQueryService,
            CandidateQueryService candidateQueryService
    ) {
        this.phase1TaskApplicationService = phase1TaskApplicationService;
        this.phase2TaskApplicationService = phase2TaskApplicationService;
        this.analysisTaskStatusQueryService = analysisTaskStatusQueryService;
        this.analysisTaskHistoryQueryService = analysisTaskHistoryQueryService;
        this.candidateQueryService = candidateQueryService;
    }

    @PostMapping
    public ApiResponse<Phase1CreateTaskResponse> create(@Valid @RequestBody CreateAnalysisTaskRequest request) {
        BigDecimal targetProfitMargin = request.targetProfitMargin() != null
                ? request.targetProfitMargin()
                : new BigDecimal("0.25");
        Phase1CreateTaskRequest phase1Request = new Phase1CreateTaskRequest(
                request.keyword(),
                "AMAZON",
                request.constraints(),
                request.limit(),
                targetProfitMargin,
                request.mode()
        );
        return ApiResponse.success(phase1TaskApplicationService.createTask(phase1Request));
    }

    @GetMapping("/history")
    public ApiResponse<TaskHistoryResponse> history() {
        return ApiResponse.success(analysisTaskHistoryQueryService.getHistory());
    }

    @GetMapping("/{taskId}/status")
    public ApiResponse<Object> status(@PathVariable String taskId) {
        return ApiResponse.success(analysisTaskStatusQueryService.getTaskStatus(taskId));
    }

    @GetMapping("/{taskId}/candidates")
    public ApiResponse<CandidateListResponse> candidates(@PathVariable String taskId) {
        return ApiResponse.success(candidateQueryService.listByTaskId(taskId));
    }

    @PostMapping("/{taskId}/selection")
    public ApiResponse<Phase2CreateTaskResponse> selectCandidate(
            @PathVariable String taskId,
            @Valid @RequestBody SelectCandidateRequest request
    ) {
        return ApiResponse.success(phase2TaskApplicationService.createTask(
                new Phase2CreateTaskRequest(taskId, request.productId())
        ));
    }
}
