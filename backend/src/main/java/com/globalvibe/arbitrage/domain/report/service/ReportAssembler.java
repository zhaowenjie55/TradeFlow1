package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.analysis.service.PricingEngine;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTrace;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportCostBreakdown;
import com.globalvibe.arbitrage.domain.report.model.ReportRiskAssessment;
import com.globalvibe.arbitrage.domain.report.model.ReportSummary;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportAssembler {

    public ArbitrageReport assemble(ReportAssemblyInput input) {
        List<DomesticProductMatch> domesticMatches = buildDomesticMatches(
                input.topDomesticMatches(),
                input.detailSnapshots(),
                input.queryRewrite()
        );

        return new ArbitrageReport(
                input.reportId(),
                input.candidate().productId(),
                input.candidate().title(),
                input.candidate().market(),
                input.candidate().imageUrl(),
                input.decision(),
                input.riskLevel(),
                input.pricing().expectedMargin(),
                OffsetDateTime.now(),
                new ReportSummary("insights.agentNarrative", buildSummaryParams(input.narrative())),
                new ReportCostBreakdown(
                        input.pricing().sourcingCost(),
                        input.pricing().domesticShippingCost(),
                        input.pricing().logisticsCost(),
                        input.pricing().platformFee(),
                        input.pricing().exchangeRateCost(),
                        input.pricing().totalCost(),
                        input.pricing().amazonPriceRmb(),
                        input.pricing().estimatedProfit()
                ),
                new ReportRiskAssessment(
                        input.riskScore(),
                        List.of("price-competitiveness", "category-demand", "market-benchmark"),
                        input.riskNotes()
                ),
                buildRecommendations(input.narrative()),
                domesticMatches,
                input.analysisTrace(),
                buildAuditData(
                        input.sourceProduct(),
                        input.queryRewrite(),
                        input.benchmark(),
                        domesticMatches,
                        input.pricing().amazonPriceUsd(),
                        input.pricing().usdToCnyRate(),
                        input.pricing().amazonPriceRmb(),
                        input.shippingText(),
                        input.narrative(),
                        input.analysisTrace()
                )
        );
    }

    private List<DomesticProductMatch> buildDomesticMatches(
            List<CandidateMatchRecord> topDomesticMatches,
            Map<String, ProductDetailSnapshot> detailSnapshots,
            QueryRewrite queryRewrite
    ) {
        if (topDomesticMatches == null || topDomesticMatches.isEmpty()) {
            return List.of();
        }
        return topDomesticMatches.stream().map(item -> {
            ProductDetailSnapshot detailSnapshot = detailSnapshots.get(item.externalItemId());
            boolean detailReady = detailSnapshot != null;
            String detailSource = detailReady
                    ? (item.matchSource() != null && item.matchSource().contains("REALTIME") ? "DOMESTIC_REALTIME_DETAIL" : "DETAIL_SNAPSHOT")
                    : "SEARCH_RESULT_ONLY";
            return new DomesticProductMatch(
                    item.matchId(),
                    item.platform(),
                    item.externalItemId(),
                    item.title(),
                    item.price(),
                    item.image(),
                    item.similarityScore().setScale(0, RoundingMode.HALF_UP).intValue(),
                    item.link(),
                    buildSearchUrl(queryRewrite.rewrittenText()),
                    item.reason(),
                    item.matchSource(),
                    detailReady,
                    detailSource,
                    item.retrievalTerms(),
                    item.scoreBreakdown(),
                    item.evidence()
            );
        }).toList();
    }

    private List<String> buildRecommendations(LLMGateway.ReportNarrativeResult narrative) {
        if (narrative.recommendations() != null && !narrative.recommendations().isEmpty()) {
            return narrative.recommendations();
        }
        return List.of(
                "优先核对国内 SKU 规格与 Amazon 主卖点是否一致。",
                "补充真实头程、税费与平台佣金后再确认最终利润。",
                "结合 Amazon 评论量与评分继续判断需求稳定性。"
        );
    }

    private String buildSearchUrl(String rewrittenQuery) {
        String keyword = rewrittenQuery == null || rewrittenQuery.isBlank() ? "1688 货源" : rewrittenQuery;
        return "https://s.1688.com/selloffer/offer_search.htm?keywords=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Map<String, Object> buildSummaryParams(LLMGateway.ReportNarrativeResult narrative) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", narrative.summaryText());
        params.put("provider", narrative.provider());
        params.put("model", narrative.model());
        params.put("currency", "CNY");
        return params;
    }

    private Map<String, Object> buildAuditData(
            Product sourceProduct,
            QueryRewrite queryRewrite,
            CandidateMatchRecord benchmark,
            List<DomesticProductMatch> renderedDomesticMatches,
            BigDecimal amazonPriceUsd,
            BigDecimal usdToCnyRate,
            BigDecimal amazonPriceRmb,
            String shippingText,
            LLMGateway.ReportNarrativeResult narrative,
            AnalysisTrace analysisTrace
    ) {
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("priceAmountUsd", amazonPriceUsd);
        auditData.put("usdToCnyRate", usdToCnyRate);
        auditData.put("amazonPriceRmb", amazonPriceRmb);
        auditData.put("shippingText", shippingText);
        auditData.put("rewrittenText", queryRewrite.rewrittenText());
        auditData.put("rewrittenKeywords", queryRewrite.keywords());
        auditData.put("rewriteProvider", queryRewrite.gatewaySource());
        auditData.put("rewriteModel", queryRewrite.gatewayModel());
        auditData.put("rewriteFallbackUsed", queryRewrite.fallbackUsed());
        auditData.put("rewriteFallbackReason", queryRewrite.fallbackReason());
        if (benchmark != null) {
            auditData.put("retrievalSource", benchmark.matchSource());
            auditData.put("retrievalFallbackUsed", benchmark.fallbackUsed());
            auditData.put("retrievalFallbackReason", benchmark.fallbackReason());
        }
        if (renderedDomesticMatches != null && !renderedDomesticMatches.isEmpty()) {
            auditData.put("detailSource", renderedDomesticMatches.get(0).detailSource());
        }
        auditData.put("narrativeProvider", narrative.provider());
        auditData.put("narrativeModel", narrative.model());
        auditData.put("narrativeFallbackUsed", narrative.fallbackUsed());
        auditData.put("narrativeFallbackReason", narrative.fallbackReason());
        auditData.put("analysisTrace", analysisTrace);
        if (sourceProduct != null && sourceProduct.rawData() != null && !sourceProduct.rawData().isEmpty()) {
            auditData.put("sourceRawData", sourceProduct.rawData());
        }
        return auditData;
    }

    public record ReportAssemblyInput(
            String reportId,
            CandidateProduct candidate,
            Product sourceProduct,
            QueryRewrite queryRewrite,
            CandidateMatchRecord benchmark,
            List<CandidateMatchRecord> topDomesticMatches,
            Map<String, ProductDetailSnapshot> detailSnapshots,
            PricingEngine.PricingBreakdown pricing,
            String decision,
            String riskLevel,
            int riskScore,
            String shippingText,
            LLMGateway.ReportNarrativeResult narrative,
            AnalysisTrace analysisTrace,
            List<String> riskNotes
    ) {
    }
}
