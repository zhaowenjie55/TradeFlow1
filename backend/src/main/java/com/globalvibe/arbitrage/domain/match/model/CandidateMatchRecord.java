package com.globalvibe.arbitrage.domain.match.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record CandidateMatchRecord(
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
        List<String> retrievalTerms,
        Map<String, BigDecimal> scoreBreakdown,
        List<String> evidence,
        OffsetDateTime createdAt
) {
}
