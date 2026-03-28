package com.globalvibe.arbitrage.domain.task.model;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisTask {

    private String taskId;
    private String parentTaskId;
    private TaskPhase phase;
    private TaskStatus status;
    private String keyword;
    @Builder.Default
    private List<TaskConstraint> constraints = new ArrayList<>();
    private String market;
    private Integer requestedLimit;
    private BigDecimal targetProfitMargin;
    private String selectedProductId;
    @Builder.Default
    private List<TaskLogEntry> logs = new ArrayList<>();
    @Builder.Default
    private List<CandidateProduct> candidates = new ArrayList<>();
    @Builder.Default
    private List<String> domesticMatches = new ArrayList<>();
    private String reportId;
    private ArbitrageReport report;
    private TaskMode mode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
