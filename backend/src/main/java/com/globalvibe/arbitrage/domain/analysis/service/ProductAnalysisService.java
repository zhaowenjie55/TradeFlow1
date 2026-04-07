package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.config.PricingProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTrace;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTraceLlm;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTracePricing;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTraceRetrieval;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTraceRewrite;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductAnalysisService {

    private static final Pattern SHIPPING_AMOUNT_PATTERN = Pattern.compile("(?:运费\\s*[¥￥]?|[¥￥])\\s*(\\d+(?:\\.\\d+)?)");

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
        Map<String, ProductDetailSnapshot> detailSnapshots = loadDetailSnapshots(topDomesticMatches);

        CandidateMatchRecord benchmark = topDomesticMatches.isEmpty() ? null : topDomesticMatches.get(0);
        Product sourceProduct = productRepository.findById(candidate.productId()).orElse(null);
        BigDecimal amazonPriceUsd = candidate.overseasPrice() != null
                ? scale(candidate.overseasPrice())
                : extractDecimal(sourceProduct, "priceAmountUsd").orElse(BigDecimal.ZERO);
        BigDecimal usdToCnyRate = pricingProperties.getUsdToCnyRate();
        BigDecimal amazonPriceRmb = scale(amazonPriceUsd.multiply(usdToCnyRate));
        BigDecimal sourcingCost = benchmark != null && benchmark.price() != null
                ? scale(benchmark.price())
                : scale(amazonPriceRmb.multiply(pricingProperties.getFallbackSourcingRate()));
        ShippingResolution shippingResolution = resolveShipping(domesticDetail);
        String shippingText = shippingResolution.text();
        boolean usedDefaultShipping = shippingResolution.usedDefault();
        BigDecimal domesticShippingCost = shippingResolution.cost();
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

        String benchmarkTitle = benchmark != null ? benchmark.title() : "暂无稳定 1688 对标货源";
        LLMGateway.ReportNarrativeResult narrative = llmGateway.generateReportNarrative(
                new LLMGateway.ReportNarrativeRequest(
                        candidate.title(),
                        candidate.market(),
                        queryRewrite.rewrittenText(),
                        queryRewrite.keywords(),
                        resolveDecision(expectedMargin),
                        resolveRiskLevel(expectedMargin),
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

        AnalysisTrace analysisTrace = buildAnalysisTrace(
                candidate,
                queryRewrite,
                benchmark,
                narrative,
                shippingText,
                usedDefaultShipping,
                usdToCnyRate,
                amazonPriceUsd,
                amazonPriceRmb,
                sourcingCost,
                domesticShippingCost,
                logisticsCost,
                platformFee,
                exchangeRateCost,
                totalCost,
                estimatedProfit,
                expectedMargin
        );

        String decision = resolveDecision(expectedMargin);
        String riskLevel = resolveRiskLevel(expectedMargin);

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
                        buildRiskNotes(domesticDetail, benchmark, shippingText, usedDefaultShipping)
                ),
                buildRecommendations(narrative),
                topDomesticMatches.stream().map(item -> {
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
                }).toList(),
                analysisTrace,
                buildAuditData(sourceProduct, queryRewrite, amazonPriceUsd, amazonPriceRmb, shippingText, narrative, analysisTrace)
        );
    }

    private Map<String, ProductDetailSnapshot> loadDetailSnapshots(List<CandidateMatchRecord> matches) {
        Map<String, ProductDetailSnapshot> snapshots = new HashMap<>();
        for (CandidateMatchRecord match : matches) {
            if (match.externalItemId() == null || match.externalItemId().isBlank()) {
                continue;
            }
            productRepository.findDetailByProductId(match.externalItemId())
                    .ifPresent(snapshot -> snapshots.put(match.externalItemId(), snapshot));
        }
        return snapshots;
    }

    private AnalysisTrace buildAnalysisTrace(
            CandidateProduct candidate,
            QueryRewrite queryRewrite,
            CandidateMatchRecord benchmark,
            LLMGateway.ReportNarrativeResult narrative,
            String shippingText,
            boolean usedDefaultShipping,
            BigDecimal usdToCnyRate,
            BigDecimal amazonPriceUsd,
            BigDecimal amazonPriceRmb,
            BigDecimal sourcingCost,
            BigDecimal domesticShippingCost,
            BigDecimal logisticsCost,
            BigDecimal platformFee,
            BigDecimal exchangeRateCost,
            BigDecimal totalCost,
            BigDecimal estimatedProfit,
            BigDecimal expectedMargin
    ) {
        AnalysisTraceRewrite rewrite = new AnalysisTraceRewrite(
                candidate.title(),
                queryRewrite.rewrittenText(),
                queryRewrite.keywords(),
                queryRewrite.gatewaySource()
        );
        AnalysisTraceRetrieval retrieval = new AnalysisTraceRetrieval(
                benchmark != null && benchmark.retrievalTerms() != null ? benchmark.retrievalTerms() : List.of(queryRewrite.rewrittenText()),
                benchmark != null ? benchmark.matchSource() : "CATALOG_TEXT",
                benchmark != null && benchmark.scoreBreakdown() != null ? benchmark.scoreBreakdown() : Map.of(),
                benchmark != null && benchmark.evidence() != null ? benchmark.evidence() : List.of("商品库未返回额外检索证据。")
        );
        AnalysisTracePricing pricing = new AnalysisTracePricing(
                "CNY",
                usdToCnyRate,
                List.of(
                        "Amazon 售价(CNY) = Amazon 售价(USD) " + lineValue(amazonPriceUsd) + " x 汇率 " + lineValue(usdToCnyRate) + " = " + lineValue(amazonPriceRmb),
                        "总成本 = 采购成本 " + lineValue(sourcingCost) + " + 国内运费 " + lineValue(domesticShippingCost) + " + 跨境物流 " + lineValue(logisticsCost) + " + 平台费 " + lineValue(platformFee) + " + 汇损 " + lineValue(exchangeRateCost) + " = " + lineValue(totalCost),
                        "预计利润 = Amazon 售价(CNY) " + lineValue(amazonPriceRmb) + " - 总成本 " + lineValue(totalCost) + " = " + lineValue(estimatedProfit),
                        "预计利润率 = 预计利润 " + lineValue(estimatedProfit) + " / Amazon 售价(CNY) " + lineValue(amazonPriceRmb) + " = " + lineValue(expectedMargin) + "%"
                ),
                List.of(
                        "跨境物流费率: " + lineValue(pricingProperties.getCrossBorderLogisticsRate().multiply(new BigDecimal("100"))) + "%",
                        "平台费率: " + lineValue(pricingProperties.getPlatformFeeRate().multiply(new BigDecimal("100"))) + "%",
                        "汇损费率: " + lineValue(pricingProperties.getExchangeLossRate().multiply(new BigDecimal("100"))) + "%",
                        usedDefaultShipping
                                ? "国内运费未从商品详情提取，使用默认值 ¥" + lineValue(pricingProperties.getDefaultDomesticShippingCost())
                                : "国内运费来自商品详情字段: " + shippingText
                )
        );
        AnalysisTraceLlm llm = new AnalysisTraceLlm(
                narrative.provider(),
                narrative.model(),
                narrative.generatedAt()
        );
        return new AnalysisTrace(rewrite, retrieval, pricing, llm);
    }

    private List<String> buildRiskNotes(
            ProductDetailSnapshot domesticDetail,
            CandidateMatchRecord benchmark,
            String shippingText,
            boolean usedDefaultShipping
    ) {
        List<String> notes = new ArrayList<>();
        notes.add(domesticDetail != null
                ? "已纳入商品详情快照中的品牌、属性与 SKU 信息。"
                : "当前未命中商品详情快照，品牌和属性信息仍建议人工复核。");
        notes.add(benchmark != null
                ? "已找到可解释价差的 1688 对标货源。"
                : "暂无高置信对标货源，建议将本次结果作为初筛样本。");
        notes.add(usedDefaultShipping
                ? "国内运费采用配置默认值，建议补录真实运费后再确认利润。"
                : "国内运费已从商品详情解析: " + shippingText);
        return notes;
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

    private String resolveDecision(BigDecimal margin) {
        if (margin.compareTo(BigDecimal.valueOf(18)) >= 0) {
            return "recommended";
        }
        if (margin.compareTo(BigDecimal.valueOf(8)) >= 0) {
            return "cautious";
        }
        return "not_recommended";
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
            BigDecimal amazonPriceUsd,
            BigDecimal amazonPriceRmb,
            String shippingText,
            LLMGateway.ReportNarrativeResult narrative,
            AnalysisTrace analysisTrace
    ) {
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("priceAmountUsd", amazonPriceUsd);
        auditData.put("usdToCnyRate", pricingProperties.getUsdToCnyRate());
        auditData.put("amazonPriceRmb", amazonPriceRmb);
        auditData.put("shippingText", shippingText);
        auditData.put("rewrittenText", queryRewrite.rewrittenText());
        auditData.put("rewrittenKeywords", queryRewrite.keywords());
        auditData.put("rewriteProvider", queryRewrite.gatewaySource());
        auditData.put("rewriteModel", queryRewrite.gatewayModel());
        auditData.put("narrativeProvider", narrative.provider());
        auditData.put("narrativeModel", narrative.model());
        auditData.put("analysisTrace", analysisTrace);
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

    private ShippingResolution resolveShipping(ProductDetailSnapshot domesticDetail) {
        String shippingText = resolveShippingText(domesticDetail);
        if (shippingText == null || shippingText.isBlank()) {
            return new ShippingResolution(
                    null,
                    scale(pricingProperties.getDefaultDomesticShippingCost()),
                    true
            );
        }
        String normalizedText = shippingText.trim();
        if (normalizedText.contains("包邮")) {
            return new ShippingResolution(normalizedText, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), false);
        }

        Matcher matcher = SHIPPING_AMOUNT_PATTERN.matcher(normalizedText);
        if (matcher.find()) {
            BigDecimal cost = scale(new BigDecimal(matcher.group(1)));
            boolean suspiciousZero = BigDecimal.ZERO.compareTo(cost) == 0
                    && !normalizedText.contains("包邮")
                    && !normalizedText.contains("免运费");
            if (!suspiciousZero) {
                return new ShippingResolution(normalizedText, cost, false);
            }
        }

        return new ShippingResolution(
                normalizedText,
                scale(pricingProperties.getDefaultDomesticShippingCost()),
                true
        );
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String lineValue(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private record ShippingResolution(String text, BigDecimal cost, boolean usedDefault) {
    }
}
