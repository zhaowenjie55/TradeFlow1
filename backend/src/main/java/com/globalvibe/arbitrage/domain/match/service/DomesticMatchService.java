package com.globalvibe.arbitrage.domain.match.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.repository.CandidateMatchRepository;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.service.DomesticProductFallbackService;
import com.globalvibe.arbitrage.domain.product.service.DomesticVectorSearchService;
import com.globalvibe.arbitrage.domain.product.service.DomesticVectorSearchService.VectorSearchResult;
import com.globalvibe.arbitrage.domain.product.service.ProductCatalogSyncService;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DomesticMatchService {

    private final ProductCatalogSyncService productCatalogSyncService;
    private final DomesticProductFallbackService domesticProductFallbackService;
    private final CandidateMatchRepository candidateMatchRepository;
    private final DomesticVectorSearchService domesticVectorSearchService;
    private final VectorSearchProperties vectorSearchProperties;

    public DomesticMatchService(
            ProductCatalogSyncService productCatalogSyncService,
            DomesticProductFallbackService domesticProductFallbackService,
            CandidateMatchRepository candidateMatchRepository,
            ObjectProvider<DomesticVectorSearchService> domesticVectorSearchServiceProvider,
            VectorSearchProperties vectorSearchProperties
    ) {
        this.productCatalogSyncService = productCatalogSyncService;
        this.domesticProductFallbackService = domesticProductFallbackService;
        this.candidateMatchRepository = candidateMatchRepository;
        this.domesticVectorSearchService = domesticVectorSearchServiceProvider.getIfAvailable();
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
        boolean searchFallbackUsed = searchExecution.fallbackUsed();
        boolean fallbackUsed = searchFallbackUsed;

        List<CandidateMatchRecord> matches = searchExecution.scoredCandidates().stream()
                .sorted(Comparator
                        .comparing(ScoredCandidate::finalScore, Comparator.reverseOrder())
                        .thenComparing(item -> item.product().price(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(item -> toMatch(
                        taskId,
                        candidateId,
                        sourceCandidate,
                        queryRewrite,
                        item,
                        searchFallbackUsed,
                        searchExecution.fallbackReason()
                ))
                .toList();

        if (matches.isEmpty()) {
            fallbackUsed = true;
            matches = markFallback(
                    candidateMatchRepository.findTopBySourceProductId(sourceCandidate.productId(), limit),
                    "HISTORICAL_SOURCE_PRODUCT_MATCH",
                    "no domestic catalog results for rewritten query"
            );
        }

        if (matches.isEmpty()) {
            fallbackUsed = true;
            matches = markFallback(
                    candidateMatchRepository.findTopByCandidateId(candidateId, limit),
                    "HISTORICAL_CANDIDATE_MATCH",
                    "no source-product historical match available"
            );
        } else {
            candidateMatchRepository.replaceForCandidate(candidateId, matches);
        }

        return new MatchExecutionResult(matches, fallbackUsed);
    }

    private DomesticSearchExecution searchDomesticProducts(CandidateProduct sourceCandidate, QueryRewrite queryRewrite, int limit) {
        int searchLimit = Math.max(limit * 3, 12);
        Map<String, MatchCandidateAccumulator> merged = new LinkedHashMap<>();
        boolean fallbackUsed = false;
        String fallbackReason = null;

        for (String term : buildSearchTerms(queryRewrite)) {
            if (domesticVectorSearchService != null) {
                List<VectorSearchResult> vectorProducts = domesticVectorSearchService.searchWithScores(term, searchLimit);
                if (!vectorProducts.isEmpty()) {
                    vectorProducts.forEach(hit -> mergeCandidate(merged, hit.product(), term, hit.score(), true, false, false));
                }
            }

            List<Product> liveProducts = List.of();
            try {
                liveProducts = productCatalogSyncService.syncDomesticKeywordProducts(term);
            } catch (RuntimeException ex) {
                fallbackUsed = true;
                fallbackReason = fallbackReason == null ? ex.getMessage() : fallbackReason;
            }

            if (!liveProducts.isEmpty()) {
                liveProducts.forEach(product -> mergeCandidate(merged, product, term, null, false, true, false));
            } else {
                List<Product> historicalProducts = domesticProductFallbackService.searchHistoricalProducts(
                        MarketplaceType.fromValue("TAOBAO"),
                        term,
                        searchLimit
                );
                if (!historicalProducts.isEmpty()) {
                    fallbackUsed = true;
                    if (fallbackReason == null) {
                        fallbackReason = "domestic live search unavailable or returned empty results";
                    }
                    historicalProducts.forEach(product -> mergeCandidate(merged, product, term, null, false, false, true));
                }
            }
        }

        return new DomesticSearchExecution(
                scoreCandidates(sourceCandidate, queryRewrite, merged.values()),
                fallbackUsed,
                fallbackReason
        );
    }

    private void mergeCandidate(
            Map<String, MatchCandidateAccumulator> merged,
            Product product,
            String term,
            Double vectorScore,
            boolean vectorHit,
            boolean liveHit,
            boolean historicalHit
    ) {
        MatchCandidateAccumulator accumulator = merged.computeIfAbsent(
                product.id(),
                ignored -> new MatchCandidateAccumulator(product)
        );
        accumulator.product = product;
        accumulator.searchTerms.add(term);
        if (accumulator.primarySearchTerm == null) {
            accumulator.primarySearchTerm = term;
        }
        if (vectorHit) {
            accumulator.vectorHit = true;
            accumulator.vectorScore = accumulator.vectorScore == null
                    ? vectorScore
                    : Math.max(accumulator.vectorScore, vectorScore == null ? 0D : vectorScore);
        }
        accumulator.liveHit = accumulator.liveHit || liveHit;
        accumulator.historicalHit = accumulator.historicalHit || historicalHit;
    }

    private List<String> buildSearchTerms(QueryRewrite queryRewrite) {
        LinkedHashSet<String> searchTerms = new LinkedHashSet<>();
        if (queryRewrite.rewrittenText() != null && !queryRewrite.rewrittenText().isBlank()) {
            searchTerms.add(queryRewrite.rewrittenText().trim());
        }
        searchTerms.add(vectorSearchProperties.getFixedKeyword());
        if (queryRewrite.keywords() != null) {
            queryRewrite.keywords().stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .map(String::trim)
                    .forEach(searchTerms::add);
        }
        return new ArrayList<>(searchTerms);
    }

    private List<ScoredCandidate> scoreCandidates(
            CandidateProduct sourceCandidate,
            QueryRewrite queryRewrite,
            java.util.Collection<MatchCandidateAccumulator> candidates
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
                    BigDecimal titleOverlap = scoreTitleOverlap(sourceCandidate.title(), candidate.product.title());
                    BigDecimal rewriteCoverage = scoreRewriteCoverage(queryRewrite, candidate.product.title());
                    BigDecimal priceReasonability = scorePriceReasonability(candidate.product.price(), minPrice, maxPrice);
                    BigDecimal vectorBoost = scoreVectorBoost(candidate.vectorScore);
                    BigDecimal finalScore = titleOverlap
                            .add(rewriteCoverage)
                            .add(priceReasonability)
                            .add(vectorBoost)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ScoredCandidate(
                            candidate.product,
                            candidate.primarySearchTerm,
                            candidate.vectorHit,
                            candidate.liveHit,
                            candidate.historicalHit,
                            titleOverlap,
                            rewriteCoverage,
                            priceReasonability,
                            vectorBoost,
                            finalScore
                    );
                })
                .toList();
    }

    private BigDecimal scoreTitleOverlap(String sourceTitle, String targetTitle) {
        return ratioScore(tokens(normalize(sourceTitle)), tokens(normalize(targetTitle)), 40);
    }

    private BigDecimal scoreRewriteCoverage(QueryRewrite queryRewrite, String targetTitle) {
        List<String> rewriteTokens = new ArrayList<>();
        rewriteTokens.addAll(tokens(normalize(queryRewrite.rewrittenText())));
        if (queryRewrite.keywords() != null) {
            queryRewrite.keywords().forEach(keyword -> rewriteTokens.addAll(tokens(normalize(keyword))));
        }
        return ratioScore(rewriteTokens, tokens(normalize(targetTitle)), 25);
    }

    private BigDecimal scorePriceReasonability(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (maxPrice.compareTo(minPrice) <= 0) {
            return new BigDecimal("10.00");
        }
        BigDecimal denominator = maxPrice.subtract(minPrice);
        BigDecimal normalized = maxPrice.subtract(price)
                .divide(denominator, 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return normalized.multiply(new BigDecimal("20")).setScale(2, RoundingMode.HALF_UP);
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

    private BigDecimal ratioScore(List<String> sourceTokens, List<String> targetTokens, int maxScore) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long overlap = sourceTokens.stream().distinct().filter(targetTokens::contains).count();
        BigDecimal ratio = BigDecimal.valueOf(overlap)
                .divide(BigDecimal.valueOf(Math.max(1, sourceTokens.stream().distinct().count())), 4, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(maxScore)).setScale(2, RoundingMode.HALF_UP);
    }

    private CandidateMatchRecord toMatch(
            String taskId,
            String candidateId,
            CandidateProduct sourceCandidate,
            QueryRewrite queryRewrite,
            ScoredCandidate scoredCandidate,
            boolean fallbackUsed,
            String fallbackReason
    ) {
        boolean recordFallback = fallbackUsed || (scoredCandidate.historicalHit() && !scoredCandidate.liveHit());
        String resolvedFallbackReason = recordFallback && (fallbackReason == null || fallbackReason.isBlank())
                ? "domestic live search unavailable or returned empty results"
                : fallbackReason;
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
                .fallbackUsed(recordFallback)
                .fallbackReason(resolvedFallbackReason)
                .reason(buildReason(queryRewrite, scoredCandidate))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private String resolveMatchSource(ScoredCandidate scoredCandidate) {
        if (scoredCandidate.vectorHit() && scoredCandidate.historicalHit() && !scoredCandidate.liveHit()) {
            return "DOMESTIC_VECTOR_HISTORY_SEARCH";
        }
        if (scoredCandidate.vectorHit() && scoredCandidate.liveHit()) {
            return "DOMESTIC_VECTOR_LIVE_SEARCH";
        }
        if (scoredCandidate.historicalHit() && !scoredCandidate.liveHit()) {
            return "DOMESTIC_HISTORY_SEARCH";
        }
        if (scoredCandidate.vectorHit()) {
            return "DOMESTIC_VECTOR_SEARCH";
        }
        return "DOMESTIC_CATALOG_SEARCH";
    }

    private String buildReason(QueryRewrite queryRewrite, ScoredCandidate scoredCandidate) {
        return "基于搜索词 “" + scoredCandidate.searchKeyword() + "” 完成匹配，标题重叠得分 "
                + scoredCandidate.titleOverlapScore().stripTrailingZeros().toPlainString()
                + "，改写覆盖得分 "
                + scoredCandidate.rewriteCoverageScore().stripTrailingZeros().toPlainString()
                + "，价格合理性得分 "
                + scoredCandidate.priceReasonabilityScore().stripTrailingZeros().toPlainString()
                + "，向量命中加权分 "
                + scoredCandidate.vectorBoostScore().stripTrailingZeros().toPlainString()
                + "；改写主词为 “" + queryRewrite.rewrittenText() + "”。";
    }

    private List<CandidateMatchRecord> markFallback(List<CandidateMatchRecord> records, String matchSource, String fallbackReason) {
        return records.stream()
                .map(record -> CandidateMatchRecord.builder()
                        .matchId(record.matchId())
                        .taskId(record.taskId())
                        .candidateId(record.candidateId())
                        .sourceProductId(record.sourceProductId())
                        .platform(record.platform())
                        .externalItemId(record.externalItemId())
                        .title(record.title())
                        .price(record.price())
                        .image(record.image())
                        .link(record.link())
                        .similarityScore(record.similarityScore())
                        .matchSource(matchSource)
                        .searchKeyword(record.searchKeyword())
                        .fallbackUsed(true)
                        .fallbackReason(fallbackReason)
                        .reason(record.reason())
                        .createdAt(record.createdAt())
                        .build())
                .toList();
    }

    private List<String> tokens(String value) {
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsHan}a-z0-9]+", " ").trim();
    }

    public record MatchExecutionResult(
            List<CandidateMatchRecord> matches,
            boolean fallbackUsed
    ) {
    }

    private record DomesticSearchExecution(
            List<ScoredCandidate> scoredCandidates,
            boolean fallbackUsed,
            String fallbackReason
    ) {
    }

    private static final class MatchCandidateAccumulator {

        private Product product;
        private String primarySearchTerm;
        private final LinkedHashSet<String> searchTerms = new LinkedHashSet<>();
        private Double vectorScore;
        private boolean vectorHit;
        private boolean liveHit;
        private boolean historicalHit;

        private MatchCandidateAccumulator(Product product) {
            this.product = product;
        }
    }

    private record ScoredCandidate(
            Product product,
            String searchKeyword,
            boolean vectorHit,
            boolean liveHit,
            boolean historicalHit,
            BigDecimal titleOverlapScore,
            BigDecimal rewriteCoverageScore,
            BigDecimal priceReasonabilityScore,
            BigDecimal vectorBoostScore,
            BigDecimal finalScore
    ) {
    }
}
