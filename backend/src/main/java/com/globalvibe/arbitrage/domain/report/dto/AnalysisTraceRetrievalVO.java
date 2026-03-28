package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalysisTraceRetrievalVO(
        List<String> retrievalTerms,
        String matchSource,
        Map<String, BigDecimal> scoreBreakdown,
        List<String> evidence
) {
}
