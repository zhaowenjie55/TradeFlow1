package com.globalvibe.arbitrage.domain.product.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
public class PostgresProductRepository implements ProductRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public PostgresProductRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    @Override
    public void saveAll(List<Product> products) {
        products.forEach(this::save);
    }

    @Override
    public Product save(Product product) {
        upsertGlobalPlatformItem(product);
        insertPlatformItemSnapshot(product, null);
        return product;
    }

    @Override
    public Optional<Product> findById(String productId) {
        List<Product> products = jdbcTemplate.query(
                """
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE p.external_item_id = ?
                        ORDER BY p.updated_at DESC
                        LIMIT 1
                        """,
                productRowMapper(),
                productId
        );
        return products.stream().findFirst();
    }

    @Override
    public List<Product> findByPlatform(MarketplaceType platform) {
        return jdbcTemplate.query(
                """
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE __PLATFORM_PREDICATE__
                        ORDER BY p.updated_at DESC, p.title ASC
                        """.replace("__PLATFORM_PREDICATE__", platformPredicate("p.platform", platform)),
                productRowMapper(),
                withPlatformValues(platform)
        );
    }

    @Override
    public List<Product> searchByPlatformAndKeyword(MarketplaceType platform, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return findByPlatform(platform).stream().limit(limit).toList();
        }
        List<Product> trigramMatches = jdbcTemplate.query("""
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE __PLATFORM_PREDICATE__
                          AND LOWER(p.title) % LOWER(?)
                        ORDER BY similarity(LOWER(p.title), LOWER(?)) DESC, p.updated_at DESC
                        LIMIT ?
                        """.replace("__PLATFORM_PREDICATE__", platformPredicate("p.platform", platform)),
                productRowMapper(),
                withPlatformValues(platform, keyword, keyword, limit)
        );
        if (!trigramMatches.isEmpty()) {
            return trigramMatches;
        }
        return jdbcTemplate.query("""
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE __PLATFORM_PREDICATE__
                          AND LOWER(p.title) LIKE CONCAT('%', LOWER(?), '%')
                        ORDER BY p.updated_at DESC
                        LIMIT ?
                        """.replace("__PLATFORM_PREDICATE__", platformPredicate("p.platform", platform)),
                productRowMapper(),
                withPlatformValues(platform, keyword.replace(" ", ""), limit)
        );
    }

    @Override
    public List<Product> searchByPlatformAndKeywordIncludingDetails(MarketplaceType platform, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return findByPlatform(platform).stream().limit(limit).toList();
        }
        List<Product> trigramMatches = jdbcTemplate.query("""
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN product_detail_snapshot d
                          ON d.product_id = p.external_item_id
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE __PLATFORM_PREDICATE__
                          AND (
                            LOWER(p.title) % LOWER(?)
                            OR LOWER(COALESCE(d.title, '')) % LOWER(?)
                            OR LOWER(COALESCE(d.brand, '')) % LOWER(?)
                          )
                        ORDER BY GREATEST(
                            similarity(LOWER(p.title), LOWER(?)),
                            similarity(LOWER(COALESCE(d.title, '')), LOWER(?)),
                            similarity(LOWER(COALESCE(d.brand, '')), LOWER(?))
                        ) DESC,
                        p.updated_at DESC
                        LIMIT ?
                        """.replace("__PLATFORM_PREDICATE__", platformPredicate("p.platform", platform)),
                productRowMapper(),
                withPlatformValues(platform, keyword, keyword, keyword, keyword, keyword, keyword, limit)
        );
        if (!trigramMatches.isEmpty()) {
            return trigramMatches;
        }
        return jdbcTemplate.query("""
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN product_detail_snapshot d
                          ON d.product_id = p.external_item_id
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        WHERE __PLATFORM_PREDICATE__
                          AND (
                            LOWER(p.title) LIKE CONCAT('%', LOWER(?), '%')
                            OR LOWER(COALESCE(d.title, '')) LIKE CONCAT('%', LOWER(?), '%')
                            OR LOWER(COALESCE(d.brand, '')) LIKE CONCAT('%', LOWER(?), '%')
                            OR LOWER(COALESCE(d.description, '')) LIKE CONCAT('%', LOWER(?), '%')
                            OR LOWER(COALESCE(d.attributes_json::text, '')) LIKE CONCAT('%', LOWER(?), '%')
                          )
                        ORDER BY p.updated_at DESC
                        LIMIT ?
                        """.replace("__PLATFORM_PREDICATE__", platformPredicate("p.platform", platform)),
                productRowMapper(),
                withPlatformValues(platform, keyword, keyword, keyword, keyword, keyword, limit)
        );
    }

    @Override
    public List<Product> findAll() {
        return jdbcTemplate.query(
                """
                        SELECT p.platform, p.external_item_id AS product_id, p.title, p.price, p.image, p.link,
                               p.rating, p.reviews, COALESCE(p.attributes_jsonb, '{}'::jsonb) AS attributes_json,
                               s.raw_jsonb AS raw_data_json
                        FROM gv_platform_item p
                        LEFT JOIN LATERAL (
                            SELECT raw_jsonb
                            FROM gv_platform_item_snapshot s
                            WHERE s.platform = p.platform
                              AND s.external_item_id = p.external_item_id
                            ORDER BY s.created_at DESC
                            LIMIT 1
                        ) s ON TRUE
                        ORDER BY p.updated_at DESC, p.title ASC
                        """,
                productRowMapper()
        );
    }

    @Override
    public void saveDetail(ProductDetailSnapshot detailSnapshot) {
        jdbcTemplate.update("""
                INSERT INTO product_detail_snapshot (
                    product_id, platform, title, price, brand, image, link, description,
                    gallery_json, attributes_json, sku_data_json, raw_data_json, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (product_id) DO UPDATE SET
                    platform = EXCLUDED.platform,
                    title = EXCLUDED.title,
                    price = EXCLUDED.price,
                    brand = EXCLUDED.brand,
                    image = EXCLUDED.image,
                    link = EXCLUDED.link,
                    description = EXCLUDED.description,
                    gallery_json = EXCLUDED.gallery_json,
                    attributes_json = EXCLUDED.attributes_json,
                    sku_data_json = EXCLUDED.sku_data_json,
                    raw_data_json = EXCLUDED.raw_data_json,
                    updated_at = NOW()
                """,
                detailSnapshot.productId(),
                detailSnapshot.platform().value(),
                detailSnapshot.title(),
                detailSnapshot.price(),
                detailSnapshot.brand(),
                detailSnapshot.image(),
                detailSnapshot.link(),
                detailSnapshot.description(),
                jdbcJsonSupport.toJsonb(detailSnapshot.gallery()),
                jdbcJsonSupport.toJsonb(detailSnapshot.attributes()),
                jdbcJsonSupport.toJsonb(detailSnapshot.skuData()),
                jdbcJsonSupport.toJsonb(detailSnapshot.rawData())
        );
        upsertGlobalPlatformItem(new Product(
                detailSnapshot.productId(),
                detailSnapshot.platform(),
                detailSnapshot.title(),
                detailSnapshot.price(),
                detailSnapshot.image(),
                detailSnapshot.link(),
                null,
                null,
                detailSnapshot.attributes(),
                detailSnapshot.rawData()
        ));
        insertDetailSnapshot(detailSnapshot);
    }

    @Override
    public Optional<ProductDetailSnapshot> findDetailByProductId(String productId) {
        List<ProductDetailSnapshot> details = jdbcTemplate.query(
                "SELECT * FROM product_detail_snapshot WHERE product_id = ?",
                detailRowMapper(),
                productId
        );
        return details.stream().findFirst();
    }

    private RowMapper<Product> productRowMapper() {
        return (rs, rowNum) -> new Product(
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
        );
    }

    private RowMapper<ProductDetailSnapshot> detailRowMapper() {
        return (rs, rowNum) -> new ProductDetailSnapshot(
                rs.getString("product_id"),
                MarketplaceType.fromValue(rs.getString("platform")),
                rs.getString("title"),
                rs.getBigDecimal("price"),
                rs.getString("brand"),
                rs.getString("image"),
                rs.getString("link"),
                rs.getString("description"),
                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "gallery_json"), STRING_LIST), List.of()),
                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "attributes_json"), MAP_TYPE), Map.of()),
                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "sku_data_json"), MAP_TYPE), Map.of()),
                defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "raw_data_json"), MAP_TYPE), Map.of())
        );
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

    private void upsertGlobalPlatformItem(Product product) {
        jdbcTemplate.update("""
                INSERT INTO gv_platform_item (
                    platform, external_item_id, title, price, rating, reviews, image, link,
                    attributes_jsonb, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (platform, external_item_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    price = EXCLUDED.price,
                    rating = EXCLUDED.rating,
                    reviews = EXCLUDED.reviews,
                    image = EXCLUDED.image,
                    link = EXCLUDED.link,
                    attributes_jsonb = EXCLUDED.attributes_jsonb,
                    updated_at = NOW()
                """,
                product.platform().value(),
                product.id(),
                product.title(),
                product.price(),
                product.rating(),
                product.reviews(),
                product.image(),
                product.link(),
                jdbcJsonSupport.toJsonb(product.attributes())
        );
    }

    private void insertPlatformItemSnapshot(Product product, String queryText) {
        jdbcTemplate.update("""
                INSERT INTO gv_platform_item_snapshot (
                    snapshot_id, platform, external_item_id, query_text, title, price, rating,
                    reviews, image, link, raw_jsonb, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "snap-" + UUID.randomUUID(),
                product.platform().value(),
                product.id(),
                queryText,
                product.title(),
                product.price(),
                product.rating(),
                product.reviews(),
                product.image(),
                product.link(),
                jdbcJsonSupport.toJsonb(product.rawData()),
                OffsetDateTime.now()
        );
    }

    private void insertDetailSnapshot(ProductDetailSnapshot detailSnapshot) {
        jdbcTemplate.update("""
                INSERT INTO gv_platform_item_snapshot (
                    snapshot_id, platform, external_item_id, query_text, title, price, rating,
                    reviews, image, link, raw_jsonb, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "snap-" + UUID.randomUUID(),
                detailSnapshot.platform().value(),
                detailSnapshot.productId(),
                null,
                detailSnapshot.title(),
                detailSnapshot.price(),
                null,
                null,
                detailSnapshot.image(),
                detailSnapshot.link(),
                jdbcJsonSupport.toJsonb(detailSnapshot.rawData()),
                OffsetDateTime.now()
        );
    }

    private String platformPredicate(String column, MarketplaceType platform) {
        return column + " IN (" + String.join(", ", Collections.nCopies(platform.databaseValues().size(), "?")) + ")";
    }

    private Object[] withPlatformValues(MarketplaceType platform, Object... additionalParameters) {
        List<Object> parameters = new ArrayList<>(platform.databaseValues());
        Collections.addAll(parameters, additionalParameters);
        return parameters.toArray();
    }
}
