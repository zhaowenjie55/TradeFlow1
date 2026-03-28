package com.globalvibe.arbitrage.domain.match.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CandidateMatchVO(
        String matchId,
        String taskId,
        String candidateId,
        String sourceProductId,
        String platform,
        String externalItemId,
        String title,
        BigDecimal price,
        String image,
        String link,
        BigDecimal similarityScore,
        String matchSource,
        String searchKeyword,
        boolean fallbackUsed,
        String fallbackReason,
        String reason,
        OffsetDateTime createdAt
) {
}
