package com.globalvibe.arbitrage.domain.analysis.model;

import java.time.OffsetDateTime;

public record AnalysisTask(
        String id,
        String externalItemId,
        AnalysisTaskStatus status,
        OffsetDateTime createdAt
) {
}
