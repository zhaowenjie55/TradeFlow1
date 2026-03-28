package com.globalvibe.arbitrage.domain.report.model;

import java.time.OffsetDateTime;

public record AnalysisTraceLlm(
        String provider,
        String model,
        OffsetDateTime generatedAt
) {
}
