package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.repository.ProductEmbeddingRepository;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "app.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomesticVectorSearchService {

    private final VectorSearchProperties properties;
    private final EmbeddingModel embeddingModel;
    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository productEmbeddingRepository;

    public DomesticVectorSearchService(
            VectorSearchProperties properties,
            EmbeddingModel embeddingModel,
            ProductRepository productRepository,
            ProductEmbeddingRepository productEmbeddingRepository
    ) {
        this.properties = properties;
        this.embeddingModel = embeddingModel;
        this.productRepository = productRepository;
        this.productEmbeddingRepository = productEmbeddingRepository;
    }

    public void rebuildDomesticIndex() {
        MarketplaceType platform = domesticPlatform();
        List<Product> products = productRepository.findByPlatform(platform);
        productEmbeddingRepository.deleteByPlatform(platform);
        for (Product product : products) {
            productEmbeddingRepository.upsert(
                    platform,
                    product,
                    embeddingModel.embed(toIndexDocument(product)).content().vector(),
                    toIndexDocument(product),
                    buildMetadata(product)
            );
        }
    }

    public List<Product> search(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        List<Product> vectorMatches = productEmbeddingRepository.semanticSearch(
                domesticPlatform(),
                embeddingModel.embed(toQueryText(keyword)).content().vector(),
                Math.max(limit, properties.getMaxResults()),
                properties.getMinScore()
        );
        Map<String, Product> deduplicated = new LinkedHashMap<>();
        vectorMatches.forEach(product -> deduplicated.put(product.id(), product));
        return deduplicated.values().stream().limit(limit).toList();
    }

    private MarketplaceType domesticPlatform() {
        return MarketplaceType.valueOf(properties.getDomesticPlatform());
    }

    private String toIndexDocument(Product product) {
        String attributeTokens = product.attributes().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(" "));
        return """
                商品标题：%s
                召回关键词：%s
                商品属性：%s
                """.formatted(
                product.title(),
                properties.getFixedKeyword(),
                attributeTokens
        ).trim();
    }

    private String toQueryText(String keyword) {
        return """
                为这个商品检索语句生成向量：%s
                补充关键词：%s
                """.formatted(keyword, properties.getFixedKeyword()).trim();
    }

    private Map<String, Object> buildMetadata(Product product) {
        return Map.of(
                "platform", product.platform().name(),
                "productId", product.id(),
                "title", product.title(),
                "fixedKeyword", properties.getFixedKeyword()
        );
    }
}
