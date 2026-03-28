package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportCostBreakdown;
import com.globalvibe.arbitrage.domain.report.model.ReportRiskAssessment;
import com.globalvibe.arbitrage.domain.report.model.ReportSummary;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ProductAnalysisService {

    private final LLMGateway llmGateway;

    public ProductAnalysisService(LLMGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public ArbitrageReport buildReport(
            String reportId,
            CandidateProduct candidate,
            ProductDetailSnapshot domesticDetail,
            String rewrittenQuery,
            List<CandidateMatchRecord> domesticMatches
    ) {
        List<CandidateMatchRecord> topDomesticMatches = domesticMatches.stream()
                .sorted(java.util.Comparator
                        .comparing(CandidateMatchRecord::similarityScore, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(CandidateMatchRecord::price, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(3)
                .toList();

        CandidateMatchRecord benchmark = topDomesticMatches.isEmpty() ? null : topDomesticMatches.getFirst();
        BigDecimal sellingPrice = candidate.overseasPrice() != null
                ? candidate.overseasPrice()
                : BigDecimal.ZERO;
        BigDecimal sourcingCost = benchmark != null && benchmark.price() != null
                ? scale(benchmark.price())
                : scale(sellingPrice.multiply(BigDecimal.valueOf(0.45)));
        BigDecimal logisticsCost = scale(sellingPrice.multiply(BigDecimal.valueOf(0.12)));
        BigDecimal platformFee = scale(sellingPrice.multiply(BigDecimal.valueOf(0.15)));
        BigDecimal exchangeRateCost = scale(sellingPrice.multiply(BigDecimal.valueOf(0.03)));
        BigDecimal totalCost = scale(sourcingCost.add(logisticsCost).add(platformFee).add(exchangeRateCost));
        BigDecimal estimatedProfit = scale(sellingPrice.subtract(totalCost));
        BigDecimal expectedMargin = sellingPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : scale(estimatedProfit.divide(sellingPrice, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));

        String benchmarkTitle = benchmark != null ? benchmark.title() : "未找到稳定国内货源";
        String detailBrand = domesticDetail != null ? domesticDetail.brand() : "未知品牌";
        String decision = expectedMargin.compareTo(BigDecimal.valueOf(18)) >= 0 ? "recommended"
                : expectedMargin.compareTo(BigDecimal.valueOf(8)) >= 0 ? "cautious"
                : "not_recommended";
        String riskLevel = resolveRiskLevel(expectedMargin);
        LLMGateway.ReportNarrativeResult narrative = llmGateway.generateReportNarrative(
                new LLMGateway.ReportNarrativeRequest(
                        candidate.title(),
                        candidate.market(),
                        rewrittenQuery,
                        decision,
                        riskLevel,
                        scale(sellingPrice),
                        totalCost,
                        estimatedProfit,
                        expectedMargin,
                        benchmarkTitle,
                        topDomesticMatches.stream().map(CandidateMatchRecord::title).toList()
                )
        );

        return new ArbitrageReport(
                reportId,
                candidate.productId(),
                candidate.title(),
                candidate.market(),
                candidate.imageUrl(),
                decision,
                riskLevel,
                expectedMargin,
                OffsetDateTime.now(),
                new ReportSummary(
                        "insights.agentNarrative",
                        Map.of("message", narrative.summaryText())
                ),
                new ReportCostBreakdown(
                        sourcingCost,
                        logisticsCost,
                        platformFee,
                        exchangeRateCost,
                        totalCost,
                        scale(sellingPrice),
                        estimatedProfit
                ),
                new ReportRiskAssessment(
                        resolveRiskScore(expectedMargin),
                        List.of("price-competitiveness", "category-demand", "market-benchmark"),
                        buildRiskNotes(narrative, domesticDetail != null, benchmark != null, detailBrand)
                ),
                buildRecommendations(narrative),
                topDomesticMatches.stream().map(item -> new DomesticProductMatch(
                        item.matchId(),
                        item.platform(),
                        item.externalItemId(),
                        item.title(),
                        item.price(),
                        item.image(),
                        item.similarityScore().setScale(0, RoundingMode.HALF_UP).intValue(),
                        item.link(),
                        buildSearchUrl(rewrittenQuery),
                        item.reason()
                )).toList()
        );
    }

    private List<String> buildRiskNotes(
            LLMGateway.ReportNarrativeResult narrative,
            boolean hasDetail,
            boolean hasBenchmark,
            String detailBrand
    ) {
        java.util.ArrayList<String> notes = new java.util.ArrayList<>();
        if (narrative.riskNotes() != null) {
            notes.addAll(narrative.riskNotes());
        }
        notes.add(hasDetail
                ? "已纳入历史详情快照中的品牌、属性与 SKU 信息，当前参考品牌为 " + detailBrand + "。"
                : "未命中实时详情，当前主要基于历史标题和价格数据做分析。");
        notes.add(hasBenchmark
                ? "已找到可用于解释价差的国内历史货源样本。"
                : "当前没有高置信国内货源，建议把本次结果定位为趋势验证而非直接上架结论。");
        if (narrative.fallbackUsed() && narrative.fallbackReason() != null && !narrative.fallbackReason().isBlank()) {
            notes.add(narrative.fallbackReason());
        }
        return notes;
    }

    private List<String> buildRecommendations(LLMGateway.ReportNarrativeResult narrative) {
        if (narrative.recommendations() != null && !narrative.recommendations().isEmpty()) {
            return narrative.recommendations();
        }
        return List.of(
                "建议优先核对国内 SKU 规格与 Amazon 主卖点一致性。",
                "建议补充运费、税费、佣金等真实成本后再做最终利润确认。",
                "建议结合 Amazon 评论数和评分判断海外市场竞争强度。"
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveRiskLevel(BigDecimal margin) {
        if (margin.compareTo(BigDecimal.valueOf(18)) >= 0) {
            return "low";
        }
        if (margin.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "medium";
        }
        return "high";
    }

    private int resolveRiskScore(BigDecimal margin) {
        if (margin.compareTo(BigDecimal.valueOf(18)) >= 0) {
            return 82;
        }
        if (margin.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return 63;
        }
        return 38;
    }

    private String buildSearchUrl(String rewrittenQuery) {
        return "https://s.1688.com/selloffer/offer_search.htm?keywords=" + java.net.URLEncoder.encode(rewrittenQuery, java.nio.charset.StandardCharsets.UTF_8);
    }
}
