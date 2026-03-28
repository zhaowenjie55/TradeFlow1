package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReportDetailVO(
        String taskId,
        String reportId,
        String productId,
        String title,
        String market,
        String image,
        String decision,
        String riskLevel,
        BigDecimal expectedMargin,
        OffsetDateTime generatedAt,
        ReportSummaryVO summary,
        ReportCostBreakdownVO costBreakdown,
        ReportRiskAssessmentVO riskAssessment,
        List<String> recommendations,
        List<DomesticProductMatchVO> domesticMatches,
        ReportDownloadDocumentVO downloadDocument
) {
}
