package com.globalvibe.arbitrage.integration.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the Python FastAPI crawler service.
 * <p>
 * Java does not call SerpApi directly. This client is the only backend entry
 * point for the current crawler-based search integration.
 */
@Component
public class PythonCrawlerClient {

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;

    public PythonCrawlerClient(IntegrationGatewayProperties integrationGatewayProperties) {
        this.restClient = RestClient.builder().build();
        this.integrationGatewayProperties = integrationGatewayProperties;
    }

    /**
     * Sends a keyword search request to the configured crawler endpoint.
     * The endpoint defaults to {@code http://127.0.0.1:8001/api/search} and can
     * be overridden via {@code CRAWLER_SEARCH_ENDPOINT}.
     */
    public JsonNode searchProducts(String keyword, int page) {
        String endpoint = integrationGatewayProperties.getCrawler().getSearchEndpoint();
        if (!integrationGatewayProperties.getCrawler().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Crawler search endpoint is not configured.");
        }

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PythonCrawlerSearchRequest(keyword, page))
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Sends a product detail request to the configured crawler detail endpoint.
     */
    public JsonNode getProductDetail(String externalItemId) {
        String endpoint = integrationGatewayProperties.getCrawler().getDetailEndpoint();
        if (!integrationGatewayProperties.getCrawler().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Crawler detail endpoint is not configured.");
        }

        return restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PythonCrawlerDetailRequest(externalItemId))
                .retrieve()
                .body(JsonNode.class);
    }
}
