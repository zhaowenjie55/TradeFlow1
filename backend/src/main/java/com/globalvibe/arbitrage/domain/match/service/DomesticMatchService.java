package com.globalvibe.arbitrage.domain.match.service;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.repository.CandidateMatchRepository;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.service.DomesticProductFallbackService;
import com.globalvibe.arbitrage.domain.product.service.ProductCatalogSyncService;
import com.globalvibe.arbitrage.domain.product.service.DomesticVectorSearchService;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    public DomesticMatchService(
            ProductCatalogSyncService productCatalogSyncService,
            DomesticProductFallbackService domesticProductFallbackService,
            CandidateMatchRepository candidateMatchRepository,
            ObjectProvider<DomesticVectorSearchService> domesticVectorSearchServiceProvider
    ) {
        this.productCatalogSyncService = productCatalogSyncService;
        this.domesticProductFallbackService = domesticProductFallbackService;
        this.candidateMatchRepository = candidateMatchRepository;
        this.domesticVectorSearchService = domesticVectorSearchServiceProvider.getIfAvailable();
    }

    public MatchExecutionResult match(String taskId, String candidateId, String sourceProductId, String sourceTitle, QueryRewrite queryRewrite, int limit) {
        DomesticSearchExecution searchExecution = searchDomesticProducts(queryRewrite, limit);
        boolean fallbackUsed = searchExecution.fallbackUsed();
        List<CandidateMatchRecord> matches = searchExecution.products().stream()
                .sorted(Comparator.comparing((Product product) -> similarity(sourceTitle, queryRewrite.rewrittenText(), product.title())).reversed())
                .limit(limit)
                .map(product -> toMatch(
                        taskId,
                        candidateId,
                        sourceProductId,
                        sourceTitle,
                        queryRewrite,
                        product,
                        searchExecution.fallbackUsed(),
                        searchExecution.fallbackReason()
                ))
                .toList();

        if (matches.isEmpty()) {
            fallbackUsed = true;
            matches = markFallback(
                    candidateMatchRepository.findTopBySourceProductId(sourceProductId, limit),
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

    private DomesticSearchExecution searchDomesticProducts(QueryRewrite queryRewrite, int limit) {
        int searchLimit = Math.max(limit, 10);
        Map<String, Product> merged = new LinkedHashMap<>();
        boolean fallbackUsed = false;
        String fallbackReason = null;
        for (String term : buildSearchTerms(queryRewrite)) {
            if (domesticVectorSearchService != null) {
                List<Product> vectorProducts = domesticVectorSearchService.search(term, searchLimit);
                if (!vectorProducts.isEmpty()) {
                    vectorProducts.forEach(product -> merged.put(product.id(), product));
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
                liveProducts.forEach(product -> merged.put(product.id(), product));
            } else {
                List<Product> historicalProducts = domesticProductFallbackService.searchHistoricalProducts(
                        MarketplaceType.TAOBAO,
                        term,
                        searchLimit
                );
                if (!historicalProducts.isEmpty()) {
                    fallbackUsed = true;
                    if (fallbackReason == null) {
                        fallbackReason = "domestic live search unavailable or returned empty results";
                    }
                    historicalProducts.forEach(product -> merged.put(product.id(), product));
                }
            }
            if (merged.size() >= searchLimit) {
                break;
            }
        }

        return new DomesticSearchExecution(
                new ArrayList<>(merged.values()),
                fallbackUsed,
                fallbackReason
        );
    }

    private List<String> buildSearchTerms(QueryRewrite queryRewrite) {
        List<String> searchTerms = new ArrayList<>();
        if (queryRewrite.rewrittenText() != null && !queryRewrite.rewrittenText().isBlank()) {
            searchTerms.add(queryRewrite.rewrittenText());
        }
        if (queryRewrite.keywords() == null) {
            return searchTerms;
        }
        for (String keyword : queryRewrite.keywords()) {
            if (keyword != null && !keyword.isBlank() && !searchTerms.contains(keyword)) {
                searchTerms.add(keyword);
            }
        }
        return searchTerms;
    }

    private CandidateMatchRecord toMatch(
            String taskId,
            String candidateId,
            String sourceProductId,
            String sourceTitle,
            QueryRewrite queryRewrite,
            Product product,
            boolean fallbackUsed,
            String fallbackReason
    ) {
        return CandidateMatchRecord.builder()
                .matchId("match-" + UUID.randomUUID())
                .taskId(taskId)
                .candidateId(candidateId)
                .sourceProductId(sourceProductId)
                .platform(product.platform().name())
                .externalItemId(product.id())
                .title(product.title())
                .price(product.price())
                .image(product.image())
                .link(product.link())
                .similarityScore(BigDecimal.valueOf(similarity(sourceTitle, queryRewrite.rewrittenText(), product.title()) * 100)
                        .setScale(2, java.math.RoundingMode.HALF_UP))
                .matchSource(fallbackUsed ? "DOMESTIC_HISTORY_SEARCH" : "DOMESTIC_CATALOG_SEARCH")
                .searchKeyword(queryRewrite.rewrittenText())
                .fallbackUsed(fallbackUsed)
                .fallbackReason(fallbackReason)
                .reason(fallbackUsed
                        ? "国内实时搜索不可用，已基于数据库中的历史商品快照与改写搜索词进行匹配。"
                        : "基于改写搜索词 “" + queryRewrite.rewrittenText() + "” 和标题相似度匹配得到。")
                .createdAt(OffsetDateTime.now())
                .build();
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

    private double similarity(String sourceTitle, String rewrittenText, String targetTitle) {
        String left = normalize(sourceTitle + " " + rewrittenText);
        String right = normalize(targetTitle);
        if (left.isBlank() || right.isBlank()) {
            return 0D;
        }
        List<String> sourceTokens = tokens(left);
        List<String> targetTokens = tokens(right);
        long overlap = sourceTokens.stream().distinct().filter(targetTokens::contains).count();
        double coverage = (double) overlap / Math.max(1, sourceTokens.size());
        return Math.min(0.99, 0.25 + coverage * 0.7);
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
            List<Product> products,
            boolean fallbackUsed,
            String fallbackReason
    ) {
    }
}
