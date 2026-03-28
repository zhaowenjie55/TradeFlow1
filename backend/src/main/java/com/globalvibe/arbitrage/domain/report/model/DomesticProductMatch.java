package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;

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
        String reason
) {
}
