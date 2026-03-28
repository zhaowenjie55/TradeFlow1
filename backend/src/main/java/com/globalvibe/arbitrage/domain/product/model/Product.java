package com.globalvibe.arbitrage.domain.product.model;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;

import java.math.BigDecimal;
import java.util.Map;

public record Product(
        String id,
        MarketplaceType platform,
        String title,
        BigDecimal price,
        String image,
        String link,
        Double rating,
        Integer reviews,
        Map<String, Object> attributes,
        Map<String, Object> rawData
) {
}
