package com.globalvibe.arbitrage.domain.product.service;

import com.globalvibe.arbitrage.config.VectorSearchProperties;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.repository.ProductEmbeddingRepository;
import com.globalvibe.arbitrage.domain.product.repository.ProductEmbeddingRepository.SemanticSearchHit;
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
            String indexedText = toIndexDocument(product);
            float[] embedding = embeddingModel.embed(indexedText).content().vector();
            verifyDimension(embedding);
            productEmbeddingRepository.upsert(
                    platform,
                    product,
                    embedding,
                    indexedText,
                    buildMetadata(product)
            );
        }
    }

    public List<Product> search(String keyword, int limit) {
        return searchWithScores(keyword, limit).stream()
                .map(VectorSearchResult::product)
                .toList();
    }

    public List<VectorSearchResult> searchWithScores(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        float[] queryVector = embeddingModel.embed(toQueryText(keyword)).content().vector();
        verifyDimension(queryVector);
        List<SemanticSearchHit> vectorMatches = productEmbeddingRepository.semanticSearch(
                domesticPlatform(),
                queryVector,
                Math.max(limit, properties.getMaxResults()),
                properties.getMinScore()
        );
        Map<String, VectorSearchResult> deduplicated = new LinkedHashMap<>();
        vectorMatches.forEach(hit -> deduplicated.put(
                hit.product().id(),
                new VectorSearchResult(hit.product(), hit.score())
        ));
        return deduplicated.values().stream().limit(limit).toList();
    }

    private MarketplaceType domesticPlatform() {
        return MarketplaceType.fromValue(properties.getDomesticPlatform());
    }

    private String toIndexDocument(Product product) {
        String attributeTokens = product.attributes().entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(" "));
        return String.join(" ",
                safeText(product.title()),
                safeText(properties.getFixedKeyword()),
                safeText(attributeTokens)
        ).trim();
    }

    private String toQueryText(String keyword) {
        return String.join(" ",
                safeText(keyword),
                safeText(properties.getFixedKeyword())
        ).trim();
    }

    private Map<String, Object> buildMetadata(Product product) {
        return Map.of(
                "platform", product.platform().value(),
                "productId", product.id(),
                "title", product.title(),
                "fixedKeyword", properties.getFixedKeyword()
        );
    }

    private void verifyDimension(float[] vector) {
        if (vector == null || vector.length != properties.getDimension()) {
            throw new IllegalStateException("Embedding dimension mismatch, expected " + properties.getDimension());
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    public record VectorSearchResult(
            Product product,
            double score
    ) {
    }
}
