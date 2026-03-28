package com.globalvibe.arbitrage.domain.report.model;

import java.util.List;

public record ReportRiskAssessment(
        int score,
        List<String> factors,
        List<String> notes
) {
}
