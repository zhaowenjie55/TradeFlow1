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
import java.util.Locale;
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
            if (mode == TaskMode.REAL && result.fallbackUsed()) {
                throw new IllegalStateException("真实 GLM 改写失败: " + normalizeFallbackReason(result.fallbackReason()));
            }
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
        String anchoredCoffeeQuery = deriveAnchoredCoffeeQuery(sourceText);
        if (rewrittenText == null || rewrittenText.isBlank()) {
            return anchoredCoffeeQuery != null ? anchoredCoffeeQuery : deriveFallbackDomesticQuery(sourceText);
        }
        String normalized = rewrittenText.trim();
        if (shouldPreferAnchoredQuery(normalized, anchoredCoffeeQuery, sourceText)) {
            return anchoredCoffeeQuery;
        }
        if (containsHan(normalized)) {
            return normalized;
        }
        String domesticFallback = anchoredCoffeeQuery != null ? anchoredCoffeeQuery : deriveFallbackDomesticQuery(sourceText);
        if (containsHan(domesticFallback)) {
            return domesticFallback;
        }
        return normalized;
    }

    private List<String> normalizeKeywords(List<String> keywords, String rewrittenText, String sourceText) {
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        boolean preferDomesticKeywords = containsHan(rewrittenText);
        if (rewrittenText != null && !rewrittenText.isBlank()) {
            deduplicated.add(rewrittenText.trim());
        }
        deriveAnchoredKeywords(sourceText).forEach(deduplicated::add);
        if (keywords != null) {
            keywords.stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .map(String::trim)
                    .filter(keyword -> !preferDomesticKeywords || containsHan(keyword))
                    .filter(keyword -> !isWeakAsciiKeyword(keyword))
                    .forEach(deduplicated::add);
        }
        String domesticFallback = deriveFallbackDomesticQuery(sourceText);
        if (domesticFallback != null && !domesticFallback.isBlank()) {
            if (deduplicated.isEmpty() || containsHan(domesticFallback)) {
                deduplicated.add(domesticFallback);
            }
        }
        if (deduplicated.isEmpty()) {
            String fallbackKeyword = domesticFallback == null || domesticFallback.isBlank()
                    ? vectorSearchProperties.getFixedKeyword()
                    : domesticFallback;
            if (fallbackKeyword != null && !fallbackKeyword.isBlank()) {
                deduplicated.add(fallbackKeyword);
            }
        }
        String fixedKeyword = vectorSearchProperties.getFixedKeyword();
        if (fixedKeyword != null && !fixedKeyword.isBlank()) {
            deduplicated.add(fixedKeyword.trim());
        }
        return new ArrayList<>(deduplicated);
    }

    private String deriveFallbackDomesticQuery(String sourceText) {
        String anchoredCoffeeQuery = deriveAnchoredCoffeeQuery(sourceText);
        if (anchoredCoffeeQuery != null) {
            return anchoredCoffeeQuery;
        }
        if (sourceText == null || sourceText.isBlank()) {
            return vectorSearchProperties.getFixedKeyword();
        }
        String normalized = sourceText.trim();
        if (containsHan(normalized)) {
            return normalized;
        }
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("coffee")) {
            if (lowerCase.contains("ground")) {
                return lowerCase.contains("hawaiian") ? "夏威夷咖啡粉" : "研磨咖啡粉";
            }
            if (lowerCase.contains("bean")) {
                return lowerCase.contains("hawaiian") ? "夏威夷咖啡豆" : "咖啡豆";
            }
            if (lowerCase.contains("instant")) {
                return "速溶咖啡";
            }
            return lowerCase.contains("hawaiian") ? "夏威夷咖啡" : "咖啡";
        }
        String compact = normalized
                .replaceAll("(?i)\\b(innovation award winner|award winner|replacement filters?|multiple pets?|grey|gray|plastic|stainless steel|wireless|portable|automatic)\\b", " ")
                .replaceAll("(?i)\\b\\d+(?:\\.\\d+)?(?:oz|l|ml|cm|mm|inch|inches)?\\b", " ")
                .replaceAll("[^a-zA-Z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return compact.isBlank() ? vectorSearchProperties.getFixedKeyword() : compact;
    }

    private boolean shouldPreferAnchoredQuery(String rewrittenText, String anchoredQuery, String sourceText) {
        if (anchoredQuery == null || anchoredQuery.isBlank()) {
            return false;
        }
        if (rewrittenText == null || rewrittenText.isBlank()) {
            return true;
        }
        return anchoredQuerySpecificityScore(anchoredQuery, sourceText)
                > anchoredQuerySpecificityScore(rewrittenText, sourceText);
    }

    private int anchoredQuerySpecificityScore(String query, String sourceText) {
        if (query == null || query.isBlank() || sourceText == null || sourceText.isBlank()) {
            return 0;
        }
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        String normalizedSource = sourceText.toLowerCase(Locale.ROOT);
        int score = 0;
        if (normalizedSource.contains("coffee")) {
            score += 1;
        }
        if (normalizedSource.contains("ground") && normalizedQuery.contains("咖啡粉")) {
            score += 2;
        }
        if (normalizedSource.contains("bean") && normalizedQuery.contains("咖啡豆")) {
            score += 2;
        }
        if (normalizedSource.contains("instant") && normalizedQuery.contains("速溶")) {
            score += 2;
        }
        if (normalizedSource.contains("vanilla") && normalizedQuery.contains("香草")) {
            score += 2;
        }
        if ((normalizedSource.contains("macadamia") || normalizedSource.contains("macadamia nut"))
                && normalizedQuery.contains("夏威夷果")) {
            score += 2;
        }
        if (normalizedSource.contains("hawaiian") && normalizedQuery.contains("夏威夷")) {
            score += 1;
        }
        if (normalizedQuery.contains("风味")) {
            score += 1;
        }
        return score;
    }

    private List<String> deriveAnchoredKeywords(String sourceText) {
        String anchoredCoffeeQuery = deriveAnchoredCoffeeQuery(sourceText);
        if (anchoredCoffeeQuery == null || anchoredCoffeeQuery.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.add(anchoredCoffeeQuery);
        String normalizedSource = sourceText == null ? "" : sourceText.toLowerCase(Locale.ROOT);
        if (normalizedSource.contains("macadamia")) {
            keywords.add(anchoredCoffeeQuery.replace("香草夏威夷果风味", "夏威夷果风味"));
            keywords.add(anchoredCoffeeQuery.replace("香草夏威夷果风味", "夏威夷果"));
        }
        if (normalizedSource.contains("vanilla")) {
            keywords.add(anchoredCoffeeQuery.replace("香草夏威夷果风味", "香草风味"));
        }
        if (normalizedSource.contains("ground")) {
            keywords.add("咖啡粉");
        } else if (normalizedSource.contains("bean")) {
            keywords.add("咖啡豆");
        }
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toList();
    }

    private String deriveAnchoredCoffeeQuery(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            return null;
        }
        String normalized = sourceText.toLowerCase(Locale.ROOT);
        if (!normalized.contains("coffee")) {
            return null;
        }
        List<String> descriptors = new ArrayList<>();
        if (normalized.contains("vanilla")) {
            descriptors.add("香草");
        }
        if (normalized.contains("macadamia")) {
            descriptors.add("夏威夷果");
        } else if (normalized.contains("hawaiian")) {
            descriptors.add("夏威夷");
        }
        String baseForm;
        if (normalized.contains("ground")) {
            baseForm = "咖啡粉";
        } else if (normalized.contains("bean")) {
            baseForm = "咖啡豆";
        } else if (normalized.contains("instant")) {
            baseForm = "速溶咖啡";
        } else {
            baseForm = "咖啡";
        }
        if (descriptors.isEmpty()) {
            return null;
        }
        String prefix = String.join("", descriptors);
        if (!prefix.endsWith("风味") && (normalized.contains("vanilla") || normalized.contains("macadamia"))) {
            prefix = prefix + "风味";
        }
        return prefix + baseForm;
    }

    private boolean isWeakAsciiKeyword(String keyword) {
        if (keyword == null || keyword.isBlank() || containsHan(keyword)) {
            return false;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT).trim();
        String[] parts = normalized.split("\\s+");
        return parts.length == 1 && parts[0].length() <= 10;
    }

    private boolean containsHan(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String normalizeFallbackReason(String fallbackReason) {
        return fallbackReason == null || fallbackReason.isBlank() ? "LLM 返回了模拟改写结果。" : fallbackReason;
    }

    public record RewriteExecutionResult(
            QueryRewrite queryRewrite,
            boolean fallbackUsed
    ) {
    }
}
