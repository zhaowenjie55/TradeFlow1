package com.globalvibe.arbitrage.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDetailVO(
        String productId,
        String platform,
        String title,
        BigDecimal price,
        String image,
        String link,
        Double rating,
        Integer reviews,
        Map<String, Object> attributes,
        String brand,
        String description,
        List<String> gallery,
        Map<String, Object> skuData,
        boolean detailLoaded
) {
}
