package com.globalvibe.arbitrage.domain.system.service;

import com.globalvibe.arbitrage.domain.system.dto.IntegrityAuditResponse;
import com.globalvibe.arbitrage.domain.system.dto.IntegrityConstraintStatusVO;
import com.globalvibe.arbitrage.domain.system.dto.IntegrityOrphanCheckVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RelationalIntegrityAuditService {

    private static final List<OrphanCheckDefinition> ORPHAN_CHECKS = List.of(
            new OrphanCheckDefinition(
                    "gv_search_run_result.search_run_id -> gv_search_run.search_run_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_search_run_result r
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_search_run s
                                WHERE s.search_run_id = r.search_run_id
                            )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_query_rewrite.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_query_rewrite q
                            WHERE q.task_id IS NOT NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM gv_analysis_task t
                                  WHERE t.task_id = q.task_id
                              )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_query_rewrite.candidate_id -> gv_analysis_candidate.candidate_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_query_rewrite q
                            WHERE q.candidate_id IS NOT NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM gv_analysis_candidate c
                                  WHERE c.candidate_id = q.candidate_id
                              )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_candidate_match.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_candidate_match m
                            WHERE m.task_id IS NOT NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM gv_analysis_task t
                                  WHERE t.task_id = m.task_id
                              )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_candidate_match.candidate_id -> gv_analysis_candidate.candidate_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_candidate_match m
                            WHERE m.candidate_id IS NOT NULL
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM gv_analysis_candidate c
                                  WHERE c.candidate_id = m.candidate_id
                              )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_analysis_task_log.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_analysis_task_log l
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_analysis_task t
                                WHERE t.task_id = l.task_id
                            )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_analysis_report.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_analysis_report r
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_analysis_task t
                                WHERE t.task_id = r.task_id
                            )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_analysis_candidate.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_analysis_candidate c
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_analysis_task t
                                WHERE t.task_id = c.task_id
                            )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_search_run.task_id -> gv_analysis_task.task_id",
                    """
                            SELECT COUNT(*)
                            FROM gv_search_run s
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_analysis_task t
                                WHERE t.task_id = s.task_id
                            )
                            """
            ),
            new OrphanCheckDefinition(
                    "gv_analysis_report.pricing_config_version -> gv_pricing_config.version",
                    """
                            SELECT COUNT(*)
                            FROM gv_analysis_report r
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM gv_pricing_config p
                                WHERE p.version = r.pricing_config_version
                            )
                            """
            )
    );

    private static final List<String> FK_CONSTRAINTS = List.of(
            "fk_gv_analysis_task_log_task",
            "fk_gv_search_run_task",
            "fk_gv_search_run_result_run",
            "fk_gv_analysis_candidate_task",
            "fk_gv_query_rewrite_task",
            "fk_gv_query_rewrite_candidate",
            "fk_gv_candidate_match_task",
            "fk_gv_candidate_match_candidate",
            "fk_gv_analysis_report_task",
            "fk_gv_analysis_report_pricing_config"
    );

    private final JdbcTemplate jdbcTemplate;

    public RelationalIntegrityAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IntegrityAuditResponse audit() {
        List<IntegrityOrphanCheckVO> orphanChecks = ORPHAN_CHECKS.stream()
                .map(definition -> new IntegrityOrphanCheckVO(definition.name(), count(definition.sql())))
                .toList();

        List<IntegrityConstraintStatusVO> constraintStatuses = FK_CONSTRAINTS.stream()
                .map(constraintName -> new IntegrityConstraintStatusVO(constraintName, isConstraintValidated(constraintName)))
                .toList();

        long totalOrphanCount = orphanChecks.stream()
                .mapToLong(IntegrityOrphanCheckVO::orphanCount)
                .sum();

        boolean allConstraintsValidated = constraintStatuses.stream()
                .allMatch(IntegrityConstraintStatusVO::validated);

        return new IntegrityAuditResponse(
                totalOrphanCount == 0 && allConstraintsValidated,
                allConstraintsValidated,
                totalOrphanCount,
                OffsetDateTime.now(),
                orphanChecks,
                constraintStatuses
        );
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private boolean isConstraintValidated(String constraintName) {
        Boolean validated = jdbcTemplate.queryForObject(
                """
                        SELECT convalidated
                        FROM pg_constraint
                        WHERE conname = ?
                        """,
                Boolean.class,
                constraintName
        );
        return Boolean.TRUE.equals(validated);
    }

    private record OrphanCheckDefinition(
            String name,
            String sql
    ) {
    }
}
