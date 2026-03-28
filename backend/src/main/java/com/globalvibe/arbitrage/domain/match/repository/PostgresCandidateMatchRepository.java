package com.globalvibe.arbitrage.domain.match.repository;

import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class PostgresCandidateMatchRepository implements CandidateMatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresCandidateMatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void replaceForCandidate(String candidateId, List<CandidateMatchRecord> matches) {
        jdbcTemplate.update("DELETE FROM gv_candidate_match WHERE candidate_id = ?", candidateId);
        matches.forEach(match -> jdbcTemplate.update("""
                        INSERT INTO gv_candidate_match (
                            match_id, task_id, candidate_id, source_product_id, platform, external_item_id,
                            title, price, similarity_score, match_source, search_keyword,
                            fallback_used, fallback_reason, reason, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                match.matchId() != null ? match.matchId() : "match-" + UUID.randomUUID(),
                match.taskId(),
                candidateId,
                match.sourceProductId(),
                match.platform(),
                match.externalItemId(),
                match.title(),
                match.price(),
                match.similarityScore(),
                match.matchSource(),
                match.searchKeyword(),
                match.fallbackUsed(),
                match.fallbackReason(),
                match.reason(),
                match.createdAt() != null ? match.createdAt() : OffsetDateTime.now()
        ));
    }

    @Override
    public List<CandidateMatchRecord> findTopByCandidateId(String candidateId, int limit) {
        return jdbcTemplate.query("""
                        SELECT m.task_id, m.candidate_id, m.source_product_id, m.match_id,
                               m.platform, m.external_item_id, m.title, m.price,
                               m.similarity_score, m.match_source, m.search_keyword,
                               m.fallback_used, m.fallback_reason, m.reason, m.created_at, p.image, p.link
                        FROM gv_candidate_match m
                        LEFT JOIN gv_platform_item p
                          ON p.platform = m.platform AND p.external_item_id = m.external_item_id
                        WHERE m.candidate_id = ?
                        ORDER BY m.similarity_score DESC, m.created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> CandidateMatchRecord.builder()
                        .matchId(rs.getString("match_id"))
                        .taskId(rs.getString("task_id"))
                        .candidateId(rs.getString("candidate_id"))
                        .sourceProductId(rs.getString("source_product_id"))
                        .platform(rs.getString("platform"))
                        .externalItemId(rs.getString("external_item_id"))
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price"))
                        .image(rs.getString("image"))
                        .link(rs.getString("link"))
                        .similarityScore(rs.getBigDecimal("similarity_score"))
                        .matchSource(rs.getString("match_source"))
                        .searchKeyword(rs.getString("search_keyword"))
                        .fallbackUsed(Boolean.TRUE.equals(rs.getObject("fallback_used", Boolean.class)))
                        .fallbackReason(rs.getString("fallback_reason"))
                        .reason(rs.getString("reason"))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build(),
                candidateId,
                limit
        );
    }

    @Override
    public List<CandidateMatchRecord> findTopByTaskId(String taskId, int limitPerCandidate) {
        return jdbcTemplate.query("""
                        SELECT ranked.task_id, ranked.candidate_id, ranked.source_product_id, ranked.match_id,
                               ranked.platform, ranked.external_item_id, ranked.title, ranked.price,
                               ranked.similarity_score, ranked.match_source, ranked.search_keyword,
                               ranked.fallback_used, ranked.fallback_reason, ranked.reason, ranked.created_at, p.image, p.link
                        FROM (
                            SELECT m.*,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY m.candidate_id
                                       ORDER BY m.similarity_score DESC, m.created_at DESC
                                   ) AS row_no
                            FROM gv_candidate_match m
                            WHERE m.task_id = ?
                        ) ranked
                        LEFT JOIN gv_platform_item p
                          ON p.platform = ranked.platform AND p.external_item_id = ranked.external_item_id
                        WHERE ranked.row_no <= ?
                        ORDER BY ranked.created_at DESC, ranked.similarity_score DESC
                        """,
                (rs, rowNum) -> CandidateMatchRecord.builder()
                        .matchId(rs.getString("match_id"))
                        .taskId(rs.getString("task_id"))
                        .candidateId(rs.getString("candidate_id"))
                        .sourceProductId(rs.getString("source_product_id"))
                        .platform(rs.getString("platform"))
                        .externalItemId(rs.getString("external_item_id"))
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price"))
                        .image(rs.getString("image"))
                        .link(rs.getString("link"))
                        .similarityScore(rs.getBigDecimal("similarity_score"))
                        .matchSource(rs.getString("match_source"))
                        .searchKeyword(rs.getString("search_keyword"))
                        .fallbackUsed(Boolean.TRUE.equals(rs.getObject("fallback_used", Boolean.class)))
                        .fallbackReason(rs.getString("fallback_reason"))
                        .reason(rs.getString("reason"))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build(),
                taskId,
                limitPerCandidate
        );
    }

    @Override
    public List<CandidateMatchRecord> findTopBySourceProductId(String sourceProductId, int limit) {
        return jdbcTemplate.query("""
                        SELECT m.task_id, m.candidate_id, m.source_product_id, m.match_id,
                               m.platform, m.external_item_id, m.title, m.price,
                               m.similarity_score, m.match_source, m.search_keyword,
                               m.fallback_used, m.fallback_reason, m.reason, m.created_at, p.image, p.link
                        FROM gv_candidate_match m
                        LEFT JOIN gv_platform_item p
                          ON p.platform = m.platform AND p.external_item_id = m.external_item_id
                        WHERE m.source_product_id = ?
                        ORDER BY m.similarity_score DESC, m.created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> CandidateMatchRecord.builder()
                        .matchId(rs.getString("match_id"))
                        .taskId(rs.getString("task_id"))
                        .candidateId(rs.getString("candidate_id"))
                        .sourceProductId(rs.getString("source_product_id"))
                        .platform(rs.getString("platform"))
                        .externalItemId(rs.getString("external_item_id"))
                        .title(rs.getString("title"))
                        .price(rs.getBigDecimal("price"))
                        .image(rs.getString("image"))
                        .link(rs.getString("link"))
                        .similarityScore(rs.getBigDecimal("similarity_score"))
                        .matchSource(rs.getString("match_source"))
                        .searchKeyword(rs.getString("search_keyword"))
                        .fallbackUsed(Boolean.TRUE.equals(rs.getObject("fallback_used", Boolean.class)))
                        .fallbackReason(rs.getString("fallback_reason"))
                        .reason(rs.getString("reason"))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build(),
                sourceProductId,
                limit
        );
    }
}
