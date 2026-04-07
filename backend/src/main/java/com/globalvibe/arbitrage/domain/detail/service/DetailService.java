package com.globalvibe.arbitrage.domain.detail.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.domain.detail.dto.DetailRequest;
import com.globalvibe.arbitrage.domain.detail.dto.ProductDetailResponse;
import com.globalvibe.arbitrage.integration.crawler.PythonCrawlerClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DetailService {

    private static final Duration DETAIL_CACHE_TTL = Duration.ofHours(6);

    private final PythonCrawlerClient pythonCrawlerClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public DetailService(
            PythonCrawlerClient pythonCrawlerClient,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.pythonCrawlerClient = pythonCrawlerClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public ProductDetailResponse getAmazonDetail(DetailRequest request) {
        String cacheKey = buildCacheKey(request.externalItemId());
        ProductDetailResponse cached = readFromCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        JsonNode root = pythonCrawlerClient.getProductDetail(request.externalItemId());
        ProductDetailResponse response = new ProductDetailResponse(
                textOrDefault(root, "externalItemId", request.externalItemId()),
                textOrNull(root, "title"),
                textOrNull(root, "description"),
                stringListOrEmpty(root, "features"),
                textOrNull(root, "price"),
                stringListOrEmpty(root, "images"),
                doubleOrNull(root, "rating"),
                integerOrNull(root, "reviewCount"),
                mapOrEmpty(root.path("rawData"))
        );
        writeToCache(cacheKey, response);
        return response;
    }

    private String buildCacheKey(String externalItemId) {
        return "detail:amazon:" + externalItemId;
    }

    private ProductDetailResponse readFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached instanceof ProductDetailResponse response ? response : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void writeToCache(String cacheKey, ProductDetailResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, DETAIL_CACHE_TTL);
        } catch (RuntimeException ignored) {
        }
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

    private List<String> stringListOrEmpty(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (!fieldNode.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(fieldNode, new TypeReference<>() {});
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

    private Map<String, Object> mapOrEmpty(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }
}
