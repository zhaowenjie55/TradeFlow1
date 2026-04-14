package com.globalvibe.arbitrage.domain.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.integration.crawler.PythonCrawlerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AmazonCrawlerProductSearchService {

    private static final Pattern DECIMAL_PATTERN = Pattern.compile("(-?\\d[\\d,]*(?:\\.\\d+)?)");
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(30);
    private static final Logger log = LoggerFactory.getLogger(AmazonCrawlerProductSearchService.class);

    private final PythonCrawlerClient pythonCrawlerClient;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IntegrationGatewayProperties integrationGatewayProperties;

    public AmazonCrawlerProductSearchService(
            PythonCrawlerClient pythonCrawlerClient,
            ObjectMapper objectMapper,
            RedisTemplate<String, Object> redisTemplate,
            IntegrationGatewayProperties integrationGatewayProperties
    ) {
        this.pythonCrawlerClient = pythonCrawlerClient;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.integrationGatewayProperties = integrationGatewayProperties;
    }

    public List<Product> search(String keyword, int page) {
        String cacheKey = buildCacheKey(keyword, page);
        if (integrationGatewayProperties.getCrawler().isCacheEnabled()) {
            List<Product> cached = readFromCache(cacheKey);
            if (cached != null) {
                log.info("amazon search cache hit keyword='{}' page={}", normalizeKeyword(keyword), page);
                return cached;
            }
            log.info("amazon search cache miss keyword='{}' page={}", normalizeKeyword(keyword), page);
        }

        JsonNode root = pythonCrawlerClient.searchProducts(keyword, page);
        JsonNode itemsNode = root.path("items");
        if (!itemsNode.isArray()) {
            return List.of();
        }

        List<Product> products = new ArrayList<>();
        for (JsonNode itemNode : itemsNode) {
            if (!itemNode.isObject()) {
                continue;
            }
            String externalItemId = textOrNull(itemNode, "externalItemId");
            String title = textOrNull(itemNode, "title");
            if (externalItemId == null || title == null) {
                continue;
            }
            products.add(new Product(
                    externalItemId,
                    MarketplaceType.AMAZON,
                    title,
                    decimalOrNull(itemNode.path("price")),
                    textOrNull(itemNode, "imageUrl"),
                    textOrNull(itemNode, "productUrl"),
                    doubleOrNull(itemNode, "rating"),
                    integerOrNull(itemNode, "reviewCount"),
                    buildAttributes(itemNode),
                    buildSearchRawData(itemNode)
            ));
        }
        if (integrationGatewayProperties.getCrawler().isCacheEnabled()) {
            writeToCache(cacheKey, products);
        }
        return products;
    }

    private String buildCacheKey(String keyword, int page) {
        return "workflow:search:amazon:" + normalizeKeyword(keyword) + ":" + page;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        return keyword.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private List<Product> readFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached == null) {
                return null;
            }
            return objectMapper.convertValue(cached, new TypeReference<>() {});
        } catch (RuntimeException ex) {
            log.warn("amazon search cache read failed key={}", cacheKey, ex);
            return null;
        }
    }

    private void writeToCache(String cacheKey, List<Product> products) {
        try {
            redisTemplate.opsForValue().set(cacheKey, products, SEARCH_CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("amazon search cache write failed key={}", cacheKey, ex);
        }
    }

    private Map<String, Object> buildAttributes(JsonNode itemNode) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "platform", textOrNull(itemNode, "platform"));
        putIfPresent(attributes, "brand", textOrNull(itemNode, "brand"));
        putIfPresent(attributes, "imageUrl", textOrNull(itemNode, "imageUrl"));
        putIfPresent(attributes, "productUrl", textOrNull(itemNode, "productUrl"));
        putIfPresent(attributes, "rating", doubleOrNull(itemNode, "rating"));
        putIfPresent(attributes, "reviewCount", integerOrNull(itemNode, "reviewCount"));
        putIfPresent(attributes, "boughtLastMonth", textOrNull(itemNode, "bought_last_month"));
        putIfPresent(attributes, "badges", textListOrNull(itemNode.path("badges")));
        return attributes;
    }

    /**
     * Phase 1 only needs compact audit data for scoring/reporting.
     * Persisting the full SerpApi search payload for high-variant keywords
     * (for example sunglasses) is unnecessarily large and can stall the workflow.
     */
    private Map<String, Object> buildSearchRawData(JsonNode itemNode) {
        JsonNode rawDataNode = itemNode.path("rawData").isObject() ? itemNode.path("rawData") : itemNode;
        Map<String, Object> rawData = new LinkedHashMap<>();
        putIfPresent(rawData, "asin", textOrNull(rawDataNode, "asin"));
        putIfPresent(rawData, "brand", textOrNull(rawDataNode, "brand"));
        putIfPresent(rawData, "title", textOrNull(rawDataNode, "title"));
        putIfPresent(rawData, "priceText", textOrNull(rawDataNode, "price"));
        putIfPresent(rawData, "priceAmountUsd", decimalOrNull(rawDataNode.path("extracted_price")));
        putIfPresent(rawData, "oldPriceText", textOrNull(rawDataNode, "old_price"));
        putIfPresent(rawData, "oldPriceAmountUsd", decimalOrNull(rawDataNode.path("extracted_old_price")));
        putIfPresent(rawData, "rating", doubleOrNull(rawDataNode, "rating"));
        putIfPresent(rawData, "reviewCount", integerOrNull(rawDataNode, "reviews"));
        putIfPresent(rawData, "boughtLastMonth", textOrNull(rawDataNode, "bought_last_month"));
        putIfPresent(rawData, "badges", textListOrNull(rawDataNode.path("badges")));
        putIfPresent(rawData, "delivery", textListOrNull(rawDataNode.path("delivery")));
        putIfPresent(rawData, "linkClean", textOrNull(rawDataNode, "link_clean"));

        JsonNode variantsNode = rawDataNode.path("variants");
        if (variantsNode.isObject()) {
            JsonNode optionsNode = variantsNode.path("options");
            if (optionsNode.isArray() && !optionsNode.isEmpty()) {
                rawData.put("variantCount", optionsNode.size());
            }
            putIfPresent(rawData, "moreVariants", textOrNull(variantsNode, "more_variants"));
        }
        return rawData;
    }

    private Map<String, Object> mapOrEmpty(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value != null) {
            target.put(key, value);
        }
    }

    private List<String> textListOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            String value = item.asText();
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String value = fieldNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String value = node.asText();
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher matcher = DECIMAL_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        String normalized = matcher.group(1).replace(",", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
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
