package com.globalvibe.arbitrage.domain.report.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record ReportAggregate(
        String taskId,
        String reportId,
        BigDecimal estimatedProfit,
        BigDecimal estimatedMargin,
        ReportProvenance provenance,
        String reportMarkdown,
        ArbitrageReport report,
        OffsetDateTime createdAt
) {
}
