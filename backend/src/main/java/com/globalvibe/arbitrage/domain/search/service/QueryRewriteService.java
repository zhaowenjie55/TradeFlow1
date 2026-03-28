package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.search.repository.QueryRewriteRepository;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class QueryRewriteService {

    private final LLMGateway llmGateway;
    private final QueryRewriteRepository queryRewriteRepository;
    private final VectorSearchProperties vectorSearchProperties;

    public QueryRewriteService(
            LLMGateway llmGateway,
            QueryRewriteRepository queryRewriteRepository,
            VectorSearchProperties vectorSearchProperties
    ) {
        this.llmGateway = llmGateway;
        this.queryRewriteRepository = queryRewriteRepository;
        this.vectorSearchProperties = vectorSearchProperties;
    }

    public RewriteExecutionResult rewrite(String sourceText) {
        return rewrite(null, null, null, sourceText, TaskMode.REAL);
    }

    public RewriteExecutionResult rewrite(String taskId, String candidateId, String sourceProductId, String sourceText) {
        return rewrite(taskId, candidateId, sourceProductId, sourceText, TaskMode.REAL);
    }

    public RewriteExecutionResult rewrite(String taskId, String candidateId, String sourceProductId, String sourceText, TaskMode mode) {
        try {
            LLMGateway.RewriteResult result = llmGateway.rewriteTitle(sourceText);
            List<String> normalizedKeywords = normalizeKeywords(result.keywords(), result.rewrittenText());
            QueryRewrite rewrite = queryRewriteRepository.save(QueryRewrite.builder()
                    .taskId(taskId)
                    .candidateId(candidateId)
                    .sourceProductId(sourceProductId)
                    .sourceText(sourceText)
                    .rewrittenText(normalizeRewrittenText(result.rewrittenText()))
                    .keywords(normalizedKeywords)
                    .gatewaySource(result.provider())
                    .gatewayModel(result.model())
                    .fallbackUsed(result.fallbackUsed())
                    .fallbackReason(result.fallbackReason())
                    .createdAt(OffsetDateTime.now())
                    .build());
            return new RewriteExecutionResult(rewrite, result.fallbackUsed());
        } catch (RuntimeException ex) {
            if (mode == TaskMode.REAL) {
                throw new IllegalStateException("真实 GLM 改写失败: " + ex.getMessage(), ex);
            }
            Optional<QueryRewrite> fallback = candidateId == null
                    ? queryRewriteRepository.findLatestBySourceText(sourceText)
                    : queryRewriteRepository.findLatestByCandidateId(candidateId)
                            .or(() -> queryRewriteRepository.findLatestBySourceText(sourceText));
            QueryRewrite rewrite = fallback
                    .map(this::normalizeExistingRewrite)
                    .orElseThrow(() -> ex);
            return new RewriteExecutionResult(rewrite, true);
        }
    }

    private QueryRewrite normalizeExistingRewrite(QueryRewrite rewrite) {
        return QueryRewrite.builder()
                .rewriteId(rewrite.rewriteId())
                .taskId(rewrite.taskId())
                .candidateId(rewrite.candidateId())
                .sourceProductId(rewrite.sourceProductId())
                .sourceText(rewrite.sourceText())
                .rewrittenText(normalizeRewrittenText(rewrite.rewrittenText()))
                .keywords(normalizeKeywords(rewrite.keywords(), rewrite.rewrittenText()))
                .gatewaySource(rewrite.gatewaySource())
                .gatewayModel(rewrite.gatewayModel())
                .fallbackUsed(rewrite.fallbackUsed())
                .fallbackReason(rewrite.fallbackReason())
                .createdAt(rewrite.createdAt())
                .build();
    }

    private String normalizeRewrittenText(String rewrittenText) {
        if (rewrittenText == null || rewrittenText.isBlank()) {
            return vectorSearchProperties.getFixedKeyword();
        }
        return rewrittenText.trim();
    }

    private List<String> normalizeKeywords(List<String> keywords, String rewrittenText) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        if (rewrittenText != null && !rewrittenText.isBlank()) {
            deduplicated.add(rewrittenText.trim());
        }
        if (keywords != null) {
            keywords.stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .map(String::trim)
                    .forEach(deduplicated::add);
        }
        deduplicated.add(vectorSearchProperties.getFixedKeyword());
        return new ArrayList<>(deduplicated);
    }

    public record RewriteExecutionResult(
            QueryRewrite queryRewrite,
            boolean fallbackUsed
    ) {
    }
}
