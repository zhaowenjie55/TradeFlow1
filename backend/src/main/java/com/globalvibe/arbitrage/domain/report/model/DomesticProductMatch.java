package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DomesticProductMatch(
        String id,
        String platform,
        String platformProductId,
        String title,
        BigDecimal price,
        String image,
        int similarityScore,
        String detailUrl,
        String searchUrl,
        String reason,
        String matchSource,
        List<String> retrievalTerms,
        Map<String, BigDecimal> scoreBreakdown,
        List<String> evidence
) {
}
