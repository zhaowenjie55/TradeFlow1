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
import com.globalvibe.arbitrage.domain.report.service.ReportAssembler;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductAnalysisService {

    private static final Pattern SHIPPING_AMOUNT_PATTERN = Pattern.compile(
            "(?:运费\\s*[¥￥]?\\s*(\\d+(?:\\.\\d+)?)|[¥￥]\\s*(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*元)"
    );

    private final PricingProperties pricingProperties;
    private final ProductRepository productRepository;
    private final PricingEngine pricingEngine;
    private final MatchSelectionPolicy matchSelectionPolicy;
    private final ReportNarrativeService reportNarrativeService;
    private final ReportAssembler reportAssembler;

    public ProductAnalysisService(
            PricingProperties pricingProperties,
            ProductRepository productRepository,
            PricingEngine pricingEngine,
            MatchSelectionPolicy matchSelectionPolicy,
            ReportNarrativeService reportNarrativeService,
            ReportAssembler reportAssembler
    ) {
        this.pricingProperties = pricingProperties;
        this.productRepository = productRepository;
        this.pricingEngine = pricingEngine;
        this.matchSelectionPolicy = matchSelectionPolicy;
        this.reportNarrativeService = reportNarrativeService;
        this.reportAssembler = reportAssembler;
    }

    public ArbitrageReport buildReport(
            String reportId,
            CandidateProduct candidate,
            ProductDetailSnapshot domesticDetail,
            QueryRewrite queryRewrite,
            List<CandidateMatchRecord> domesticMatches
    ) {
        List<CandidateMatchRecord> topDomesticMatches = matchSelectionPolicy.selectTopMatches(domesticMatches, 3);
        Map<String, ProductDetailSnapshot> detailSnapshots = loadDetailSnapshots(topDomesticMatches);

        CandidateMatchRecord benchmark = topDomesticMatches.isEmpty() ? null : topDomesticMatches.get(0);
        Product sourceProduct = productRepository.findById(candidate.productId()).orElse(null);
        BigDecimal amazonPriceUsd = candidate.overseasPrice() != null
                ? scale(candidate.overseasPrice())
                : extractDecimal(sourceProduct, "priceAmountUsd").orElse(BigDecimal.ZERO);
        ShippingResolution shippingResolution = resolveShipping(domesticDetail);
        String shippingText = shippingResolution.text();
        boolean usedDefaultShipping = shippingResolution.usedDefault();
        PricingEngine.PricingBreakdown pricing = pricingEngine.calculate(
                amazonPriceUsd,
                benchmark != null ? benchmark.price() : null,
                shippingResolution.cost()
        );
        BigDecimal usdToCnyRate = pricing.usdToCnyRate();
        BigDecimal amazonPriceRmb = pricing.amazonPriceRmb();
        BigDecimal sourcingCost = pricing.sourcingCost();
        BigDecimal domesticShippingCost = pricing.domesticShippingCost();
        BigDecimal logisticsCost = pricing.logisticsCost();
        BigDecimal platformFee = pricing.platformFee();
        BigDecimal exchangeRateCost = pricing.exchangeRateCost();
        BigDecimal totalCost = pricing.totalCost();
        BigDecimal estimatedProfit = pricing.estimatedProfit();
        BigDecimal expectedMargin = pricing.expectedMargin();

        String decision = resolveDecision(expectedMargin);
        String riskLevel = resolveRiskLevel(expectedMargin);
        LLMGateway.ReportNarrativeResult narrative = reportNarrativeService.generate(
                candidate,
                queryRewrite,
                pricing,
                decision,
                riskLevel,
                benchmark,
                topDomesticMatches
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

        return reportAssembler.assemble(
                new ReportAssembler.ReportAssemblyInput(
                        reportId,
                        candidate,
                        sourceProduct,
                        queryRewrite,
                        benchmark,
                        topDomesticMatches,
                        detailSnapshots,
                        pricing,
                        decision,
                        riskLevel,
                        resolveRiskScore(expectedMargin),
                        shippingText,
                        narrative,
                        analysisTrace,
                        buildRiskNotes(domesticDetail, benchmark, shippingText, usedDefaultShipping)
                )
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
            String amount = matcher.group(1);
            if (amount == null) {
                amount = matcher.group(2);
            }
            if (amount == null) {
                amount = matcher.group(3);
            }
            BigDecimal cost = scale(new BigDecimal(amount));
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
