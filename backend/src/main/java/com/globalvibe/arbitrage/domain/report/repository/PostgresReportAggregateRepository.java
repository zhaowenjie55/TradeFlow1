package com.globalvibe.arbitrage.domain.report.repository;

import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import com.globalvibe.arbitrage.domain.report.model.ReportProvenance;
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
                    report_id, task_id, estimated_profit, estimated_margin,
                    rewrite_provider, rewrite_model, retrieval_source, detail_source,
                    fallback_used, fallback_reason, llm_provider, llm_model, quality_tier,
                    pricing_config_version, report_markdown, report_jsonb, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (report_id) DO UPDATE SET
                    task_id = EXCLUDED.task_id,
                    estimated_profit = EXCLUDED.estimated_profit,
                    estimated_margin = EXCLUDED.estimated_margin,
                    rewrite_provider = EXCLUDED.rewrite_provider,
                    rewrite_model = EXCLUDED.rewrite_model,
                    retrieval_source = EXCLUDED.retrieval_source,
                    detail_source = EXCLUDED.detail_source,
                    fallback_used = EXCLUDED.fallback_used,
                    fallback_reason = EXCLUDED.fallback_reason,
                    llm_provider = EXCLUDED.llm_provider,
                    llm_model = EXCLUDED.llm_model,
                    quality_tier = EXCLUDED.quality_tier,
                    pricing_config_version = EXCLUDED.pricing_config_version,
                    report_markdown = EXCLUDED.report_markdown,
                    report_jsonb = EXCLUDED.report_jsonb,
                    created_at = EXCLUDED.created_at
                """,
                reportAggregate.reportId(),
                reportAggregate.taskId(),
                reportAggregate.estimatedProfit(),
                reportAggregate.estimatedMargin(),
                reportAggregate.provenance() != null ? reportAggregate.provenance().rewriteProvider() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().rewriteModel() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().retrievalSource() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().detailSource() : null,
                reportAggregate.provenance() != null && reportAggregate.provenance().fallbackUsed(),
                reportAggregate.provenance() != null ? reportAggregate.provenance().fallbackReason() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().llmProvider() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().llmModel() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().qualityTier() : null,
                reportAggregate.provenance() != null ? reportAggregate.provenance().pricingConfigVersion() : null,
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
                        .provenance(mapProvenance(rs))
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
                        .provenance(mapProvenance(rs))
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
                        .provenance(mapProvenance(rs))
                        .reportMarkdown(rs.getString("report_markdown"))
                        .report(jdbcJsonSupport.fromJson(jsonText(rs, "report_jsonb"), ArbitrageReport.class))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .build()
        );
    }

    private ReportProvenance mapProvenance(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ReportProvenance(
                rs.getString("rewrite_provider"),
                rs.getString("rewrite_model"),
                rs.getString("retrieval_source"),
                rs.getString("detail_source"),
                Boolean.TRUE.equals(rs.getObject("fallback_used", Boolean.class)),
                rs.getString("fallback_reason"),
                rs.getString("llm_provider"),
                rs.getString("llm_model"),
                rs.getString("quality_tier"),
                rs.getString("pricing_config_version")
        );
    }

    private String jsonText(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }
}
