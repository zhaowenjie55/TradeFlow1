package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.ai.workflow.Phase1Workflow;
import com.globalvibe.arbitrage.ai.workflow.Phase1WorkflowFailedException;
import com.globalvibe.arbitrage.ai.workflow.Phase1WorkflowResult;
import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.candidate.service.CandidateSnapshotService;
import com.globalvibe.arbitrage.domain.search.model.SearchRun;
import com.globalvibe.arbitrage.domain.search.model.SearchRunStatus;
import com.globalvibe.arbitrage.domain.search.repository.SearchRunRepository;
import com.globalvibe.arbitrage.domain.search.service.SearchRunResultDeduplicator;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class Phase1TaskProcessor {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase1Workflow phase1Workflow;
    private final TaskExecutionProperties taskExecutionProperties;
    private final CandidateSnapshotService candidateSnapshotService;
    private final SearchRunRepository searchRunRepository;
    private final TaskStatusTransitionPolicy taskStatusTransitionPolicy;

    public Phase1TaskProcessor(
            AnalysisTaskRepository analysisTaskRepository,
            Phase1Workflow phase1Workflow,
            TaskExecutionProperties taskExecutionProperties,
            CandidateSnapshotService candidateSnapshotService,
            SearchRunRepository searchRunRepository,
            TaskStatusTransitionPolicy taskStatusTransitionPolicy
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase1Workflow = phase1Workflow;
        this.taskExecutionProperties = taskExecutionProperties;
        this.candidateSnapshotService = candidateSnapshotService;
        this.searchRunRepository = searchRunRepository;
        this.taskStatusTransitionPolicy = taskStatusTransitionPolicy;
    }

    @Async(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public void processAsync(String taskId) {
        AnalysisTask analysisTask = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));
        SearchRun searchRun = createSearchRun(analysisTask);

        updateStatus(analysisTask, TaskStatus.RUNNING, "phase1.queue", "任务已开始执行，准备进行 Amazon 海外盘点。");
        pause();

        try {
            Phase1WorkflowResult workflowResult = runWorkflowWithTimeout(analysisTask);
            analysisTask.getLogs().addAll(workflowResult.logs());
            analysisTask.setCandidates(workflowResult.candidates());
            taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), TaskStatus.WAITING_USER_SELECTION);
            analysisTask.setStatus(TaskStatus.WAITING_USER_SELECTION);
            analysisTask.setUpdatedAt(OffsetDateTime.now());
            candidateSnapshotService.replaceForTask(analysisTask);
            analysisTaskRepository.save(analysisTask);
            finalizeSearchRun(searchRun, workflowResult, analysisTask);
        } catch (Phase1WorkflowFailedException ex) {
            analysisTask.getLogs().addAll(ex.logs());
            markSearchRunFailed(searchRun, ex.getMessage());
            markTaskFailed(analysisTask, "phase1.failed", "海外盘点执行失败: " + ex.getMessage());
        } catch (RuntimeException ex) {
            markSearchRunFailed(searchRun, ex.getMessage());
            markTaskFailed(analysisTask, "phase1.failed", "海外盘点执行失败: " + ex.getMessage());
        } catch (Throwable ex) {
            markSearchRunFailed(searchRun, ex.getMessage());
            markTaskFailed(analysisTask, "phase1.failed", "海外盘点执行异常终止: " + ex.getMessage());
        }
    }

    private Phase1WorkflowResult runWorkflowWithTimeout(AnalysisTask analysisTask) {
        long timeoutMillis = Math.max(1_000L, taskExecutionProperties.getPhase1WorkflowTimeoutMillis());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Phase1WorkflowResult> future = executor.submit(() -> phase1Workflow.run(analysisTask));
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            throw new Phase1WorkflowFailedException(
                    "Amazon 一阶段检索超时（>" + timeoutMillis + "ms）。请检查 crawler 负载或缩短返回载荷后重试。",
                    List.of(buildLog("phase1.timeout", TaskLogLevel.ERROR,
                            "一阶段工作流执行超时，系统已中止本次 Amazon 候选检索。"))
            );
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Phase1WorkflowFailedException workflowFailedException) {
                throw workflowFailedException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause == null ? ex.getMessage() : cause.getMessage(), cause == null ? ex : cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Phase1WorkflowFailedException(
                    "Amazon 一阶段检索被中断，请重试。",
                    List.of(buildLog("phase1.interrupted", TaskLogLevel.ERROR,
                            "一阶段工作流在执行过程中被中断，已提前结束。"))
            );
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
                "phase1-processor"
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

    private SearchRun createSearchRun(AnalysisTask analysisTask) {
        SearchRun searchRun = SearchRun.builder()
                .searchRunId("sr-" + UUID.randomUUID())
                .taskId(analysisTask.getTaskId())
                .phase(analysisTask.getPhase().name())
                .platform(analysisTask.getMarket())
                .queryText(analysisTask.getKeyword())
                .status(SearchRunStatus.RUNNING)
                .fallbackUsed(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        searchRunRepository.save(searchRun);
        return searchRun;
    }

    private void finalizeSearchRun(SearchRun searchRun, Phase1WorkflowResult workflowResult, AnalysisTask analysisTask) {
        SearchRun finalized = SearchRun.builder()
                .searchRunId(searchRun.searchRunId())
                .taskId(searchRun.taskId())
                .phase(searchRun.phase())
                .platform(searchRun.platform())
                .queryText(searchRun.queryText())
                .status(workflowResult.fallbackUsed() ? SearchRunStatus.FALLBACK : SearchRunStatus.SUCCEEDED)
                .fallbackUsed(workflowResult.fallbackUsed())
                .createdAt(searchRun.createdAt())
                .updatedAt(analysisTask.getUpdatedAt())
                .build();
        searchRunRepository.save(finalized);
        searchRunRepository.replaceResults(
                searchRun.searchRunId(),
                SearchRunResultDeduplicator.fromProducts(searchRun.searchRunId(), workflowResult.sourceProducts())
        );
    }

    private void markSearchRunFailed(SearchRun searchRun, String errorMessage) {
        searchRunRepository.save(SearchRun.builder()
                .searchRunId(searchRun.searchRunId())
                .taskId(searchRun.taskId())
                .phase(searchRun.phase())
                .platform(searchRun.platform())
                .queryText(searchRun.queryText())
                .status(SearchRunStatus.FAILED)
                .fallbackUsed(false)
                .errorMessage(errorMessage)
                .createdAt(searchRun.createdAt())
                .updatedAt(OffsetDateTime.now())
                .build());
    }

    private void markTaskFailed(AnalysisTask analysisTask, String stage, String message) {
        taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), TaskStatus.FAILED);
        analysisTask.setStatus(TaskStatus.FAILED);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(buildLog(stage, TaskLogLevel.ERROR, message));
        analysisTaskRepository.save(analysisTask);
    }

    private TaskLogEntry buildLog(String stage, TaskLogLevel level, String message) {
        return new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                level,
                message,
                "phase1-processor"
        );
    }

}
