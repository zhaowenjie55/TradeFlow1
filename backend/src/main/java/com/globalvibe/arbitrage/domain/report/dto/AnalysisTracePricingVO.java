package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisTracePricingVO(
        String currency,
        BigDecimal usdToCnyRate,
        List<String> formulaLines,
        List<String> assumptions
) {
}
