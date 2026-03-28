package com.globalvibe.arbitrage.domain.candidate.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CandidateSnapshotVO(
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
