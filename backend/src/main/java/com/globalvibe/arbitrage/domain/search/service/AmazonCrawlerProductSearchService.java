package com.globalvibe.arbitrage.domain.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.integration.crawler.PythonCrawlerClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AmazonCrawlerProductSearchService {

    private static final Pattern DECIMAL_PATTERN = Pattern.compile("(-?\\d[\\d,]*(?:\\.\\d+)?)");

    private final PythonCrawlerClient pythonCrawlerClient;
    private final ObjectMapper objectMapper;

    public AmazonCrawlerProductSearchService(PythonCrawlerClient pythonCrawlerClient, ObjectMapper objectMapper) {
        this.pythonCrawlerClient = pythonCrawlerClient;
        this.objectMapper = objectMapper;
    }

    public List<Product> search(String keyword, int page) {
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
                    rawDataOrItem(itemNode)
            ));
        }
        return products;
    }

    private Map<String, Object> buildAttributes(JsonNode itemNode) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "platform", textOrNull(itemNode, "platform"));
        putIfPresent(attributes, "imageUrl", textOrNull(itemNode, "imageUrl"));
        putIfPresent(attributes, "productUrl", textOrNull(itemNode, "productUrl"));
        putIfPresent(attributes, "rating", doubleOrNull(itemNode, "rating"));
        putIfPresent(attributes, "reviewCount", integerOrNull(itemNode, "reviewCount"));
        return attributes;
    }

    private Map<String, Object> rawDataOrItem(JsonNode itemNode) {
        JsonNode rawDataNode = itemNode.path("rawData");
        if (rawDataNode.isObject()) {
            return objectMapper.convertValue(rawDataNode, new TypeReference<>() {});
        }
        return mapOrEmpty(itemNode);
    }

    private Map<String, Object> mapOrEmpty(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
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
