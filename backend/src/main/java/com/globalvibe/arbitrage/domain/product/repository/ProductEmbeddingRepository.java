package com.globalvibe.arbitrage.domain.product.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class ProductEmbeddingRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new MapTypeReference();

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public ProductEmbeddingRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    public void deleteByPlatform(MarketplaceType platform) {
        jdbcTemplate.update(
                "DELETE FROM gv_product_embedding WHERE " + platformPredicate("platform", platform),
                platform.databaseValues().toArray()
        );
    }

    public void upsert(MarketplaceType platform, Product product, float[] embedding, String indexedText, Map<String, Object> metadata) {
        jdbcTemplate.update("""
                        INSERT INTO gv_product_embedding (
                            embedding_id, platform, external_item_id, indexed_text, embedding, metadata_jsonb, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, CAST(? AS vector), ?, NOW(), NOW())
                        ON CONFLICT (platform, external_item_id) DO UPDATE SET
                            embedding_id = EXCLUDED.embedding_id,
                            indexed_text = EXCLUDED.indexed_text,
                            embedding = EXCLUDED.embedding,
                            metadata_jsonb = EXCLUDED.metadata_jsonb,
                            updated_at = NOW()
                """,
                buildEmbeddingId(platform, product.id()),
                platform.value(),
                product.id(),
                indexedText,
                toVectorLiteral(embedding),
                jdbcJsonSupport.toJsonb(metadata)
        );
    }

    public List<SemanticSearchHit> semanticSearch(MarketplaceType platform, float[] queryVector, int limit, double minScore) {
        String vectorLiteral = toVectorLiteral(queryVector);
        return jdbcTemplate.query("""
                        SELECT p.platform,
                               p.external_item_id AS product_id,
                               p.title,
                               p.price,
                               p.image,
                               p.link,
                               p.rating,
                               p.reviews,
                               (1 - (e.embedding <=> CAST(? AS vector))) AS vector_score,
                               COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_product_embedding e
                        JOIN gv_platform_item p
                          ON p.platform = e.platform
                         AND p.external_item_id = e.external_item_id
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE %s
                          AND (1 - (e.embedding <=> CAST(? AS vector))) >= ?
                        ORDER BY e.embedding <=> CAST(? AS vector), p.updated_at DESC
                        LIMIT ?
                        """.formatted(platformPredicate("e.platform", platform)),
                (rs, rowNum) -> new SemanticSearchHit(
                        new Product(
                                rs.getString("product_id"),
                                MarketplaceType.fromValue(rs.getString("platform")),
                                rs.getString("title"),
                                rs.getBigDecimal("price"),
                                rs.getString("image"),
                                rs.getString("link"),
                                nullableDouble(rs, "rating"),
                                nullableInteger(rs, "reviews"),
                                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "attributes_json"), MAP_TYPE), Map.of()),
                                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "raw_data_json"), MAP_TYPE), Map.of())
                        ),
                        rs.getDouble("vector_score")
                ),
                semanticSearchParameters(platform, vectorLiteral, minScore, limit)
        );
    }

    private String buildEmbeddingId(MarketplaceType platform, String productId) {
        return platform.value().toLowerCase(Locale.ROOT) + ":" + productId;
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }

    private String jsonText(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private <T> T defaultIfNull(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private String platformPredicate(String column, MarketplaceType platform) {
        return column + " IN (" + String.join(", ", Collections.nCopies(platform.databaseValues().size(), "?")) + ")";
    }

    private Object[] semanticSearchParameters(
            MarketplaceType platform,
            String vectorLiteral,
            double minScore,
            int limit
    ) {
        List<Object> parameters = new ArrayList<>();
        parameters.add(vectorLiteral);
        parameters.addAll(platform.databaseValues());
        parameters.add(vectorLiteral);
        parameters.add(minScore);
        parameters.add(vectorLiteral);
        parameters.add(limit);
        return parameters.toArray();
    }

    public record SemanticSearchHit(
            Product product,
            double score
    ) {
    }

    private static final class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
