package com.globalvibe.arbitrage.domain.analysis.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AnalysisResultVO(
        String taskId,
        String reportId,
        String sourceProductId,
        String sourceTitle,
        String benchmarkProductId,
        String benchmarkProductTitle,
        String benchmarkPlatform,
        BigDecimal benchmarkPrice,
        BigDecimal expectedMargin,
        Integer matchScore,
        String decision,
        String riskLevel,
        String summary,
        OffsetDateTime createdAt
) {
}
