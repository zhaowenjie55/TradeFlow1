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
            String normalizedRewrittenText = normalizeRewrittenText(result.rewrittenText(), sourceText);
            List<String> normalizedKeywords = normalizeKeywords(result.keywords(), normalizedRewrittenText, sourceText);
            QueryRewrite rewrite = queryRewriteRepository.save(QueryRewrite.builder()
                    .taskId(taskId)
                    .candidateId(candidateId)
                    .sourceProductId(sourceProductId)
                    .sourceText(sourceText)
                    .rewrittenText(normalizedRewrittenText)
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
                .rewrittenText(normalizeRewrittenText(rewrite.rewrittenText(), rewrite.sourceText()))
                .keywords(normalizeKeywords(rewrite.keywords(), rewrite.rewrittenText(), rewrite.sourceText()))
                .gatewaySource(rewrite.gatewaySource())
                .gatewayModel(rewrite.gatewayModel())
                .fallbackUsed(rewrite.fallbackUsed())
                .fallbackReason(rewrite.fallbackReason())
                .createdAt(rewrite.createdAt())
                .build();
    }

    private String normalizeRewrittenText(String rewrittenText, String sourceText) {
        if (rewrittenText == null || rewrittenText.isBlank()) {
            return deriveFallbackDomesticQuery(sourceText);
        }
        return rewrittenText.trim();
    }

    private List<String> normalizeKeywords(List<String> keywords, String rewrittenText, String sourceText) {
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
        if (deduplicated.isEmpty()) {
            deduplicated.add(deriveFallbackDomesticQuery(sourceText));
        }
        String fixedKeyword = vectorSearchProperties.getFixedKeyword();
        if (fixedKeyword != null && !fixedKeyword.isBlank()) {
            deduplicated.add(fixedKeyword.trim());
        }
        return new ArrayList<>(deduplicated);
    }

    private String deriveFallbackDomesticQuery(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return vectorSearchProperties.getFixedKeyword();
        }
        String normalized = sourceText.trim();
        if (containsHan(normalized)) {
            return normalized;
        }
        String compact = normalized
                .replaceAll("(?i)\\b(innovation award winner|award winner|replacement filters?|multiple pets?|grey|gray|plastic|stainless steel|wireless|portable|automatic)\\b", " ")
                .replaceAll("(?i)\\b\\d+(?:\\.\\d+)?(?:oz|l|ml|cm|mm|inch|inches)?\\b", " ")
                .replaceAll("[^a-zA-Z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compact.isBlank() ? vectorSearchProperties.getFixedKeyword() : compact;
    }

    private boolean containsHan(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    public record RewriteExecutionResult(
            QueryRewrite queryRewrite,
            boolean fallbackUsed
    ) {
    }
}
