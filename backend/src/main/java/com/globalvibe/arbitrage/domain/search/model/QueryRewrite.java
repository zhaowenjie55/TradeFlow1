package com.globalvibe.arbitrage.domain.search.model;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record QueryRewrite(
        String rewriteId,
        String taskId,
        String candidateId,
        String sourceProductId,
        String sourceText,
        String rewrittenText,
        List<String> keywords,
        String gatewaySource,
        boolean fallbackUsed,
        String fallbackReason,
        OffsetDateTime createdAt
) {
}
