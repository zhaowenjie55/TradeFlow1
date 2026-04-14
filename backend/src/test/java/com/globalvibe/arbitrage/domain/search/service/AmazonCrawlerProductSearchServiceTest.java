package com.globalvibe.arbitrage.domain.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.integration.crawler.PythonCrawlerClient;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonCrawlerProductSearchServiceTest {

    @Test
    void returnsCachedProductsWithoutCallingCrawler() {
        PythonCrawlerClient pythonCrawlerClient = mock(PythonCrawlerClient.class);
        IntegrationGatewayProperties integrationGatewayProperties = new IntegrationGatewayProperties();
        integrationGatewayProperties.getCrawler().setCacheEnabled(true);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<Map<String, Object>> cached = List.of(
                Map.of(
                        "id", "B001",
                        "platform", "AMAZON",
                        "title", "Coffee",
                        "price", new BigDecimal("10.00"),
                        "image", "img",
                        "link", "link",
                        "attributes", Map.of(),
                        "rawData", Map.of()
                )
        );
        when(valueOperations.get("workflow:search:amazon:coffee:1")).thenReturn(cached);

        AmazonCrawlerProductSearchService service = new AmazonCrawlerProductSearchService(
                pythonCrawlerClient,
                new ObjectMapper(),
                redisTemplate,
                integrationGatewayProperties
        );

        List<?> result = service.search("Coffee", 1);

        assertEquals(1, result.size());
        verify(pythonCrawlerClient, never()).searchProducts("Coffee", 1);
    }

    @Test
    void cachesFreshCrawlerResultsUsingNormalizedKey() {
        PythonCrawlerClient pythonCrawlerClient = mock(PythonCrawlerClient.class);
        IntegrationGatewayProperties integrationGatewayProperties = new IntegrationGatewayProperties();
        integrationGatewayProperties.getCrawler().setCacheEnabled(true);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("workflow:search:amazon:coffee beans:1")).thenReturn(null);
        when(pythonCrawlerClient.searchProducts("  Coffee   Beans  ", 1)).thenReturn(new ObjectMapper().valueToTree(
                Map.of("items", List.of(
                        Map.of(
                                "externalItemId", "B001",
                                "title", "Coffee Beans",
                                "price", "18.99",
                                "imageUrl", "img",
                                "productUrl", "link",
                                "platform", "amazon",
                                "rawData", Map.of("a", "b")
                        )
                ))
        ));

        AmazonCrawlerProductSearchService service = new AmazonCrawlerProductSearchService(
                pythonCrawlerClient,
                new ObjectMapper(),
                redisTemplate,
                integrationGatewayProperties
        );

        var result = service.search("  Coffee   Beans  ", 1);

        assertEquals(1, result.size());
        assertEquals("B001", result.get(0).id());
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("workflow:search:amazon:coffee beans:1"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsRedisWhenCrawlerCacheDisabled() {
        PythonCrawlerClient pythonCrawlerClient = mock(PythonCrawlerClient.class);
        IntegrationGatewayProperties integrationGatewayProperties = new IntegrationGatewayProperties();
        integrationGatewayProperties.getCrawler().setCacheEnabled(false);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(pythonCrawlerClient.searchProducts("Coffee", 1)).thenReturn(new ObjectMapper().valueToTree(
                Map.of("items", List.of(
                        Map.of(
                                "externalItemId", "B001",
                                "title", "Coffee",
                                "price", "18.99",
                                "imageUrl", "img",
                                "productUrl", "link",
                                "platform", "amazon",
                                "rawData", Map.of()
                        )
                ))
        ));

        AmazonCrawlerProductSearchService service = new AmazonCrawlerProductSearchService(
                pythonCrawlerClient,
                new ObjectMapper(),
                redisTemplate,
                integrationGatewayProperties
        );

        var result = service.search("Coffee", 1);

        assertEquals(1, result.size());
        verify(redisTemplate, never()).opsForValue();
    }
}
