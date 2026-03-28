package com.globalvibe.arbitrage.integration.domestic;

import com.fasterxml.jackson.databind.JsonNode;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HttpTaobaoMarketplaceGateway {

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;

    public HttpTaobaoMarketplaceGateway(IntegrationGatewayProperties integrationGatewayProperties) {
        this.restClient = RestClient.builder().build();
        this.integrationGatewayProperties = integrationGatewayProperties;
    }

    public List<Product> searchProducts(String keyword) {
        String endpoint = integrationGatewayProperties.getDomestic().getSearchEndpoint();
        if (!integrationGatewayProperties.getDomestic().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Domestic search endpoint is not configured.");
        }

        JsonNode root = restClient.get()
                .uri(endpoint + "?keyword={keyword}", keyword)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getDomestic().getApiKey()))
                .retrieve()
                .body(JsonNode.class);

        return parseKeywordProducts(root);
    }

    public Optional<ProductDetailSnapshot> loadDetail(String productId) {
        String endpoint = integrationGatewayProperties.getDomestic().getDetailEndpoint();
        if (!integrationGatewayProperties.getDomestic().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Domestic detail endpoint is not configured.");
        }

        JsonNode root = restClient.get()
                .uri(endpoint + "?productId={productId}", productId)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getDomestic().getApiKey()))
                .retrieve()
                .body(JsonNode.class);

        return parseDetail(root, productId);
    }

    private List<Product> parseKeywordProducts(JsonNode root) {
        JsonNode items = root == null ? null : root.path("items").path("item");
        if (items == null || items.isMissingNode() || !items.isArray()) {
            if (root != null && root.isArray()) {
                items = root;
            } else {
                return List.of();
            }
        }

        List<Product> products = new ArrayList<>();
        for (JsonNode item : items) {
            products.add(new Product(
                    item.path("num_iid").asText(),
                    MarketplaceType.TAOBAO,
                    item.path("title").asText(),
                    decimal(item.path("price")),
                    normalizeImage(item.path("pic_url").asText()),
                    item.path("detail_url").asText(),
                    null,
                    null,
                    Map.of(),
                    toFlatMap(item)
            ));
        }
        return products;
    }

    private Optional<ProductDetailSnapshot> parseDetail(JsonNode root, String productId) {
        JsonNode item = root == null ? null : root.path("item");
        if (item == null || item.isMissingNode()) {
            item = root;
        }
        if (item == null || item.isMissingNode() || !productId.equals(item.path("num_iid").asText())) {
            return Optional.empty();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        JsonNode props = item.path("props");
        if (props.isArray()) {
            for (JsonNode prop : props) {
                attributes.put(prop.path("name").asText(), prop.path("value").asText());
            }
        }

        List<String> gallery = new ArrayList<>();
        JsonNode itemImgs = item.path("item_imgs");
        if (itemImgs.isArray()) {
            for (JsonNode image : itemImgs) {
                gallery.add(normalizeImage(image.path("url").asText()));
            }
        }

        return Optional.of(new ProductDetailSnapshot(
                item.path("num_iid").asText(),
                MarketplaceType.TAOBAO,
                item.path("title").asText(),
                decimal(item.path("price")),
                item.path("brand").asText(),
                normalizeImage(item.path("pic_url").asText()),
                item.path("detail_url").asText(),
                item.path("desc_short").asText("").isBlank() ? item.path("title").asText() : item.path("desc_short").asText(),
                gallery,
                attributes,
                toFlatMap(item.path("skus")),
                toFlatMap(item)
        ));
    }

    private BigDecimal decimal(JsonNode node) {
        String value = node == null ? "" : node.asText();
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
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

    private void applyApiKey(org.springframework.http.HttpHeaders headers, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
    }
}
