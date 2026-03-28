package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.ReportCostBreakdown;
import com.globalvibe.arbitrage.domain.report.model.ReportRiskAssessment;
import com.globalvibe.arbitrage.domain.report.model.ReportSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredReportMarkdownRendererTest {

    @Test
    void shouldRenderRenminbiAmountsAndDomesticShippingLine() {
        StructuredReportMarkdownRenderer renderer = new StructuredReportMarkdownRenderer();

        ArbitrageReport report = new ArbitrageReport(
                "report-1",
                "amz-1",
                "Acrylic Desktop Organizer",
                "AMAZON",
                null,
                "recommended",
                "low",
                new BigDecimal("32.10"),
                OffsetDateTime.now(),
                new ReportSummary("insights.agentNarrative", Map.of("message", "摘要")),
                new ReportCostBreakdown(
                        new BigDecimal("15.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("11.22"),
                        new BigDecimal("14.03"),
                        new BigDecimal("2.81"),
                        new BigDecimal("48.06"),
                        new BigDecimal("93.53"),
                        new BigDecimal("45.47")
                ),
                new ReportRiskAssessment(80, List.of("factor"), List.of("note")),
                List.of("建议1"),
                List.of(),
                Map.of("priceAmountUsd", new BigDecimal("12.99"))
        );

        String markdown = renderer.render(report);

        assertTrue(markdown.contains("Amazon 售价（人民币）: ¥93.53"));
        assertTrue(markdown.contains("国内运费: ¥5.00"));
        assertTrue(markdown.contains("预计利润: ¥45.47"));
    }
}
