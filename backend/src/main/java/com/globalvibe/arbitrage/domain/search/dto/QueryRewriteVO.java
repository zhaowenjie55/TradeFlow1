package com.globalvibe.arbitrage.domain.search.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record QueryRewriteVO(
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
