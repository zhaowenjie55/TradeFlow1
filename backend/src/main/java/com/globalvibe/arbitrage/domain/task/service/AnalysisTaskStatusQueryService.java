package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AnalysisTaskStatusQueryService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final AnalysisTaskStatusViewAssembler analysisTaskStatusViewAssembler;
    private final TaskExecutionProperties taskExecutionProperties;
    private final TaskStatusTransitionPolicy taskStatusTransitionPolicy;

    public AnalysisTaskStatusQueryService(
            AnalysisTaskRepository analysisTaskRepository,
            AnalysisTaskStatusViewAssembler analysisTaskStatusViewAssembler,
            TaskExecutionProperties taskExecutionProperties,
            TaskStatusTransitionPolicy taskStatusTransitionPolicy
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.analysisTaskStatusViewAssembler = analysisTaskStatusViewAssembler;
        this.taskExecutionProperties = taskExecutionProperties;
        this.taskStatusTransitionPolicy = taskStatusTransitionPolicy;
    }

    public Object getTaskStatus(String taskId) {
        AnalysisTask analysisTask = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));
        AnalysisTask recovered = recoverIfStale(analysisTask);
        return recovered.getPhase() == TaskPhase.PHASE1
                ? analysisTaskStatusViewAssembler.toPhase1Response(recovered)
                : analysisTaskStatusViewAssembler.toPhase2Response(recovered);
    }

    private AnalysisTask recoverIfStale(AnalysisTask analysisTask) {
        if (analysisTask.getPhase() != TaskPhase.PHASE1 || analysisTask.getStatus() != TaskStatus.RUNNING) {
            return analysisTask;
        }
        long timeoutMillis = Math.max(1_000L, taskExecutionProperties.getPhase1WorkflowTimeoutMillis()) * 2;
        OffsetDateTime updatedAt = analysisTask.getUpdatedAt();
        if (updatedAt == null || updatedAt.isAfter(OffsetDateTime.now().minusNanos(timeoutMillis * 1_000_000))) {
            return analysisTask;
        }
        taskStatusTransitionPolicy.assertAllowed(analysisTask.getStatus(), TaskStatus.FAILED);
        analysisTask.setStatus(TaskStatus.FAILED);
        analysisTask.setUpdatedAt(OffsetDateTime.now());
        analysisTask.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                "phase1.timeout.recovered",
                TaskLogLevel.ERROR,
                "检测到一阶段任务长时间停留在 RUNNING，系统已将其标记为失败。请重新发起检索。",
                "phase1-status-query"
        ));
        return analysisTaskRepository.save(analysisTask);
    }
}
