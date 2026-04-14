package com.globalvibe.arbitrage.domain.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.system.dto.RuntimeCacheStatusVO;
import com.globalvibe.arbitrage.domain.system.dto.RuntimeDomesticSessionStatusVO;
import com.globalvibe.arbitrage.domain.system.dto.RuntimeIntegrationStatusResponse;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.OffsetDateTime;

@Service
public class RuntimeIntegrationStatusService {

    private final RedisConnectionFactory redisConnectionFactory;
    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final RestClient restClient;

    public RuntimeIntegrationStatusService(
            RedisConnectionFactory redisConnectionFactory,
            IntegrationGatewayProperties integrationGatewayProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.integrationGatewayProperties = integrationGatewayProperties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(5_000);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public RuntimeIntegrationStatusResponse getStatus() {
        return new RuntimeIntegrationStatusResponse(
                OffsetDateTime.now(),
                resolveRedisStatus(),
                resolveDomesticSessionStatus()
        );
    }

    private RuntimeCacheStatusVO resolveRedisStatus() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            boolean reachable = pong != null && !pong.isBlank();
            return new RuntimeCacheStatusVO(true, reachable, reachable ? "UP" : "DEGRADED", reachable ? "Redis ping succeeded." : "Redis ping returned empty response.");
        } catch (Exception ex) {
            return new RuntimeCacheStatusVO(true, false, "DEGRADED", ex.getMessage());
        }
    }

    private RuntimeDomesticSessionStatusVO resolveDomesticSessionStatus() {
        IntegrationGatewayProperties.DomesticProperties domestic = integrationGatewayProperties.getDomestic();
        String statusEndpoint = deriveDomesticSessionStatusEndpoint(domestic.getSearchEndpoint());
        if (!domestic.isEnabled() || statusEndpoint == null) {
            return new RuntimeDomesticSessionStatusVO(
                    false,
                    false,
                    false,
                    null,
                    false,
                    null,
                    0,
                    null,
                    0,
                    "Domestic gateway is disabled or not configured."
            );
        }

        try {
            JsonNode response = restClient.get()
                    .uri(statusEndpoint)
                    .retrieve()
                    .body(JsonNode.class);

            return new RuntimeDomesticSessionStatusVO(
                    true,
                    true,
                    response.path("active").asBoolean(false),
                    textOrNull(response, "mode"),
                    response.path("verificationRequired").asBoolean(false),
                    textOrNull(response, "lastVerificationUrl"),
                    response.path("cooldownRemainingSeconds").asInt(0),
                    textOrNull(response, "currentUrl"),
                    response.path("idleTtlSeconds").asInt(0),
                    "Domestic session status fetched successfully."
            );
        } catch (Exception ex) {
            return new RuntimeDomesticSessionStatusVO(
                    true,
                    false,
                    false,
                    null,
                    false,
                    null,
                    0,
                    null,
                    0,
                    ex.getMessage()
            );
        }
    }

    private String deriveDomesticSessionStatusEndpoint(String searchEndpoint) {
        if (searchEndpoint == null || searchEndpoint.isBlank()) {
            return null;
        }
        try {
            URI searchUri = URI.create(searchEndpoint);
            String path = searchUri.getPath();
            if (path == null || !path.endsWith("/search")) {
                return null;
            }
            String sessionPath = path.substring(0, path.length() - "/search".length()) + "/session-status";
            return new URI(
                    searchUri.getScheme(),
                    searchUri.getUserInfo(),
                    searchUri.getHost(),
                    searchUri.getPort(),
                    sessionPath,
                    null,
                    null
            ).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
