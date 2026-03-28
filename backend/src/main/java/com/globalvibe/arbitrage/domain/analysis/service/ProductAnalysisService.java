package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.config.PricingProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportCostBreakdown;
import com.globalvibe.arbitrage.domain.report.model.ReportRiskAssessment;
import com.globalvibe.arbitrage.domain.report.model.ReportSummary;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductAnalysisService {

    private final LLMGateway llmGateway;
    private final PricingProperties pricingProperties;
    private final ProductRepository productRepository;

    public ProductAnalysisService(
            LLMGateway llmGateway,
            PricingProperties pricingProperties,
            ProductRepository productRepository
    ) {
        this.llmGateway = llmGateway;
        this.pricingProperties = pricingProperties;
        this.productRepository = productRepository;
    }

    public ArbitrageReport buildReport(
            String reportId,
            CandidateProduct candidate,
            ProductDetailSnapshot domesticDetail,
            QueryRewrite queryRewrite,
            List<CandidateMatchRecord> domesticMatches
    ) {
        List<CandidateMatchRecord> topDomesticMatches = domesticMatches.stream()
                .sorted(java.util.Comparator
                        .comparing(CandidateMatchRecord::similarityScore, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(CandidateMatchRecord::price, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(3)
                .toList();

        CandidateMatchRecord benchmark = topDomesticMatches.isEmpty() ? null : topDomesticMatches.get(0);
        Product sourceProduct = productRepository.findById(candidate.productId()).orElse(null);
        BigDecimal amazonPriceUsd = candidate.overseasPrice() != null
                ? scale(candidate.overseasPrice())
                : extractDecimal(sourceProduct, "priceAmountUsd").orElse(BigDecimal.ZERO);
        BigDecimal amazonPriceRmb = scale(amazonPriceUsd.multiply(pricingProperties.getUsdToCnyRate()));
        BigDecimal sourcingCost = benchmark != null && benchmark.price() != null
                ? scale(benchmark.price())
                : scale(amazonPriceRmb.multiply(pricingProperties.getFallbackSourcingRate()));
        String shippingText = resolveShippingText(domesticDetail);
        BigDecimal domesticShippingCost = parseShippingCost(shippingText);
        BigDecimal logisticsCost = scale(amazonPriceRmb.multiply(pricingProperties.getCrossBorderLogisticsRate()));
        BigDecimal platformFee = scale(amazonPriceRmb.multiply(pricingProperties.getPlatformFeeRate()));
        BigDecimal exchangeRateCost = scale(amazonPriceRmb.multiply(pricingProperties.getExchangeLossRate()));
        BigDecimal totalCost = scale(sourcingCost
                .add(domesticShippingCost)
                .add(logisticsCost)
                .add(platformFee)
                .add(exchangeRateCost));
        BigDecimal estimatedProfit = scale(amazonPriceRmb.subtract(totalCost));
        BigDecimal expectedMargin = amazonPriceRmb.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : scale(estimatedProfit.divide(amazonPriceRmb, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));

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
                        queryRewrite.rewrittenText(),
                        queryRewrite.keywords(),
                        decision,
                        riskLevel,
                        amazonPriceUsd,
                        amazonPriceRmb,
                        sourcingCost,
                        domesticShippingCost,
                        logisticsCost,
                        platformFee,
                        exchangeRateCost,
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
                        buildSummaryParams(narrative)
                ),
                new ReportCostBreakdown(
                        sourcingCost,
                        domesticShippingCost,
                        logisticsCost,
                        platformFee,
                        exchangeRateCost,
                        totalCost,
                        amazonPriceRmb,
                        estimatedProfit
                ),
                new ReportRiskAssessment(
                        resolveRiskScore(expectedMargin),
                        List.of("price-competitiveness", "category-demand", "market-benchmark"),
                        buildRiskNotes(narrative, domesticDetail != null, benchmark != null, detailBrand, shippingText)
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
                        buildSearchUrl(queryRewrite.rewrittenText()),
                        item.reason()
                )).toList(),
                buildAuditData(sourceProduct, queryRewrite, amazonPriceUsd, amazonPriceRmb, shippingText, narrative)
        );
    }

    private List<String> buildRiskNotes(
            LLMGateway.ReportNarrativeResult narrative,
            boolean hasDetail,
            boolean hasBenchmark,
            String detailBrand,
            String shippingText
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
        if (shippingText != null && !shippingText.isBlank()) {
            notes.add("国内运费按商品快照中的 “" + shippingText + "” 解析。");
        }
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
        String keyword = rewrittenQuery == null || rewrittenQuery.isBlank() ? "亚克力透明收纳架" : rewrittenQuery;
        return "https://s.1688.com/selloffer/offer_search.htm?keywords=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Map<String, Object> buildSummaryParams(LLMGateway.ReportNarrativeResult narrative) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("message", narrative.summaryText());
        params.put("provider", narrative.provider());
        params.put("currency", "CNY");
        return params;
    }

    private Map<String, Object> buildAuditData(
            Product sourceProduct,
            QueryRewrite queryRewrite,
            BigDecimal amazonPriceUsd,
            BigDecimal amazonPriceRmb,
            String shippingText,
            LLMGateway.ReportNarrativeResult narrative
    ) {
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("priceAmountUsd", amazonPriceUsd);
        auditData.put("usdToCnyRate", pricingProperties.getUsdToCnyRate());
        auditData.put("amazonPriceRmb", amazonPriceRmb);
        auditData.put("shippingText", shippingText);
        auditData.put("rewrittenText", queryRewrite.rewrittenText());
        auditData.put("rewrittenKeywords", queryRewrite.keywords());
        auditData.put("narrativeProvider", narrative.provider());
        if (sourceProduct != null && sourceProduct.rawData() != null && !sourceProduct.rawData().isEmpty()) {
            auditData.put("sourceRawData", sourceProduct.rawData());
        }
        return auditData;
    }

    private Optional<BigDecimal> extractDecimal(Product sourceProduct, String key) {
        if (sourceProduct == null || sourceProduct.rawData() == null) {
            return Optional.empty();
        }
        Object value = sourceProduct.rawData().get(key);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.toString()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String resolveShippingText(ProductDetailSnapshot domesticDetail) {
        if (domesticDetail == null) {
            return null;
        }
        String shippingFromRaw = extractText(domesticDetail.rawData(), "shippingText");
        if (shippingFromRaw != null && !shippingFromRaw.isBlank()) {
            return shippingFromRaw;
        }
        return extractText(domesticDetail.attributes(), "shippingText");
    }

    private String extractText(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }

    private BigDecimal parseShippingCost(String shippingText) {
        if (shippingText == null || shippingText.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String normalized = shippingText.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return scale(new BigDecimal(normalized));
    }
}
