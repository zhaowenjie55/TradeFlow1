package com.globalvibe.arbitrage.domain.match.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.match.dto.CandidateMatchListResponse;
import com.globalvibe.arbitrage.domain.match.service.CandidateMatchQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CandidateMatchController {

    private final CandidateMatchQueryService candidateMatchQueryService;

    public CandidateMatchController(CandidateMatchQueryService candidateMatchQueryService) {
        this.candidateMatchQueryService = candidateMatchQueryService;
    }

    @GetMapping("/candidate/{candidateId}/matches")
    public ApiResponse<CandidateMatchListResponse> listByCandidateId(
            @PathVariable String candidateId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(candidateMatchQueryService.listByCandidateId(candidateId, limit));
    }

    @GetMapping("/task/{taskId}/matches")
    public ApiResponse<CandidateMatchListResponse> listByTaskId(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "5") int limitPerCandidate
    ) {
        return ApiResponse.success(candidateMatchQueryService.listByTaskId(taskId, limitPerCandidate));
    }
}
