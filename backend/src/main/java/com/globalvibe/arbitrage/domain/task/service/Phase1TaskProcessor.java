package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.ai.workflow.Phase1Workflow;
import com.globalvibe.arbitrage.ai.workflow.Phase1WorkflowResult;
import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.candidate.service.CandidateSnapshotService;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.search.model.SearchRun;
import com.globalvibe.arbitrage.domain.search.model.SearchRunResult;
import com.globalvibe.arbitrage.domain.search.model.SearchRunStatus;
import com.globalvibe.arbitrage.domain.search.repository.SearchRunRepository;
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

@Service
public class Phase1TaskProcessor {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase1Workflow phase1Workflow;
    private final TaskExecutionProperties taskExecutionProperties;
    private final CandidateSnapshotService candidateSnapshotService;
    private final SearchRunRepository searchRunRepository;

    public Phase1TaskProcessor(
            AnalysisTaskRepository analysisTaskRepository,
            Phase1Workflow phase1Workflow,
            TaskExecutionProperties taskExecutionProperties,
            CandidateSnapshotService candidateSnapshotService,
            SearchRunRepository searchRunRepository
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase1Workflow = phase1Workflow;
        this.taskExecutionProperties = taskExecutionProperties;
        this.candidateSnapshotService = candidateSnapshotService;
        this.searchRunRepository = searchRunRepository;
    }

    @Async(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public void processAsync(String taskId) {
        AnalysisTask analysisTask = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));
        SearchRun searchRun = createSearchRun(analysisTask);

        updateStatus(analysisTask, TaskStatus.RUNNING, "phase1.queue", "任务已出队，开始执行海外盘点。");
        pause();

        try {
            Phase1WorkflowResult workflowResult = phase1Workflow.run(analysisTask);
            analysisTask.getLogs().addAll(workflowResult.logs());
            analysisTask.setCandidates(workflowResult.candidates());
            analysisTask.setStatus(TaskStatus.WAITING_USER_SELECTION);
            analysisTask.setUpdatedAt(OffsetDateTime.now());
            candidateSnapshotService.replaceForTask(analysisTask);
            analysisTaskRepository.save(analysisTask);
            finalizeSearchRun(searchRun, workflowResult, analysisTask);
        } catch (RuntimeException ex) {
            markSearchRunFailed(searchRun, ex.getMessage());
            markTaskFailed(analysisTask, "phase1.failed", "海外盘点执行失败: " + ex.getMessage());
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
                toSearchRunResults(searchRun.searchRunId(), workflowResult.sourceProducts())
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
        analysisTask.setStatus(TaskStatus.FAILED);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                TaskLogLevel.ERROR,
                message,
                "phase1-processor"
        ));
        analysisTaskRepository.save(analysisTask);
    }

    private List<SearchRunResult> toSearchRunResults(String searchRunId, List<Product> products) {
        return java.util.stream.IntStream.range(0, products.size())
                .mapToObj(index -> {
                    Product product = products.get(index);
                    return SearchRunResult.builder()
                            .searchRunId(searchRunId)
                            .platform(product.platform().value())
                            .externalItemId(product.id())
                            .rankNo(index + 1)
                            .title(product.title())
                            .price(product.price())
                            .image(product.image())
                            .link(product.link())
                            .rawData(product.rawData())
                            .build();
                })
                .toList();
    }
}
