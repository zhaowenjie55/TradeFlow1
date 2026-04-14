package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.search.model.SearchRunResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchRunResultDeduplicatorTest {

    @Test
    void deduplicatesByExternalItemIdAndReassignsContiguousRank() {
        List<Product> products = List.of(
                new Product("B001", MarketplaceType.AMAZON, "First", new BigDecimal("10.00"), null, "https://a", null, null, Map.of(), Map.of()),
                new Product("B001", MarketplaceType.AMAZON, "First duplicate", new BigDecimal("11.00"), null, "https://a-dup", null, null, Map.of(), Map.of()),
                new Product("B002", MarketplaceType.AMAZON, "Second", new BigDecimal("12.00"), null, "https://b", null, null, Map.of(), Map.of())
        );

        List<SearchRunResult> results = SearchRunResultDeduplicator.fromProducts("sr-1", products);

        assertEquals(2, results.size());
        assertEquals("B001", results.get(0).externalItemId());
        assertEquals(1, results.get(0).rankNo());
        assertEquals("B002", results.get(1).externalItemId());
        assertEquals(2, results.get(1).rankNo());
        assertEquals("First", results.get(0).title());
    }

    @Test
    void fallsBackToNormalizedLinkWhenExternalItemIdIsMissing() {
        List<Product> products = List.of(
                new Product(null, MarketplaceType.AMAZON, "First", new BigDecimal("10.00"), null, " https://amazon.test/item-1 ", null, null, Map.of(), Map.of()),
                new Product(null, MarketplaceType.AMAZON, "Duplicate by link", new BigDecimal("11.00"), null, "https://amazon.test/item-1", null, null, Map.of(), Map.of()),
                new Product(null, MarketplaceType.AMAZON, "Second", new BigDecimal("12.00"), null, "https://amazon.test/item-2", null, null, Map.of(), Map.of())
        );

        List<SearchRunResult> results = SearchRunResultDeduplicator.fromProducts("sr-2", products);

        assertEquals(2, results.size());
        assertEquals("https://amazon.test/item-1", results.get(0).externalItemId());
        assertEquals(1, results.get(0).rankNo());
        assertEquals("https://amazon.test/item-2", results.get(1).externalItemId());
        assertEquals(2, results.get(1).rankNo());
    }
}
