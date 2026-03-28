package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.ai.workflow.Phase2Workflow;
import com.globalvibe.arbitrage.ai.workflow.Phase2WorkflowResult;
import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.report.service.ReportAggregateService;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class Phase2TaskProcessor {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase2Workflow phase2Workflow;
    private final TaskExecutionProperties taskExecutionProperties;
    private final ReportAggregateService reportAggregateService;

    public Phase2TaskProcessor(
            AnalysisTaskRepository analysisTaskRepository,
            Phase2Workflow phase2Workflow,
            TaskExecutionProperties taskExecutionProperties,
            ReportAggregateService reportAggregateService
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase2Workflow = phase2Workflow;
        this.taskExecutionProperties = taskExecutionProperties;
        this.reportAggregateService = reportAggregateService;
    }

    @Async(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public void processAsync(String taskId) {
        AnalysisTask phase2Task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));

        updateStatus(phase2Task, TaskStatus.RUNNING, "phase2.queue", "二次分析已开始，准备抓取国内货源。");
        pause();
        updateStatus(phase2Task, TaskStatus.ANALYZING_SOURCE, "phase2.domestic-match", "开始执行国内货源匹配与 ROI 试算。");
        pause();

        AnalysisTask phase1Task = analysisTaskRepository.findById(phase2Task.getParentTaskId())
                .orElseThrow(() -> new AnalysisTaskNotFoundException(phase2Task.getParentTaskId()));

        try {
            Phase2WorkflowResult workflowResult = phase2Workflow.run(phase2Task, phase1Task, phase2Task.getSelectedProductId());
            phase2Task.getLogs().addAll(workflowResult.logs());
            phase2Task.setDomesticMatches(workflowResult.candidateMatches().stream()
                    .map(match -> match.externalItemId())
                    .toList());
            phase2Task.setReportId(workflowResult.report().reportId());
            phase2Task.setStatus(TaskStatus.REPORT_READY);
            phase2Task.setUpdatedAt(OffsetDateTime.now());
            analysisTaskRepository.save(phase2Task);
            reportAggregateService.save(phase2Task.getTaskId(), workflowResult.report());
        } catch (RuntimeException ex) {
            markFailed(phase2Task, "phase2.failed", "二次分析执行失败: " + ex.getMessage());
        }
    }

    private void updateStatus(AnalysisTask analysisTask, TaskStatus status, String stage, String message) {
        analysisTask.setStatus(status);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                TaskLogLevel.INFO,
                message,
                "phase2-processor"
        ));
        analysisTaskRepository.save(analysisTask);
    }

    private void pause() {
        try {
            Thread.sleep(taskExecutionProperties.getProcessingDelayMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void markFailed(AnalysisTask analysisTask, String stage, String message) {
        analysisTask.setStatus(TaskStatus.FAILED);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                TaskLogLevel.ERROR,
                message,
                "phase2-processor"
        ));
        analysisTaskRepository.save(analysisTask);
    }
}
