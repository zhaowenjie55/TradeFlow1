package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;

public record DomesticProductMatchVO(
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
