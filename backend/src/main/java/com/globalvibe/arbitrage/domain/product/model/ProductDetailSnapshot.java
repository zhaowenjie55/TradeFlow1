package com.globalvibe.arbitrage.domain.product.model;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductDetailSnapshot(
        String productId,
        MarketplaceType platform,
        String title,
        BigDecimal price,
        String brand,
        String image,
        String link,
        String description,
        List<String> gallery,
        Map<String, Object> attributes,
        Map<String, Object> skuData,
        Map<String, Object> rawData
) {
}
