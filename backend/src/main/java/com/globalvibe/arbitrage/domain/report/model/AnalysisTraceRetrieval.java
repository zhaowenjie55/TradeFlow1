package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalysisTraceRetrieval(
        List<String> retrievalTerms,
        String matchSource,
        Map<String, BigDecimal> scoreBreakdown,
        List<String> evidence
) {
}
