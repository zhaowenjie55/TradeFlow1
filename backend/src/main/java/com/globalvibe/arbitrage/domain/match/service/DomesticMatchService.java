package com.globalvibe.arbitrage.domain.match.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.repository.CandidateMatchRepository;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.product.service.DomesticVectorSearchService;
import com.globalvibe.arbitrage.domain.product.service.DomesticVectorSearchService.VectorSearchResult;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DomesticMatchService {

    private final CandidateMatchRepository candidateMatchRepository;
    private final DomesticVectorSearchService domesticVectorSearchService;
    private final ProductRepository productRepository;
    private final VectorSearchProperties vectorSearchProperties;

    public DomesticMatchService(
            CandidateMatchRepository candidateMatchRepository,
            ObjectProvider<DomesticVectorSearchService> domesticVectorSearchServiceProvider,
            ProductRepository productRepository,
            VectorSearchProperties vectorSearchProperties
    ) {
        this.candidateMatchRepository = candidateMatchRepository;
        this.domesticVectorSearchService = domesticVectorSearchServiceProvider.getIfAvailable();
        this.productRepository = productRepository;
        this.vectorSearchProperties = vectorSearchProperties;
    }

    public MatchExecutionResult match(
            String taskId,
            String candidateId,
            CandidateProduct sourceCandidate,
            QueryRewrite queryRewrite,
            int limit
    ) {
        DomesticSearchExecution searchExecution = searchDomesticProducts(sourceCandidate, queryRewrite, limit);
        List<CandidateMatchRecord> matches = searchExecution.scoredCandidates().stream()
                .sorted(Comparator
                        .comparing(ScoredCandidate::finalScore, Comparator.reverseOrder())
                        .thenComparing(item -> item.product().price(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(item -> toMatch(taskId, candidateId, sourceCandidate, queryRewrite, item, searchExecution.retrievalTerms()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalStateException("商品库中未找到可用的 1688 匹配结果。");
        }

        candidateMatchRepository.replaceForCandidate(candidateId, matches);
        return new MatchExecutionResult(matches, false);
    }

    private DomesticSearchExecution searchDomesticProducts(CandidateProduct sourceCandidate, QueryRewrite queryRewrite, int limit) {
        int searchLimit = Math.max(limit * 4, 12);
        Map<String, MatchCandidateAccumulator> merged = new LinkedHashMap<>();
        List<String> retrievalTerms = buildSearchTerms(sourceCandidate, queryRewrite);

        for (String term : retrievalTerms) {
            if (domesticVectorSearchService != null) {
                List<VectorSearchResult> vectorProducts = domesticVectorSearchService.searchWithScores(term, searchLimit);
                vectorProducts.forEach(hit -> mergeCandidate(merged, hit.product(), term, hit.score(), true, false));
            }

            List<Product> catalogProducts = productRepository.searchByPlatformAndKeywordIncludingDetails(
                    MarketplaceType.ALIBABA_1688,
                    term,
                    searchLimit
            );
            catalogProducts.forEach(product -> mergeCandidate(merged, product, term, null, false, true));
        }

        return new DomesticSearchExecution(
                scoreCandidates(sourceCandidate, queryRewrite, merged.values()),
                retrievalTerms
        );
    }

    private void mergeCandidate(
            Map<String, MatchCandidateAccumulator> merged,
            Product product,
            String term,
            Double vectorScore,
            boolean vectorHit,
            boolean catalogHit
    ) {
        MatchCandidateAccumulator accumulator = merged.computeIfAbsent(
                product.id(),
                ignored -> new MatchCandidateAccumulator(product)
        );
        accumulator.product = product;
        accumulator.retrievalTerms.add(term);
        if (accumulator.primarySearchTerm == null) {
            accumulator.primarySearchTerm = term;
        }
        if (vectorHit) {
            accumulator.vectorHit = true;
            accumulator.vectorScore = accumulator.vectorScore == null
                    ? vectorScore
                    : Math.max(accumulator.vectorScore, vectorScore == null ? 0D : vectorScore);
        }
        accumulator.catalogHit = accumulator.catalogHit || catalogHit;
    }

    private List<String> buildSearchTerms(CandidateProduct sourceCandidate, QueryRewrite queryRewrite) {
        LinkedHashSet<String> searchTerms = new LinkedHashSet<>();
        addIfPresent(searchTerms, queryRewrite.rewrittenText());
        if (queryRewrite.keywords() != null) {
            queryRewrite.keywords().forEach(keyword -> addIfPresent(searchTerms, keyword));
        }
        addIfPresent(searchTerms, vectorSearchProperties.getFixedKeyword());

        extractAttributeTerms(sourceCandidate.title()).stream()
                .limit(4)
                .forEach(term -> addIfPresent(searchTerms, term));
        return new ArrayList<>(searchTerms);
    }

    private void addIfPresent(LinkedHashSet<String> terms, String value) {
        if (value != null && !value.isBlank()) {
            terms.add(value.trim());
        }
    }

    private List<String> extractAttributeTerms(String title) {
        if (title == null || title.isBlank()) {
            return List.of();
        }
        return tokens(title).stream()
                .filter(token -> token.length() >= 2 || token.matches(".*\\d.*"))
                .distinct()
                .toList();
    }

    private List<ScoredCandidate> scoreCandidates(
            CandidateProduct sourceCandidate,
            QueryRewrite queryRewrite,
            Collection<MatchCandidateAccumulator> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        BigDecimal minPrice = candidates.stream()
                .map(item -> item.product.price())
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = candidates.stream()
                .map(item -> item.product.price())
                .filter(price -> price != null)
                .max(BigDecimal::compareTo)
                .orElse(minPrice);

        return candidates.stream()
                .map(candidate -> {
                    ProductDetailSnapshot detailSnapshot = productRepository.findDetailByProductId(candidate.product.id()).orElse(null);
                    BigDecimal titleOverlap = ratioScore(tokens(sourceCandidate.title()), tokens(candidate.product.title()), 40);
                    BigDecimal rewriteCoverage = scoreRewriteCoverage(queryRewrite, candidate.product, detailSnapshot);
                    BigDecimal priceReasonability = scorePriceReasonability(candidate.product.price(), minPrice, maxPrice);
                    BigDecimal vectorBoost = scoreVectorBoost(candidate.vectorScore);
                    BigDecimal attributeAlignment = scoreAttributeAlignment(sourceCandidate.title(), candidate.product, detailSnapshot);
                    Map<String, BigDecimal> scoreBreakdown = buildScoreBreakdown(
                            titleOverlap,
                            rewriteCoverage,
                            priceReasonability,
                            vectorBoost,
                            attributeAlignment
                    );
                    BigDecimal finalScore = scoreBreakdown.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ScoredCandidate(
                            candidate.product,
                            candidate.primarySearchTerm,
                            candidate.retrievalTerms.stream().toList(),
                            candidate.vectorHit,
                            candidate.catalogHit,
                            titleOverlap,
                            rewriteCoverage,
                            priceReasonability,
                            vectorBoost,
                            attributeAlignment,
                            scoreBreakdown,
                            finalScore,
                            buildEvidence(queryRewrite, candidate.product, detailSnapshot, candidate.retrievalTerms, scoreBreakdown)
                    );
                })
                .toList();
    }

    private BigDecimal scoreRewriteCoverage(QueryRewrite queryRewrite, Product product, ProductDetailSnapshot detailSnapshot) {
        List<String> rewriteTokens = new ArrayList<>();
        rewriteTokens.addAll(tokens(queryRewrite.rewrittenText()));
        if (queryRewrite.keywords() != null) {
            queryRewrite.keywords().forEach(keyword -> rewriteTokens.addAll(tokens(keyword)));
        }
        List<String> targetTokens = new ArrayList<>(tokens(product.title()));
        if (detailSnapshot != null) {
            targetTokens.addAll(tokens(detailSnapshot.title()));
            targetTokens.addAll(tokens(detailSnapshot.brand()));
            targetTokens.addAll(tokens(detailSnapshot.description()));
        }
        return ratioScore(rewriteTokens, targetTokens, 20);
    }

    private BigDecimal scorePriceReasonability(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (maxPrice.compareTo(minPrice) <= 0) {
            return new BigDecimal("15.00");
        }
        BigDecimal normalized = maxPrice.subtract(price)
                .divide(maxPrice.subtract(minPrice), 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return normalized.multiply(new BigDecimal("15")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreVectorBoost(Double vectorScore) {
        if (vectorScore == null || vectorScore <= 0D) {
            return BigDecimal.ZERO;
        }
        double bounded = Math.min(1D, Math.max(0D, vectorScore));
        return BigDecimal.valueOf(bounded)
                .multiply(new BigDecimal("15"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreAttributeAlignment(String sourceTitle, Product product, ProductDetailSnapshot detailSnapshot) {
        List<String> sourceAttributes = extractAttributeTerms(sourceTitle);
        List<String> targetAttributes = new ArrayList<>();
        targetAttributes.addAll(tokens(product.title()));
        if (product.attributes() != null) {
            product.attributes().values().forEach(value -> targetAttributes.addAll(tokens(value == null ? null : value.toString())));
        }
        if (detailSnapshot != null) {
            if (detailSnapshot.attributes() != null) {
                detailSnapshot.attributes().values().forEach(value -> targetAttributes.addAll(tokens(value == null ? null : value.toString())));
            }
            targetAttributes.addAll(tokens(detailSnapshot.brand()));
            targetAttributes.addAll(tokens(detailSnapshot.description()));
        }
        return ratioScore(sourceAttributes, targetAttributes, 10);
    }

    private Map<String, BigDecimal> buildScoreBreakdown(
            BigDecimal titleOverlap,
            BigDecimal rewriteCoverage,
            BigDecimal priceReasonability,
            BigDecimal vectorBoost,
            BigDecimal attributeAlignment
    ) {
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("titleOverlap", titleOverlap);
        breakdown.put("rewriteCoverage", rewriteCoverage);
        breakdown.put("priceReasonability", priceReasonability);
        breakdown.put("vectorBoost", vectorBoost);
        breakdown.put("attributeAlignment", attributeAlignment);
        return breakdown;
    }

    private List<String> buildEvidence(
            QueryRewrite queryRewrite,
            Product product,
            ProductDetailSnapshot detailSnapshot,
            Collection<String> retrievalTerms,
            Map<String, BigDecimal> scoreBreakdown
    ) {
        List<String> evidence = new ArrayList<>();
        evidence.add("检索词: " + String.join(" / ", retrievalTerms));
        evidence.add("改写主词: " + queryRewrite.rewrittenText());
        evidence.add("命中标题: " + product.title());
        if (detailSnapshot != null && detailSnapshot.brand() != null && !detailSnapshot.brand().isBlank()) {
            evidence.add("详情品牌: " + detailSnapshot.brand());
        }
        if (product.price() != null) {
            evidence.add("参考采购价: ¥" + product.price().setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        evidence.add("分数组成: " + scoreBreakdown.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .reduce((left, right) -> left + ", " + right)
                .orElse("--"));
        return evidence;
    }

    private BigDecimal ratioScore(List<String> sourceTokens, List<String> targetTokens, int maxScore) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long overlap = sourceTokens.stream().distinct().filter(targetTokens::contains).count();
        BigDecimal ratio = BigDecimal.valueOf(overlap)
                .divide(BigDecimal.valueOf(Math.max(1, sourceTokens.stream().distinct().count())), 4, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(maxScore)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private CandidateMatchRecord toMatch(
            String taskId,
            String candidateId,
            CandidateProduct sourceCandidate,
            QueryRewrite queryRewrite,
            ScoredCandidate scoredCandidate,
            List<String> retrievalTerms
    ) {
        return CandidateMatchRecord.builder()
                .matchId("match-" + UUID.randomUUID())
                .taskId(taskId)
                .candidateId(candidateId)
                .sourceProductId(sourceCandidate.productId())
                .platform(scoredCandidate.product().platform().value())
                .externalItemId(scoredCandidate.product().id())
                .title(scoredCandidate.product().title())
                .price(scoredCandidate.product().price())
                .image(scoredCandidate.product().image())
                .link(scoredCandidate.product().link())
                .similarityScore(scoredCandidate.finalScore())
                .matchSource(resolveMatchSource(scoredCandidate))
                .searchKeyword(scoredCandidate.searchKeyword())
                .fallbackUsed(false)
                .fallbackReason(null)
                .reason(buildReason(queryRewrite, scoredCandidate))
                .retrievalTerms(retrievalTerms)
                .scoreBreakdown(scoredCandidate.scoreBreakdown())
                .evidence(scoredCandidate.evidence())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private String resolveMatchSource(ScoredCandidate scoredCandidate) {
        if (scoredCandidate.vectorHit() && scoredCandidate.catalogHit()) {
            return "CATALOG_HYBRID";
        }
        if (scoredCandidate.vectorHit()) {
            return "PGVECTOR";
        }
        return "CATALOG_TEXT";
    }

    private String buildReason(QueryRewrite queryRewrite, ScoredCandidate scoredCandidate) {
        return String.format(
                Locale.ROOT,
                "围绕“%s”完成商品库混合检索，标题重合 %.2f，改写覆盖 %.2f，价格合理性 %.2f，向量加权 %.2f，属性对齐 %.2f。",
                queryRewrite.rewrittenText(),
                scoredCandidate.titleOverlap().doubleValue(),
                scoredCandidate.rewriteCoverage().doubleValue(),
                scoredCandidate.priceReasonability().doubleValue(),
                scoredCandidate.vectorBoost().doubleValue(),
                scoredCandidate.attributeAlignment().doubleValue()
        );
    }

    public record MatchExecutionResult(
            List<CandidateMatchRecord> matches,
            boolean fallbackUsed
    ) {
    }

    private static final class MatchCandidateAccumulator {
        private Product product;
        private final LinkedHashSet<String> retrievalTerms = new LinkedHashSet<>();
        private String primarySearchTerm;
        private boolean vectorHit;
        private boolean catalogHit;
        private Double vectorScore;

        private MatchCandidateAccumulator(Product product) {
            this.product = product;
        }
    }

    private record ScoredCandidate(
            Product product,
            String searchKeyword,
            List<String> retrievalTerms,
            boolean vectorHit,
            boolean catalogHit,
            BigDecimal titleOverlap,
            BigDecimal rewriteCoverage,
            BigDecimal priceReasonability,
            BigDecimal vectorBoost,
            BigDecimal attributeAlignment,
            Map<String, BigDecimal> scoreBreakdown,
            BigDecimal finalScore,
            List<String> evidence
    ) {
    }

    private record DomesticSearchExecution(
            List<ScoredCandidate> scoredCandidates,
            List<String> retrievalTerms
    ) {
    }
}
