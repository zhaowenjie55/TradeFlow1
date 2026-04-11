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
import com.globalvibe.arbitrage.integration.VerificationRequiredException;
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
    private final TaskStatusTransitionPolicy taskStatusTransitionPolicy;

    public Phase2TaskProcessor(
            AnalysisTaskRepository analysisTaskRepository,
            Phase2Workflow phase2Workflow,
            TaskExecutionProperties taskExecutionProperties,
            ReportAggregateService reportAggregateService,
            TaskStatusTransitionPolicy taskStatusTransitionPolicy
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase2Workflow = phase2Workflow;
        this.taskExecutionProperties = taskExecutionProperties;
        this.reportAggregateService = reportAggregateService;
        this.taskStatusTransitionPolicy = taskStatusTransitionPolicy;
    }

    @Async(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public void processAsync(String taskId) {
        AnalysisTask phase2Task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));

        updateStatus(phase2Task, TaskStatus.RUNNING, "phase2.queue", "二次分析已开始，准备匹配国内货源。");
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
            taskStatusTransitionPolicy.assertAllowed(phase2Task.getStatus(), TaskStatus.REPORT_READY);
            phase2Task.setStatus(TaskStatus.REPORT_READY);
            phase2Task.setUpdatedAt(OffsetDateTime.now());
            analysisTaskRepository.save(phase2Task);
            reportAggregateService.save(phase2Task.getTaskId(), workflowResult.report());
        } catch (VerificationRequiredException ex) {
            markWaitingVerification(phase2Task, ex.getMessage());
        } catch (RuntimeException ex) {
            markFailed(phase2Task, "phase2.failed", "二次分析执行失败: " + ex.getMessage());
        }
    }

    private void updateStatus(AnalysisTask analysisTask, TaskStatus status, String stage, String message) {
        taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), status);
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
        taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), TaskStatus.FAILED);
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

    private void markWaitingVerification(AnalysisTask analysisTask, String message) {
        taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), TaskStatus.WAITING_1688_VERIFICATION);
        analysisTask.setStatus(TaskStatus.WAITING_1688_VERIFICATION);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                "phase2.verification",
                TaskLogLevel.WARN,
                message == null || message.isBlank()
                        ? "检测到 1688 验证，请在浏览器窗口完成登录或滑块验证后点击继续。"
                        : message,
                "phase2-processor"
        ));
        analysisTaskRepository.save(analysisTask);
    }
}
