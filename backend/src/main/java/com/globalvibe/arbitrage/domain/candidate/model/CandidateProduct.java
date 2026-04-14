package com.globalvibe.arbitrage.domain.candidate.model;

import java.math.BigDecimal;

public record CandidateProduct(
        String productId,
        String title,
        String imageUrl,
        String market,
        BigDecimal overseasPrice,
        BigDecimal estimatedMargin,
        String riskTag,
        String recommendationReason,
        boolean suggestSecondPhase,
        String link
) {
}
