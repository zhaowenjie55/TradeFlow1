package com.globalvibe.arbitrage.domain.report.model;

import java.util.Map;

public record ReportSummary(
        String insightKey,
        Map<String, Object> insightParams
) {
}
