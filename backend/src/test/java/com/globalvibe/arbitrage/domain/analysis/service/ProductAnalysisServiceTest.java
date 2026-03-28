package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.config.PricingProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductAnalysisServiceTest {

    @Test
    void shouldCalculateAllCostsInCnyAndKeepAuditFields() {
        LLMGateway llmGateway = mock(LLMGateway.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        PricingProperties pricingProperties = new PricingProperties();
        ProductAnalysisService service = new ProductAnalysisService(llmGateway, pricingProperties, productRepository);

        when(llmGateway.generateReportNarrative(any())).thenReturn(new LLMGateway.ReportNarrativeResult(
                "摘要",
                List.of("建议1"),
                List.of("风险1"),
                false,
                "GLM_CHAT",
                "glm-5",
                null,
                OffsetDateTime.now()
        ));
        when(productRepository.findById("amz-acrylic-01")).thenReturn(java.util.Optional.of(new Product(
                "amz-acrylic-01",
                MarketplaceType.AMAZON,
                "Acrylic Desktop Organizer",
                new BigDecimal("12.99"),
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of("priceAmountUsd", "12.99")
        )));

        CandidateProduct candidate = new CandidateProduct(
                "amz-acrylic-01",
                "Acrylic Desktop Organizer",
                null,
                "AMAZON",
                new BigDecimal("12.99"),
                new BigDecimal("25.0"),
                "低风险",
                "reason",
                true
        );
        QueryRewrite queryRewrite = QueryRewrite.builder()
                .rewriteId("rewrite-1")
                .rewrittenText("亚克力透明收纳架")
                .keywords(List.of("亚克力透明收纳架", "亚克力桌面收纳架"))
                .gatewaySource("GLM_CHAT")
                .gatewayModel("glm-5")
                .build();
        CandidateMatchRecord match = CandidateMatchRecord.builder()
                .matchId("match-1")
                .taskId("phase2-1")
                .candidateId("phase1-1:amz-acrylic-01")
                .sourceProductId("amz-acrylic-01")
                .platform("1688")
                .externalItemId("cn-1688-acrylic-01")
                .title("亚克力透明收纳架")
                .price(new BigDecimal("15.00"))
                .similarityScore(new BigDecimal("88.00"))
                .reason("reason")
                .matchSource("CATALOG_HYBRID")
                .retrievalTerms(List.of("亚克力透明收纳架", "亚克力桌面收纳架"))
                .scoreBreakdown(Map.of("titleOverlap", new BigDecimal("32.00")))
                .evidence(List.of("检索词: 亚克力透明收纳架"))
                .createdAt(OffsetDateTime.now())
                .build();
        ProductDetailSnapshot detailSnapshot = new ProductDetailSnapshot(
                "cn-1688-acrylic-01",
                MarketplaceType.ALIBABA_1688,
                "亚克力透明收纳架",
                new BigDecimal("15.00"),
                "HBlife",
                null,
                null,
                null,
                List.of(),
                Map.of("shippingText", "5元"),
                Map.of(),
                Map.of("shippingText", "5元")
        );

        var report = service.buildReport("report-1", candidate, detailSnapshot, queryRewrite, List.of(match));

        assertEquals(new BigDecimal("93.53"), report.costBreakdown().targetSellingPrice());
        assertEquals(new BigDecimal("5.00"), report.costBreakdown().domesticShippingCost());
        assertEquals(new BigDecimal("11.22"), report.costBreakdown().logisticsCost());
        assertEquals(new BigDecimal("14.03"), report.costBreakdown().platformFee());
        assertEquals(new BigDecimal("2.81"), report.costBreakdown().exchangeRateCost());
        assertEquals(new BigDecimal("48.06"), report.costBreakdown().totalCost());
        assertEquals(new BigDecimal("45.47"), report.costBreakdown().estimatedProfit());
        assertEquals(new BigDecimal("48.62"), report.expectedMargin());
        assertEquals(new BigDecimal("12.99"), (BigDecimal) report.auditData().get("priceAmountUsd"));
        assertEquals("5元", report.auditData().get("shippingText"));
        assertEquals("glm-5", report.analysisTrace().llm().model());
    }
}
