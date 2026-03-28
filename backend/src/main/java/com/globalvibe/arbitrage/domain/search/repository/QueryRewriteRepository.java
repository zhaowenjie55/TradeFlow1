package com.globalvibe.arbitrage.domain.search.repository;

import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;

import java.util.List;
import java.util.Optional;

public interface QueryRewriteRepository {

    QueryRewrite save(QueryRewrite queryRewrite);

    Optional<QueryRewrite> findLatestBySourceText(String sourceText);

    Optional<QueryRewrite> findLatestByCandidateId(String candidateId);

    Optional<QueryRewrite> findLatestBySourceProductId(String sourceProductId);

    List<QueryRewrite> findByTaskId(String taskId);
}
