package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchSelectionPolicyTest {

    private final MatchSelectionPolicy policy = new MatchSelectionPolicy();

    @Test
    void shouldPreferRealtimeDetailedMatchAsBenchmark() {
        CandidateMatchRecord weakTopScore = match(
                "match-weak",
                "DOMESTIC_REALTIME",
                false,
                new BigDecimal("82.00"),
                Map.of(
                        "titleOverlap", new BigDecimal("8.00"),
                        "rewriteCoverage", new BigDecimal("8.00"),
                        "categoryAlignment", new BigDecimal("-8.00"),
                        "accessoryPenalty", BigDecimal.ZERO
                )
        );
        CandidateMatchRecord strongRealtime = match(
                "match-strong",
                "DOMESTIC_REALTIME",
                false,
                new BigDecimal("74.00"),
                Map.of(
                        "titleOverlap", new BigDecimal("18.00"),
                        "rewriteCoverage", new BigDecimal("16.00"),
                        "categoryAlignment", new BigDecimal("10.00"),
                        "accessoryPenalty", BigDecimal.ZERO
                )
        );

        Map<String, ProductDetailSnapshot> detailSnapshots = Map.of(
                "match-strong", detail("match-strong")
        );

        CandidateMatchRecord benchmark = policy.selectBenchmark(List.of(weakTopScore, strongRealtime), detailSnapshots, true);

        assertEquals("match-strong", benchmark.externalItemId());
    }

    @Test
    void shouldRejectStrictRealtimeBenchmarkWhenOnlyFallbackMatchesExist() {
        CandidateMatchRecord fallbackMatch = match(
                "match-fallback",
                "CATALOG_HYBRID",
                true,
                new BigDecimal("88.00"),
                Map.of(
                        "titleOverlap", new BigDecimal("20.00"),
                        "rewriteCoverage", new BigDecimal("12.00"),
                        "categoryAlignment", new BigDecimal("8.00"),
                        "accessoryPenalty", BigDecimal.ZERO
                )
        );

        CandidateMatchRecord benchmark = policy.selectBenchmark(List.of(fallbackMatch), Map.of(), true);

        assertNull(benchmark);
    }

    @Test
    void shouldAllowCrossLanguageRealtimeBenchmarkWithoutTitleOverlap() {
        CandidateMatchRecord realtimeMatch = match(
                "match-cn-coffee",
                "DOMESTIC_REALTIME",
                false,
                new BigDecimal("29.38"),
                Map.of(
                        "titleOverlap", BigDecimal.ZERO,
                        "rewriteCoverage", new BigDecimal("5.00"),
                        "attributeAlignment", BigDecimal.ZERO,
                        "categoryAlignment", new BigDecimal("10.00"),
                        "accessoryPenalty", BigDecimal.ZERO
                )
        );

        CandidateMatchRecord benchmark = policy.selectBenchmark(
                List.of(realtimeMatch),
                Map.of("match-cn-coffee", detail("match-cn-coffee")),
                true
        );

        assertEquals("match-cn-coffee", benchmark.externalItemId());
    }

    private CandidateMatchRecord match(
            String externalItemId,
            String source,
            boolean fallbackUsed,
            BigDecimal similarityScore,
            Map<String, BigDecimal> scoreBreakdown
    ) {
        return CandidateMatchRecord.builder()
                .matchId("m-" + externalItemId)
                .taskId("phase2-1")
                .candidateId("phase1-1:test")
                .sourceProductId("amz-test")
                .platform("1688")
                .externalItemId(externalItemId)
                .title("测试商品 " + externalItemId)
                .price(new BigDecimal("12.00"))
                .similarityScore(similarityScore)
                .matchSource(source)
                .fallbackUsed(fallbackUsed)
                .retrievalTerms(List.of("测试商品"))
                .scoreBreakdown(scoreBreakdown)
                .evidence(List.of())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private ProductDetailSnapshot detail(String productId) {
        return new ProductDetailSnapshot(
                productId,
                com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType.ALIBABA_1688,
                "测试详情",
                new BigDecimal("12.00"),
                "品牌",
                null,
                null,
                null,
                List.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }
}
