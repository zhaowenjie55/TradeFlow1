package com.globalvibe.arbitrage.domain.report.dto;

import java.util.List;

public record ReportRiskAssessmentVO(
        Integer score,
        List<String> factors,
        List<String> notes
) {
}
