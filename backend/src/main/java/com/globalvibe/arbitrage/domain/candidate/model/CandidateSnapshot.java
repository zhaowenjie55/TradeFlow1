package com.globalvibe.arbitrage.domain.candidate.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record CandidateSnapshot(
        String taskId,
        String productId,
        String title,
        String imageUrl,
        String market,
        BigDecimal overseasPrice,
        BigDecimal estimatedMargin,
        String riskTag,
        String recommendationReason,
        boolean suggestSecondPhase,
        OffsetDateTime createdAt
) {
}
