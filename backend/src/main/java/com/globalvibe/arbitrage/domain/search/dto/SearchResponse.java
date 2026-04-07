package com.globalvibe.arbitrage.domain.search.dto;

import java.util.List;

/**
 * Response envelope for the current Amazon search module.
 */
public record SearchResponse(
        boolean success,
        String keyword,
        Integer page,
        Integer count,
        List<ProductItem> items
) {
}
