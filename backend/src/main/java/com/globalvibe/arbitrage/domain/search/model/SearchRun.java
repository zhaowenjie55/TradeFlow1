package com.globalvibe.arbitrage.domain.search.model;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record SearchRun(
        String searchRunId,
        String taskId,
        String phase,
        String platform,
        String queryText,
        SearchRunStatus status,
        boolean fallbackUsed,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
