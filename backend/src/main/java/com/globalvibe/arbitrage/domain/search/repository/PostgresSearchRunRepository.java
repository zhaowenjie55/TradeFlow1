package com.globalvibe.arbitrage.domain.search.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.search.model.SearchRun;
import com.globalvibe.arbitrage.domain.search.model.SearchRunResult;
import com.globalvibe.arbitrage.domain.search.model.SearchRunStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PostgresSearchRunRepository implements SearchRunRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public PostgresSearchRunRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    @Override
    public SearchRun save(SearchRun searchRun) {
        jdbcTemplate.update("""
                INSERT INTO gv_search_run (
                    search_run_id, task_id, phase, platform, query_text, status, fallback_used,
                    error_message, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (search_run_id) DO UPDATE SET
                    task_id = EXCLUDED.task_id,
                    phase = EXCLUDED.phase,
                    platform = EXCLUDED.platform,
                    query_text = EXCLUDED.query_text,
                    status = EXCLUDED.status,
                    fallback_used = EXCLUDED.fallback_used,
                    error_message = EXCLUDED.error_message,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """,
                searchRun.searchRunId(),
                searchRun.taskId(),
                searchRun.phase(),
                searchRun.platform(),
                searchRun.queryText(),
                searchRun.status().name(),
                searchRun.fallbackUsed(),
                searchRun.errorMessage(),
                searchRun.createdAt(),
                searchRun.updatedAt()
        );
        return searchRun;
    }

    @Override
    public void replaceResults(String searchRunId, List<SearchRunResult> results) {
        jdbcTemplate.update("DELETE FROM gv_search_run_result WHERE search_run_id = ?", searchRunId);
        results.forEach(result -> jdbcTemplate.update("""
                        INSERT INTO gv_search_run_result (
                            search_run_id, platform, external_item_id, rank_no, title,
                            price, image, link, raw_jsonb
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (search_run_id, platform, external_item_id) DO UPDATE SET
                            rank_no = EXCLUDED.rank_no,
                            title = EXCLUDED.title,
                            price = EXCLUDED.price,
                            image = EXCLUDED.image,
                            link = EXCLUDED.link,
                            raw_jsonb = EXCLUDED.raw_jsonb
                        """,
                result.searchRunId(),
                result.platform(),
                result.externalItemId(),
                result.rankNo(),
                result.title(),
                result.price(),
                result.image(),
                result.link(),
                jdbcJsonSupport.toJsonb(result.rawData())
        ));
    }

    @Override
    public List<SearchRunResult> findResultsBySearchRunId(String searchRunId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_search_run_result
                        WHERE search_run_id = ?
                        ORDER BY rank_no ASC
                        """,
                (rs, rowNum) -> SearchRunResult.builder()
                        .searchRunId(rs.getString("search_run_id"))
                        .platform(rs.getString("platform"))
                        .externalItemId(rs.getString("external_item_id"))
                        .rankNo(rs.getInt("rank_no"))
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price"))
                        .image(rs.getString("image"))
                        .link(rs.getString("link"))
                        .rawData(defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "raw_jsonb"), MAP_TYPE), Map.of()))
                        .build(),
                searchRunId
        );
    }

    @Override
    public Optional<SearchRun> findLatestSuccessfulRun(String platform, String queryText) {
        List<SearchRun> runs = jdbcTemplate.query("""
                        SELECT * FROM gv_search_run
                        WHERE platform = ?
                          AND LOWER(query_text) = LOWER(?)
                          AND status IN ('SUCCEEDED', 'FALLBACK')
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                this::mapRun,
                platform,
                queryText
        );
        if (!runs.isEmpty() || queryText == null || queryText.isBlank()) {
            return runs.stream().findFirst();
        }
        runs = jdbcTemplate.query("""
                        SELECT * FROM gv_search_run
                        WHERE platform = ?
                          AND status IN ('SUCCEEDED', 'FALLBACK')
                          AND LOWER(query_text) % LOWER(?)
                        ORDER BY similarity(LOWER(query_text), LOWER(?)) DESC, created_at DESC
                        LIMIT 1
                        """,
                this::mapRun,
                platform,
                queryText,
                queryText
        );
        return runs.stream().findFirst();
    }

    private SearchRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        return SearchRun.builder()
                .searchRunId(rs.getString("search_run_id"))
                .taskId(rs.getString("task_id"))
                .phase(rs.getString("phase"))
                .platform(rs.getString("platform"))
                .queryText(rs.getString("query_text"))
                .status(SearchRunStatus.valueOf(rs.getString("status")))
                .fallbackUsed(rs.getBoolean("fallback_used"))
                .errorMessage(rs.getString("error_message"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .build();
    }

    private String jsonText(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }

    private <T> T defaultIfNull(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
