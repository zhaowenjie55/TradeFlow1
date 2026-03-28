package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.search.repository.QueryRewriteRepository;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class QueryRewriteService {

    private final LLMGateway llmGateway;
    private final QueryRewriteRepository queryRewriteRepository;

    public QueryRewriteService(LLMGateway llmGateway, QueryRewriteRepository queryRewriteRepository) {
        this.llmGateway = llmGateway;
        this.queryRewriteRepository = queryRewriteRepository;
    }

    public RewriteExecutionResult rewrite(String sourceText) {
        return rewrite(null, null, null, sourceText);
    }

    public RewriteExecutionResult rewrite(String taskId, String candidateId, String sourceProductId, String sourceText) {
        try {
            LLMGateway.RewriteResult result = llmGateway.rewriteTitle(sourceText);
            QueryRewrite rewrite = queryRewriteRepository.save(QueryRewrite.builder()
                    .taskId(taskId)
                    .candidateId(candidateId)
                    .sourceProductId(sourceProductId)
                    .sourceText(sourceText)
                    .rewrittenText(result.rewrittenText())
                    .keywords(result.keywords())
                    .gatewaySource(result.provider())
                    .fallbackUsed(result.fallbackUsed())
                    .fallbackReason(result.fallbackReason())
                    .createdAt(OffsetDateTime.now())
                    .build());
            return new RewriteExecutionResult(rewrite, result.fallbackUsed());
        } catch (RuntimeException ex) {
            Optional<QueryRewrite> fallback = candidateId == null
                    ? queryRewriteRepository.findLatestBySourceText(sourceText)
                    : queryRewriteRepository.findLatestByCandidateId(candidateId)
                            .or(() -> queryRewriteRepository.findLatestBySourceText(sourceText));
            QueryRewrite rewrite = fallback
                    .orElseThrow(() -> ex);
            return new RewriteExecutionResult(rewrite, true);
        }
    }

    public record RewriteExecutionResult(
            QueryRewrite queryRewrite,
            boolean fallbackUsed
    ) {
    }
}
