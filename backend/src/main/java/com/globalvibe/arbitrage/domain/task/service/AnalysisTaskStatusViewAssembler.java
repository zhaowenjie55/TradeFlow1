package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.candidate.dto.CandidateVO;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import com.globalvibe.arbitrage.domain.report.service.ReportAggregateService;
import com.globalvibe.arbitrage.domain.report.service.ReportViewAssembler;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.dto.Phase1TaskStatusResponse;
import com.globalvibe.arbitrage.domain.task.dto.Phase2TaskStatusResponse;
import com.globalvibe.arbitrage.domain.task.dto.PipelineStepVO;
import com.globalvibe.arbitrage.domain.task.dto.TaskLogVO;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisTaskStatusViewAssembler {

    private final ReportViewAssembler reportViewAssembler;
    private final ReportAggregateService reportAggregateService;

    public AnalysisTaskStatusViewAssembler(ReportViewAssembler reportViewAssembler, ReportAggregateService reportAggregateService) {
        this.reportViewAssembler = reportViewAssembler;
        this.reportAggregateService = reportAggregateService;
    }

    public Phase1TaskStatusResponse toPhase1Response(AnalysisTask analysisTask) {
        return new Phase1TaskStatusResponse(
                analysisTask.getTaskId(),
                analysisTask.getPhase(),
                analysisTask.getStatus(),
                resolveStage(analysisTask),
                resolveProgress(analysisTask),
                hasFallbackTriggered(analysisTask),
                analysisTask.getKeyword(),
                analysisTask.getMarket(),
                analysisTask.getTargetProfitMargin(),
                analysisTask.getMode(),
                analysisTask.getCreatedAt(),
                analysisTask.getUpdatedAt(),
                analysisTask.getLogs().stream().map(this::toLogView).toList(),
                buildPhase1Pipeline(analysisTask),
                analysisTask.getCandidates().stream().map(this::toCandidateView).toList()
        );
    }

    public Phase2TaskStatusResponse toPhase2Response(AnalysisTask analysisTask) {
        ReportDetailVO report = reportAggregateService.findByTaskId(analysisTask.getTaskId())
                .map(reportViewAssembler::toDetail)
                .orElse(null);
        return new Phase2TaskStatusResponse(
                analysisTask.getTaskId(),
                analysisTask.getPhase(),
                analysisTask.getStatus(),
                resolveStage(analysisTask),
                resolveProgress(analysisTask),
                hasFallbackTriggered(analysisTask),
                analysisTask.getParentTaskId(),
                analysisTask.getSelectedProductId(),
                analysisTask.getReportId(),
                analysisTask.getMode(),
                analysisTask.getCreatedAt(),
                analysisTask.getUpdatedAt(),
                analysisTask.getLogs().stream().map(this::toLogView).toList(),
                buildPhase2Pipeline(analysisTask),
                report
        );
    }

    private TaskLogVO toLogView(TaskLogEntry taskLogEntry) {
        return new TaskLogVO(
                taskLogEntry.timestamp(),
                taskLogEntry.stage(),
                taskLogEntry.level().name(),
                taskLogEntry.message(),
                taskLogEntry.source()
        );
    }

    private CandidateVO toCandidateView(CandidateProduct candidateProduct) {
        return new CandidateVO(
                candidateProduct.productId(),
                candidateProduct.title(),
                candidateProduct.imageUrl(),
                candidateProduct.market(),
                candidateProduct.overseasPrice(),
                candidateProduct.estimatedMargin(),
                candidateProduct.riskTag(),
                candidateProduct.recommendationReason(),
                candidateProduct.suggestSecondPhase()
        );
    }

    private List<PipelineStepVO> buildPhase1Pipeline(AnalysisTask analysisTask) {
        return List.of(
                pipelineStep("phase1-create", "任务创建", pipelineStatus(analysisTask,
                        List.of(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.WAITING_USER_SELECTION, TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.CREATED))),
                pipelineStep("phase1-scan", "海外盘点", pipelineStatus(analysisTask,
                        List.of(TaskStatus.WAITING_USER_SELECTION, TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.RUNNING, TaskStatus.FALLBACK_MOCK))),
                pipelineStep("phase1-selection", "候选输出", pipelineStatus(analysisTask,
                        List.of(TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.WAITING_USER_SELECTION)))
        );
    }

    private List<PipelineStepVO> buildPhase2Pipeline(AnalysisTask analysisTask) {
        return List.of(
                pipelineStep("phase2-create", "任务创建", pipelineStatus(analysisTask,
                        List.of(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.WAITING_1688_VERIFICATION, TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.CREATED))),
                pipelineStep("phase2-match", "货源匹配", pipelineStatus(analysisTask,
                        List.of(TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.RUNNING, TaskStatus.FALLBACK_MOCK, TaskStatus.WAITING_1688_VERIFICATION))),
                pipelineStep("phase2-pricing", "利润测算", pipelineStatus(analysisTask,
                        List.of(TaskStatus.REPORT_READY, TaskStatus.FAILED),
                        List.of(TaskStatus.ANALYZING_SOURCE))),
                pipelineStep("phase2-report", "报告生成", pipelineStatus(analysisTask,
                        List.of(TaskStatus.REPORT_READY),
                        List.of()))
        );
    }

    private PipelineStepVO pipelineStep(String key, String title, String status) {
        return new PipelineStepVO(key, title, status);
    }

    private boolean isComplete(AnalysisTask analysisTask, TaskStatus... statuses) {
        for (TaskStatus status : statuses) {
            if (analysisTask.getStatus() == status) {
                return true;
            }
        }
        return false;
    }

    private boolean isCurrent(AnalysisTask analysisTask, TaskStatus... statuses) {
        for (TaskStatus status : statuses) {
            if (analysisTask.getStatus() == status) {
                return true;
            }
        }
        return false;
    }

    private String pipelineStatus(AnalysisTask analysisTask, List<TaskStatus> completedStatuses, List<TaskStatus> currentStatuses) {
        if (completedStatuses.contains(analysisTask.getStatus())) {
            return "completed";
        }
        if (currentStatuses.contains(analysisTask.getStatus())) {
            return "current";
        }
        return "pending";
    }

    private String resolveStage(AnalysisTask analysisTask) {
        return switch (analysisTask.getStatus()) {
            case CREATED, QUEUED -> analysisTask.getPhase().name().toLowerCase() + ".create";
            case RUNNING -> analysisTask.getPhase() == TaskPhase.PHASE1
                    ? "phase1.market-scan"
                    : "phase2.domestic-match";
            case FALLBACK_MOCK -> analysisTask.getPhase() == TaskPhase.PHASE1
                    ? "phase1.market-scan"
                    : "phase2.domestic-match";
            case WAITING_USER_SELECTION -> "phase1.output";
            case WAITING_1688_VERIFICATION -> "phase2.verification";
            case ANALYZING_SOURCE -> "phase2.pricing";
            case REPORT_READY -> "phase2.report";
            case FAILED -> analysisTask.getPhase().name().toLowerCase() + ".failed";
        };
    }

    private int resolveProgress(AnalysisTask analysisTask) {
        return switch (analysisTask.getStatus()) {
            case CREATED, QUEUED -> analysisTask.getPhase() == TaskPhase.PHASE1 ? 10 : 8;
            case RUNNING -> analysisTask.getPhase() == TaskPhase.PHASE1 ? 42 : 44;
            case FALLBACK_MOCK -> analysisTask.getPhase() == TaskPhase.PHASE1 ? 42 : 44;
            case WAITING_USER_SELECTION, REPORT_READY -> 100;
            case WAITING_1688_VERIFICATION -> 52;
            case ANALYZING_SOURCE -> 76;
            case FAILED -> 100;
        };
    }

    private boolean hasFallbackTriggered(AnalysisTask analysisTask) {
        return analysisTask.getStatus() == TaskStatus.FALLBACK_MOCK
                || analysisTask.getLogs().stream().anyMatch(log -> log.stage().endsWith(".fallback"));
    }
}
