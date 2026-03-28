package com.globalvibe.arbitrage.domain.report.dto;

import java.time.OffsetDateTime;

public record AnalysisTraceLlmVO(
        String provider,
        String model,
        OffsetDateTime generatedAt
) {
}
