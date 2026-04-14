package com.globalvibe.arbitrage.domain.candidate.dto;

import java.math.BigDecimal;

public record CandidateVO(
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
