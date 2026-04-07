package com.globalvibe.arbitrage.domain.analysis.model;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisReport(
        String taskId,
        String summary,
        BigDecimal profitEstimate,
        RiskLevel riskLevel,
        double confidenceScore,
        String reportMarkdown,
        List<String> riskExplanations
) {
}
