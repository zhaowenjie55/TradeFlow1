package com.globalvibe.arbitrage.domain.analysis.model;

import java.math.BigDecimal;

public record CandidateMatch(
        String domesticItemId,
        String title,
        BigDecimal price,
        double similarityScore,
        String platform,
        String imageUrl
) {
}
