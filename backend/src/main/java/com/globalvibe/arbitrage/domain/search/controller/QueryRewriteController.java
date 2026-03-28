package com.globalvibe.arbitrage.domain.search.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.search.dto.QueryRewriteListResponse;
import com.globalvibe.arbitrage.domain.search.dto.QueryRewriteVO;
import com.globalvibe.arbitrage.domain.search.service.QueryRewriteQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryRewriteController {

    private final QueryRewriteQueryService queryRewriteQueryService;

    public QueryRewriteController(QueryRewriteQueryService queryRewriteQueryService) {
        this.queryRewriteQueryService = queryRewriteQueryService;
    }

    @GetMapping("/task/{taskId}/rewrites")
    public ApiResponse<QueryRewriteListResponse> listByTaskId(@PathVariable String taskId) {
        return ApiResponse.success(queryRewriteQueryService.listByTaskId(taskId));
    }

    @GetMapping("/candidate/{candidateId}/rewrite")
    public ApiResponse<QueryRewriteVO> latestByCandidateId(@PathVariable String candidateId) {
        return ApiResponse.success(queryRewriteQueryService.getLatestByCandidateId(candidateId));
    }
}
