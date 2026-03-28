package com.globalvibe.arbitrage.domain.search.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresQueryRewriteRepository implements QueryRewriteRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public PostgresQueryRewriteRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    @Override
    public QueryRewrite save(QueryRewrite queryRewrite) {
        String rewriteId = queryRewrite.rewriteId() != null ? queryRewrite.rewriteId() : "rewrite-" + UUID.randomUUID();
        OffsetDateTime createdAt = queryRewrite.createdAt() != null ? queryRewrite.createdAt() : OffsetDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO gv_query_rewrite (
                    rewrite_id, task_id, candidate_id, source_product_id, source_text,
                    rewritten_text, keywords_jsonb, gateway_source, fallback_used, fallback_reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                rewriteId,
                queryRewrite.taskId(),
                queryRewrite.candidateId(),
                queryRewrite.sourceProductId(),
                queryRewrite.sourceText(),
                queryRewrite.rewrittenText(),
                jdbcJsonSupport.toJsonb(queryRewrite.keywords()),
                queryRewrite.gatewaySource(),
                queryRewrite.fallbackUsed(),
                queryRewrite.fallbackReason(),
                createdAt
        );
        return QueryRewrite.builder()
                .rewriteId(rewriteId)
                .taskId(queryRewrite.taskId())
                .candidateId(queryRewrite.candidateId())
                .sourceProductId(queryRewrite.sourceProductId())
                .sourceText(queryRewrite.sourceText())
                .rewrittenText(queryRewrite.rewrittenText())
                .keywords(queryRewrite.keywords())
                .gatewaySource(queryRewrite.gatewaySource())
                .fallbackUsed(queryRewrite.fallbackUsed())
                .fallbackReason(queryRewrite.fallbackReason())
                .createdAt(createdAt)
                .build();
    }

    @Override
    public Optional<QueryRewrite> findLatestBySourceText(String sourceText) {
        List<QueryRewrite> rewrites = jdbcTemplate.query("""
                        SELECT * FROM gv_query_rewrite
                        WHERE source_text = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                this::mapRow,
                sourceText
        );
        return rewrites.stream().findFirst();
    }

    @Override
    public Optional<QueryRewrite> findLatestByCandidateId(String candidateId) {
        List<QueryRewrite> rewrites = jdbcTemplate.query("""
                        SELECT * FROM gv_query_rewrite
                        WHERE candidate_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                this::mapRow,
                candidateId
        );
        return rewrites.stream().findFirst();
    }

    @Override
    public Optional<QueryRewrite> findLatestBySourceProductId(String sourceProductId) {
        List<QueryRewrite> rewrites = jdbcTemplate.query("""
                        SELECT * FROM gv_query_rewrite
                        WHERE source_product_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                this::mapRow,
                sourceProductId
        );
        return rewrites.stream().findFirst();
    }

    @Override
    public List<QueryRewrite> findByTaskId(String taskId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_query_rewrite
                        WHERE task_id = ?
                        ORDER BY created_at DESC
                        """,
                this::mapRow,
                taskId
        );
    }

    private QueryRewrite mapRow(ResultSet rs, int rowNum) throws SQLException {
        return QueryRewrite.builder()
                .rewriteId(rs.getString("rewrite_id"))
                .taskId(rs.getString("task_id"))
                .candidateId(rs.getString("candidate_id"))
                .sourceProductId(rs.getString("source_product_id"))
                .sourceText(rs.getString("source_text"))
                .rewrittenText(rs.getString("rewritten_text"))
                .keywords(jdbcJsonSupport.fromJson(jsonText(rs, "keywords_jsonb"), STRING_LIST))
                .gatewaySource(rs.getString("gateway_source"))
                .fallbackUsed(Boolean.TRUE.equals(rs.getObject("fallback_used", Boolean.class)))
                .fallbackReason(rs.getString("fallback_reason"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .build();
    }

    private String jsonText(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }
}
