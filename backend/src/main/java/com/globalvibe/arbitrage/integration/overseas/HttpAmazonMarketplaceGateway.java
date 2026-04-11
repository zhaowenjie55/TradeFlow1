package com.globalvibe.arbitrage.integration.overseas;

import com.fasterxml.jackson.databind.JsonNode;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpAmazonMarketplaceGateway {

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;

    public HttpAmazonMarketplaceGateway(
            RestClient.Builder restClientBuilder,
            IntegrationGatewayProperties integrationGatewayProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.integrationGatewayProperties = integrationGatewayProperties;
    }

    public List<Product> searchProducts(String keyword, int limit) {
        String endpoint = integrationGatewayProperties.getOverseas().getSearchEndpoint();
        if (!integrationGatewayProperties.getOverseas().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Overseas search endpoint is not configured.");
        }

        JsonNode root = restClient.get()
                .uri(endpoint + "?keyword={keyword}&limit={limit}", keyword == null ? "" : keyword, limit)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getOverseas().getApiKey()))
                .retrieve()
                .body(JsonNode.class);

        return parseProducts(root, limit);
    }

    private List<Product> parseProducts(JsonNode root, int limit) {
        JsonNode items = root;
        if (items != null && items.path("items").isArray()) {
            items = items.path("items");
        }
        if (items == null || !items.isArray()) {
            return List.of();
        }

        List<Product> products = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode details = item.path("details");
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("salesLabel", details.path("salesLabel").asText());
            products.add(new Product(
                    item.path("asin").asText(),
                    MarketplaceType.AMAZON,
                    item.path("title").asText(),
                    parsePrice(details.path("priceRaw").asText()),
                    chooseImage(item, details),
                    item.path("amazonLink").asText(),
                    item.path("rating").isMissingNode() ? null : item.path("rating").asDouble(),
                    item.path("reviews").isMissingNode() ? null : item.path("reviews").asInt(),
                    attributes,
                    Map.of("source", "http-amazon-gateway")
            ));
        }
        return products.stream().limit(limit).toList();
    }

    private BigDecimal parsePrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = raw.replaceAll("[^0-9.]", "");
        return normalized.isBlank() ? BigDecimal.ZERO : new BigDecimal(normalized);
    }

    private String chooseImage(JsonNode item, JsonNode details) {
        String mainImage = item.path("mainImage").asText();
        if (mainImage != null && !mainImage.isBlank()) {
            return mainImage;
        }
        return details.path("hiResImage").asText();
    }

    private void applyApiKey(org.springframework.http.HttpHeaders headers, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
    }
}
