package com.globalvibe.arbitrage.domain.report.repository;

import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class PostgresReportAggregateRepository implements ReportAggregateRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public PostgresReportAggregateRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    @Override
    public ReportAggregate save(ReportAggregate reportAggregate) {
        jdbcTemplate.update("""
                INSERT INTO gv_analysis_report (
                    report_id, task_id, estimated_profit, estimated_margin, report_markdown, report_jsonb, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (report_id) DO UPDATE SET
                    task_id = EXCLUDED.task_id,
                    estimated_profit = EXCLUDED.estimated_profit,
                    estimated_margin = EXCLUDED.estimated_margin,
                    report_markdown = EXCLUDED.report_markdown,
                    report_jsonb = EXCLUDED.report_jsonb,
                    created_at = EXCLUDED.created_at
                """,
                reportAggregate.reportId(),
                reportAggregate.taskId(),
                reportAggregate.estimatedProfit(),
                reportAggregate.estimatedMargin(),
                reportAggregate.reportMarkdown(),
                jdbcJsonSupport.toJsonb(reportAggregate.report()),
                reportAggregate.createdAt()
        );
        return reportAggregate;
    }

    @Override
    public Optional<ReportAggregate> findByTaskId(String taskId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_analysis_report
                        WHERE task_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> ReportAggregate.builder()
                        .taskId(rs.getString("task_id"))
                        .reportId(rs.getString("report_id"))
                        .estimatedProfit(rs.getBigDecimal("estimated_profit"))
                        .estimatedMargin(rs.getBigDecimal("estimated_margin"))
                        .reportMarkdown(rs.getString("report_markdown"))
                        .report(jdbcJsonSupport.fromJson(jsonText(rs, "report_jsonb"), ArbitrageReport.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build(),
                taskId
        ).stream().findFirst();
    }

    @Override
    public Optional<ReportAggregate> findByReportId(String reportId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_analysis_report
                        WHERE report_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> ReportAggregate.builder()
                        .taskId(rs.getString("task_id"))
                        .reportId(rs.getString("report_id"))
                        .estimatedProfit(rs.getBigDecimal("estimated_profit"))
                        .estimatedMargin(rs.getBigDecimal("estimated_margin"))
                        .reportMarkdown(rs.getString("report_markdown"))
                        .report(jdbcJsonSupport.fromJson(jsonText(rs, "report_jsonb"), ArbitrageReport.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build(),
                reportId
        ).stream().findFirst();
    }

    @Override
    public List<ReportAggregate> findAll() {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_analysis_report
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> ReportAggregate.builder()
                        .taskId(rs.getString("task_id"))
                        .reportId(rs.getString("report_id"))
                        .estimatedProfit(rs.getBigDecimal("estimated_profit"))
                        .estimatedMargin(rs.getBigDecimal("estimated_margin"))
                        .reportMarkdown(rs.getString("report_markdown"))
                        .report(jdbcJsonSupport.fromJson(jsonText(rs, "report_jsonb"), ArbitrageReport.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build()
        );
    }

    private String jsonText(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }
}
