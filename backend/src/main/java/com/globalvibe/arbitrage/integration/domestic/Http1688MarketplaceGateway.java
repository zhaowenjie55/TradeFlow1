package com.globalvibe.arbitrage.integration.domestic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.common.retry.RetryExecutor;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.integration.VerificationRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class Http1688MarketplaceGateway {

    private static final Logger log = LoggerFactory.getLogger(Http1688MarketplaceGateway.class);

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final ObjectMapper objectMapper;
    private final RetryExecutor retryExecutor;

    public Http1688MarketplaceGateway(
            RestClient.Builder restClientBuilder,
            IntegrationGatewayProperties integrationGatewayProperties,
            ObjectMapper objectMapper
    ) {
        IntegrationGatewayProperties.DomesticProperties domesticProperties = integrationGatewayProperties.getDomestic();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(domesticProperties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(domesticProperties.getReadTimeoutMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.objectMapper = objectMapper;
        this.retryExecutor = new RetryExecutor(
                domesticProperties.getMaxRetries(),
                domesticProperties.getRetryInitialBackoffMillis(),
                log
        );
    }

    public List<Product> searchProducts(String keyword) {
        String endpoint = integrationGatewayProperties.getDomestic().getSearchEndpoint();
        if (!integrationGatewayProperties.getDomestic().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Domestic search endpoint is not configured.");
        }

        JsonNode root = retryExecutor.execute(() -> executeOrThrowVerification(() -> restClient.post()
                .uri(endpoint)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getDomestic().getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "platform", "1688",
                        "keyword", keyword,
                        "page", 1
                ))
                .retrieve()
                .body(JsonNode.class)), "domestic-search");

        return parseKeywordProducts(root);
    }

    public Optional<ProductDetailSnapshot> loadDetail(String productId) {
        String endpoint = integrationGatewayProperties.getDomestic().getDetailEndpoint();
        if (!integrationGatewayProperties.getDomestic().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Domestic detail endpoint is not configured.");
        }

        JsonNode root = retryExecutor.execute(() -> executeOrThrowVerification(() -> restClient.post()
                .uri(endpoint)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getDomestic().getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "platform", "1688",
                        "externalItemId", productId
                ))
                .retrieve()
                .body(JsonNode.class)), "domestic-detail");

        return parseDetail(root, productId);
    }

    private List<Product> parseKeywordProducts(JsonNode root) {
        JsonNode items = root == null ? null : root.path("items");
        if (items == null || items.isMissingNode() || !items.isArray()) {
            if (root != null && root.isArray()) {
                items = root;
            } else {
                return List.of();
            }
        }

        List<Product> products = new ArrayList<>();
        for (JsonNode item : items) {
            String productUrl = textOrNull(item.path("productUrl"));
            String title = textOrNull(item.path("title"));
            if (productUrl == null || title == null) {
                continue;
            }
            String externalItemId = textOrNull(item.path("externalItemId"));
            products.add(new Product(
                    externalItemId != null ? externalItemId : syntheticProductId(productUrl, title),
                    MarketplaceType.ALIBABA_1688,
                    title,
                    decimal(item.path("price")),
                    normalizeImage(textOrNull(item.path("imageUrl"))),
                    productUrl,
                    null,
                    null,
                    buildAttributes(item),
                    toMap(item.path("rawData"))
            ));
        }
        return products;
    }

    private Optional<ProductDetailSnapshot> parseDetail(JsonNode root, String productId) {
        JsonNode item = root;
        if (item == null || item.isMissingNode() || item.isNull()) {
            return Optional.empty();
        }
        String externalItemId = textOrNull(item.path("externalItemId"));
        if (externalItemId != null && !productId.equals(externalItemId)) {
            return Optional.empty();
        }

        String title = textOrNull(item.path("title"));
        if (title == null) {
            return Optional.empty();
        }
        Map<String, Object> attributes = buildDetailAttributes(item.path("attributes"), item);
        List<String> gallery = stringList(item.path("images"));
        String primaryImage = textOrNull(item.path("imageUrl"));
        if (primaryImage == null && !gallery.isEmpty()) {
            primaryImage = gallery.get(0);
        }
        String brand = textOrNull(item.path("brand"));
        if (brand == null) {
            brand = attributeText(attributes, "品牌", "品牌名", "brand");
        }
        String description = textOrNull(item.path("description"));
        if (description == null) {
            description = title;
        }

        return Optional.of(new ProductDetailSnapshot(
                productId,
                MarketplaceType.ALIBABA_1688,
                title,
                decimal(item.path("price")),
                brand,
                normalizeImage(primaryImage),
                textOrNull(item.path("productUrl")),
                description,
                gallery,
                attributes,
                toMap(item.path("skuData")),
                toMap(item.path("rawData"))
        ));
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        String value = node.asText();
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            log.warn("domestic gateway: malformed price value '{}', defaulting to ZERO", value);
            return BigDecimal.ZERO;
        }
    }

    private String normalizeImage(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.startsWith("//") ? "https:" + url : url;
    }

    private Map<String, Object> toFlatMap(JsonNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (node == null || !node.isObject()) {
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            if (value.isValueNode()) {
                result.put(field.getKey(), value.asText());
            }
        }
        return result;
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = normalizeImage(textOrNull(item));
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private Map<String, Object> buildAttributes(JsonNode item) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        putIfPresent(attributes, "currency", textOrNull(item.path("currency")));
        putIfPresent(attributes, "shopName", textOrNull(item.path("shopName")));
        putIfPresent(attributes, "salesText", textOrNull(item.path("salesText")));
        return attributes;
    }

    private Map<String, Object> buildDetailAttributes(JsonNode attributeNode, JsonNode item) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        Map<String, Object> rawAttributes = toMap(attributeNode);
        attributes.putAll(rawAttributes);
        putIfPresent(attributes, "currency", textOrNull(item.path("currency")));
        putIfPresent(attributes, "shopName", textOrNull(item.path("shopName")));
        putIfPresent(attributes, "salesText", textOrNull(item.path("salesText")));
        putIfPresent(attributes, "shippingText", textOrNull(item.path("shippingText")));
        return attributes;
    }

    private String attributeText(Map<String, Object> attributes, String... keys) {
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private void putIfPresent(Map<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private JsonNode executeOrThrowVerification(Supplier<JsonNode> action) {
        try {
            return action.get();
        } catch (RestClientResponseException ex) {
            VerificationRequiredException verification = resolveVerificationRequired(ex);
            if (verification != null) {
                throw verification;
            }
            throw ex;
        }
    }

    private VerificationRequiredException resolveVerificationRequired(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        if (statusCode != 409 && statusCode != 423) {
            return null;
        }
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return new VerificationRequiredException("1688 需要完成人工验证，请在浏览器窗口完成登录或滑块验证后继续。", ex);
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode detail = root.path("detail");
            if (detail.isObject() && "VERIFICATION_REQUIRED".equalsIgnoreCase(detail.path("code").asText())) {
                String message = textOrNull(detail.path("message"));
                return new VerificationRequiredException(
                        message != null ? message : "1688 需要完成人工验证，请在浏览器窗口完成登录或滑块验证后继续。",
                        ex
                );
            }
        } catch (Exception ignored) {
            // Fall through to plain-text matching below.
        }
        if (responseBody.contains("VERIFICATION_REQUIRED")) {
            return new VerificationRequiredException("1688 需要完成人工验证，请在浏览器窗口完成登录或滑块验证后继续。", ex);
        }
        return null;
    }

    private String syntheticProductId(String productUrl, String title) {
        String source = (productUrl == null || productUrl.isBlank()) ? title : productUrl;
        return "1688-" + UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private void applyApiKey(org.springframework.http.HttpHeaders headers, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
    }
}
