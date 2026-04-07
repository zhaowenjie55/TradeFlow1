package com.globalvibe.arbitrage.domain.detail.dto;

import java.util.List;
import java.util.Map;

public record ProductDetailResponse(
        String externalItemId,
        String title,
        String description,
        List<String> features,
        String price,
        List<String> images,
        Double rating,
        Integer reviewCount,
        Map<String, Object> rawData
) {
}
