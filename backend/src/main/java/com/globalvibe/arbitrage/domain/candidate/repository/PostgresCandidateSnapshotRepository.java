package com.globalvibe.arbitrage.domain.candidate.repository;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateSnapshot;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@Primary
public class PostgresCandidateSnapshotRepository implements CandidateSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresCandidateSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void replaceForTask(String taskId, List<CandidateSnapshot> snapshots) {
        jdbcTemplate.update("DELETE FROM gv_analysis_candidate WHERE task_id = ?", taskId);
        snapshots.forEach(snapshot -> jdbcTemplate.update("""
                        INSERT INTO gv_analysis_candidate (
                            candidate_id, task_id, platform, external_item_id, title, price,
                            image, score, risk_tag, recommendation_reason,
                            suggest_second_phase, link, status, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                taskId + ":" + snapshot.productId(),
                taskId,
                snapshot.market(),
                snapshot.productId(),
                snapshot.title(),
                snapshot.overseasPrice(),
                snapshot.imageUrl(),
                snapshot.estimatedMargin(),
                snapshot.riskTag(),
                snapshot.recommendationReason(),
                snapshot.suggestSecondPhase(),
                snapshot.link(),
                snapshot.suggestSecondPhase() ? "READY" : "REVIEW",
                snapshot.createdAt()
        ));
    }

    @Override
    public List<CandidateSnapshot> findByTaskId(String taskId) {
        return jdbcTemplate.query(
                "SELECT * FROM gv_analysis_candidate WHERE task_id = ? ORDER BY created_at ASC, external_item_id ASC",
                rowMapper(),
                taskId
        );
    }

    private RowMapper<CandidateSnapshot> rowMapper() {
        return (rs, rowNum) -> CandidateSnapshot.builder()
                .taskId(rs.getString("task_id"))
                .productId(rs.getString("external_item_id"))
                .title(rs.getString("title"))
                .imageUrl(rs.getString("image"))
                .market(rs.getString("platform"))
                .overseasPrice(rs.getBigDecimal("price"))
                .estimatedMargin(rs.getBigDecimal("score"))
                .riskTag(rs.getString("risk_tag"))
                .recommendationReason(rs.getString("recommendation_reason"))
                .suggestSecondPhase(rs.getBoolean("suggest_second_phase"))
                .link(rs.getString("link"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .build();
    }
}
