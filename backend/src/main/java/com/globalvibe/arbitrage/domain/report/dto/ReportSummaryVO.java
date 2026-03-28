package com.globalvibe.arbitrage.domain.report.dto;

import java.util.Map;

public record ReportSummaryVO(
        String insightKey,
        Map<String, Object> insightParams
) {
}
