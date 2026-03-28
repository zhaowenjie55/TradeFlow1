package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisTracePricing(
        String currency,
        BigDecimal usdToCnyRate,
        List<String> formulaLines,
        List<String> assumptions
) {
}
