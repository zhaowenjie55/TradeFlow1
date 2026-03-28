package com.globalvibe.arbitrage.domain.task.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.globalvibe.arbitrage.common.persistence.JdbcJsonSupport;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskConstraint;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Repository
@Primary
public class PostgresAnalysisTaskRepository implements AnalysisTaskRepository {

    private static final TypeReference<List<TaskConstraint>> TASK_CONSTRAINTS = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final JdbcJsonSupport jdbcJsonSupport;

    public PostgresAnalysisTaskRepository(JdbcTemplate jdbcTemplate, JdbcJsonSupport jdbcJsonSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcJsonSupport = jdbcJsonSupport;
    }

    @Override
    public AnalysisTask save(AnalysisTask analysisTask) {
        jdbcTemplate.update("""
                INSERT INTO gv_analysis_task (
                    task_id, parent_task_id, phase, keyword, constraints_jsonb, market,
                    requested_limit, target_profit_margin, status, mode,
                    selected_product_id, report_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (task_id) DO UPDATE SET
                    parent_task_id = EXCLUDED.parent_task_id,
                    phase = EXCLUDED.phase,
                    keyword = EXCLUDED.keyword,
                    constraints_jsonb = EXCLUDED.constraints_jsonb,
                    market = EXCLUDED.market,
                    requested_limit = EXCLUDED.requested_limit,
                    target_profit_margin = EXCLUDED.target_profit_margin,
                    status = EXCLUDED.status,
                    mode = EXCLUDED.mode,
                    selected_product_id = EXCLUDED.selected_product_id,
                    report_id = EXCLUDED.report_id,
                    created_at = EXCLUDED.created_at,
                    updated_at = EXCLUDED.updated_at
                """,
                analysisTask.getTaskId(),
                analysisTask.getParentTaskId(),
                analysisTask.getPhase().name(),
                analysisTask.getKeyword(),
                jdbcJsonSupport.toJsonb(analysisTask.getConstraints()),
                analysisTask.getMarket(),
                analysisTask.getRequestedLimit(),
                analysisTask.getTargetProfitMargin(),
                analysisTask.getStatus().name(),
                analysisTask.getMode().name(),
                analysisTask.getSelectedProductId(),
                analysisTask.getReportId(),
                analysisTask.getCreatedAt(),
                analysisTask.getUpdatedAt()
        );
        replaceLogs(analysisTask);
        return analysisTask;
    }

    @Override
    public Optional<AnalysisTask> findById(String taskId) {
        List<AnalysisTask> results = jdbcTemplate.query(
                "SELECT * FROM gv_analysis_task WHERE task_id = ?",
                (rs, rowNum) -> mapAnalysisTask(rs),
                taskId
        );
        return results.stream().findFirst();
    }

    @Override
    public List<AnalysisTask> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM gv_analysis_task ORDER BY created_at DESC",
                (rs, rowNum) -> mapAnalysisTask(rs)
        );
    }

    private AnalysisTask mapAnalysisTask(ResultSet rs) throws SQLException {
        String taskId = rs.getString("task_id");
        return AnalysisTask.builder()
                .taskId(taskId)
                .parentTaskId(rs.getString("parent_task_id"))
                .phase(TaskPhase.valueOf(rs.getString("phase")))
                .status(TaskStatus.valueOf(rs.getString("status")))
                .keyword(rs.getString("keyword"))
                .constraints(defaultIfNull(jdbcJsonSupport.fromJson(jsonText(rs, "constraints_jsonb"), TASK_CONSTRAINTS), List.of()))
                .market(rs.getString("market"))
                .requestedLimit((Integer) rs.getObject("requested_limit"))
                .targetProfitMargin(rs.getBigDecimal("target_profit_margin"))
                .selectedProductId(resolveSelectedProductId(rs))
                .logs(findLogsByTaskId(taskId))
                .candidates(findCandidatesByTaskId(taskId))
                .domesticMatches(List.of())
                .reportId(rs.getString("report_id"))
                .report(null)
                .mode(TaskMode.valueOf(rs.getString("mode")))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .build();
    }

    private void replaceLogs(AnalysisTask analysisTask) {
        jdbcTemplate.update("DELETE FROM gv_analysis_task_log WHERE task_id = ?", analysisTask.getTaskId());
        IntStream.range(0, analysisTask.getLogs().size())
                .forEach(index -> {
                    TaskLogEntry log = analysisTask.getLogs().get(index);
                    jdbcTemplate.update("""
                                    INSERT INTO gv_analysis_task_log (
                                        task_id, seq_no, log_at, stage, level, message, source
                                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                                    """,
                            analysisTask.getTaskId(),
                            index + 1,
                            log.timestamp(),
                            log.stage(),
                            log.level().name(),
                            log.message(),
                            log.source()
                    );
                });
    }

    private List<TaskLogEntry> findLogsByTaskId(String taskId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_analysis_task_log
                        WHERE task_id = ?
                        ORDER BY seq_no ASC
                        """,
                (rs, rowNum) -> new TaskLogEntry(
                        rs.getObject("log_at", OffsetDateTime.class),
                        rs.getString("stage"),
                        TaskLogLevel.valueOf(rs.getString("level")),
                        rs.getString("message"),
                        rs.getString("source")
                ),
                taskId
        );
    }

    private List<CandidateProduct> findCandidatesByTaskId(String taskId) {
        return jdbcTemplate.query("""
                        SELECT * FROM gv_analysis_candidate
                        WHERE task_id = ?
                        ORDER BY created_at ASC, external_item_id ASC
                        """,
                (rs, rowNum) -> new CandidateProduct(
                        rs.getString("external_item_id"),
                        rs.getString("title"),
                        rs.getString("image"),
                        rs.getString("platform"),
                        rs.getBigDecimal("price"),
                        rs.getBigDecimal("score"),
                        rs.getString("risk_tag"),
                        rs.getString("recommendation_reason"),
                        rs.getBoolean("suggest_second_phase")
                ),
                taskId
        );
    }

    private String jsonText(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : value.toString();
    }

    private String resolveSelectedProductId(ResultSet rs) throws SQLException {
        String selectedProductId = rs.getString("selected_product_id");
        return selectedProductId != null ? selectedProductId : rs.getString("selected_candidate_id");
    }

    private <T> T defaultIfNull(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
