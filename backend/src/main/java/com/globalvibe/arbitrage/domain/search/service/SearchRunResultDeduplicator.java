package com.globalvibe.arbitrage.domain.search.service;

import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.search.model.SearchRunResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes phase-1 source products into a persistence-safe result list.
 * <p>
 * Search run results are unique by {@code (search_run_id, platform, external_item_id)}.
 * Upstream search providers can still emit duplicates within a single batch, so
 * we deduplicate before writing and preserve first-seen ordering.
 */
public final class SearchRunResultDeduplicator {

    private SearchRunResultDeduplicator() {
    }

    public static List<SearchRunResult> fromProducts(String searchRunId, List<Product> products) {
        Map<String, Product> deduplicated = new LinkedHashMap<>();
        for (Product product : products) {
            if (product == null || product.platform() == null) {
                continue;
            }
            String identifier = resolveIdentifier(product);
            if (identifier == null) {
                continue;
            }
            deduplicated.putIfAbsent(buildKey(product.platform().value(), identifier), product);
        }

        int[] rank = {1};
        return deduplicated.values().stream()
                .map(product -> SearchRunResult.builder()
                        .searchRunId(searchRunId)
                        .platform(product.platform().value())
                        .externalItemId(resolveIdentifier(product))
                        .rankNo(rank[0]++)
                        .title(product.title())
                        .price(product.price())
                        .image(product.image())
                        .link(product.link())
                        .rawData(product.rawData())
                        .build())
                .toList();
    }

    private static String resolveIdentifier(Product product) {
        if (hasText(product.id())) {
            return product.id().trim();
        }
        if (hasText(product.link())) {
            return normalizeLink(product.link());
        }
        if (hasText(product.title())) {
            return "title:" + product.title().trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static String buildKey(String platform, String identifier) {
        return platform.trim().toLowerCase(Locale.ROOT) + "|" + identifier;
    }

    private static String normalizeLink(String link) {
        return link.trim().replaceAll("\\s+", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
