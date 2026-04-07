package com.globalvibe.arbitrage.domain.search.dto;

/**
 * Typed product view returned by the backend search API.
 */
public record ProductItem(
        String platform,
        String externalItemId,
        String title,
        String price,
        String imageUrl,
        String productUrl,
        Double rating,
        Integer reviewCount
) {
}
