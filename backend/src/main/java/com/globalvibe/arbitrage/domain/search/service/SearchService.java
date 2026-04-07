package com.globalvibe.arbitrage.domain.search.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.globalvibe.arbitrage.domain.search.dto.ProductItem;
import com.globalvibe.arbitrage.domain.search.dto.SearchRequest;
import com.globalvibe.arbitrage.domain.search.dto.SearchResponse;
import com.globalvibe.arbitrage.integration.crawler.PythonCrawlerClient;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Application service for the current Amazon search flow.
 * <p>
 * Responsibilities are intentionally small:
 * call the Python crawler client, convert normalized JSON into typed DTOs,
 * and apply conservative fallbacks when optional fields are missing.
 */
@Service
public class SearchService {

    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);

    private final PythonCrawlerClient pythonCrawlerClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public SearchService(PythonCrawlerClient pythonCrawlerClient, RedisTemplate<String, Object> redisTemplate) {
        this.pythonCrawlerClient = pythonCrawlerClient;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Executes the Java -> Python -> SerpApi search chain and returns the
     * typed response used by the backend API.
     */
    public SearchResponse searchAmazon(SearchRequest request) {
        String cacheKey = buildCacheKey(request.keyword(), request.page());
        SearchResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        JsonNode root = pythonCrawlerClient.searchProducts(request.keyword(), request.page());
        List<ProductItem> items = mapItems(root.path("items"));

        SearchResponse response = new SearchResponse(
                root.path("success").asBoolean(true),
                textOrDefault(root, "keyword", request.keyword()),
                root.path("page").isNumber() ? root.path("page").asInt() : request.page(),
                root.path("count").isNumber() ? root.path("count").asInt() : items.size(),
                items
        );
        writeToCache(cacheKey, response);
        return response;
    }

    private String buildCacheKey(String keyword, int page) {
        return "search:amazon:" + keyword + ":" + page;
    }

    private SearchResponse readFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached instanceof SearchResponse response ? response : null;
        } catch (RuntimeException ex) {
            // Redis is an optimization for this phase. Requests still proceed when cache is unavailable.
            return null;
        }
    }

    private void writeToCache(String cacheKey, SearchResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, SEARCH_CACHE_TTL);
        } catch (RuntimeException ignored) {
            // Preserve current search behavior even if Redis is temporarily unavailable.
        }
    }

    private List<ProductItem> mapItems(JsonNode itemsNode) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return List.of();
        }

        List<ProductItem> items = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            // The Python layer already normalizes field names. Java only performs
            // null-safe extraction and lightweight type conversion here.
            items.add(new ProductItem(
                    textOrNull(itemNode, "platform"),
                    textOrNull(itemNode, "externalItemId"),
                    textOrNull(itemNode, "title"),
                    textOrNull(itemNode, "price"),
                    textOrNull(itemNode, "imageUrl"),
                    textOrNull(itemNode, "productUrl"),
                    doubleOrNull(itemNode, "rating"),
                    integerOrNull(itemNode, "reviewCount")
            ));
        }
        return items;
    }

    private String textOrDefault(JsonNode node, String fieldName, String fallback) {
        String value = textOrNull(node, fieldName);
        return value != null ? value : fallback;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Double doubleOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isNumber()) {
            return fieldNode.doubleValue();
        }
        String value = fieldNode.asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer integerOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        if (fieldNode.isInt() || fieldNode.isLong() || fieldNode.isIntegralNumber()) {
            return fieldNode.intValue();
        }
        String value = fieldNode.asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
