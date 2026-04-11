package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReportListItemResponse(
        String taskId,
        String reportId,
        String productId,
        String title,
        String decision,
        BigDecimal margin,
        String riskLevel,
        String qualityTier,
        boolean fallbackUsed,
        String retrievalSource,
        String detailSource,
        OffsetDateTime generatedAt
) {
}
