package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.search.dto.QueryRewriteListResponse;
import com.globalvibe.arbitrage.domain.search.dto.QueryRewriteVO;
import com.globalvibe.arbitrage.domain.search.repository.QueryRewriteRepository;
import org.springframework.stereotype.Service;

@Service
public class QueryRewriteQueryService {

    private final QueryRewriteRepository queryRewriteRepository;
    private final QueryRewriteViewAssembler queryRewriteViewAssembler;

    public QueryRewriteQueryService(
            QueryRewriteRepository queryRewriteRepository,
            QueryRewriteViewAssembler queryRewriteViewAssembler
    ) {
        this.queryRewriteRepository = queryRewriteRepository;
        this.queryRewriteViewAssembler = queryRewriteViewAssembler;
    }

    public QueryRewriteListResponse listByTaskId(String taskId) {
        return new QueryRewriteListResponse(
                queryRewriteRepository.findByTaskId(taskId).stream()
                        .map(queryRewriteViewAssembler::toView)
                        .toList()
        );
    }

    public QueryRewriteVO getLatestByCandidateId(String candidateId) {
        return queryRewriteRepository.findLatestByCandidateId(candidateId)
                .or(() -> queryRewriteRepository.findLatestBySourceProductId(candidateId))
                .map(queryRewriteViewAssembler::toView)
                .orElseThrow(() -> new QueryRewriteNotFoundException(candidateId));
    }
}
