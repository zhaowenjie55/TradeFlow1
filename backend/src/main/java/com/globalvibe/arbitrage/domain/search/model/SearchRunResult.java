package com.globalvibe.arbitrage.domain.search.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder
public record SearchRunResult(
        String searchRunId,
        String platform,
        String externalItemId,
        int rankNo,
        String title,
        BigDecimal price,
        String image,
        String link,
        Map<String, Object> rawData
) {
}
