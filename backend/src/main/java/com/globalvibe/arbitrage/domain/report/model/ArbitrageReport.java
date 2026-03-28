package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ArbitrageReport(
        String reportId,
        String productId,
        String title,
        String market,
        String image,
        String decision,
        String riskLevel,
        BigDecimal expectedMargin,
        OffsetDateTime generatedAt,
        ReportSummary summary,
        ReportCostBreakdown costBreakdown,
        ReportRiskAssessment riskAssessment,
        List<String> recommendations,
        List<DomesticProductMatch> domesticMatches,
        Map<String, Object> auditData
) {
}
