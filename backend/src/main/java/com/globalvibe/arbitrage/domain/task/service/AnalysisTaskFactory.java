package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.model.TaskConstraint;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class AnalysisTaskFactory {

    private final TaskExecutionProperties taskExecutionProperties;

    public AnalysisTaskFactory(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    public AnalysisTask createPhase1Task(Phase1CreateTaskRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        String taskId = "phase1-" + UUID.randomUUID();
        TaskMode mode = request.mode() != null ? request.mode() : taskExecutionProperties.getDefaultMode();

        AnalysisTask analysisTask = AnalysisTask.builder()
                .taskId(taskId)
                .phase(TaskPhase.PHASE1)
                .status(TaskStatus.CREATED)
                .keyword(request.keyword())
                .market(request.market())
                .requestedLimit(request.limit())
                .constraints(toConstraints(request))
                .targetProfitMargin(request.targetProfitMargin())
                .mode(mode)
                .createdAt(now)
                .updatedAt(now)
                .build();

        analysisTask.getLogs().add(new TaskLogEntry(
                now,
                "phase1.create",
                TaskLogLevel.INFO,
                "Phase1 任务已创建并进入队列。",
                "phase1-api"
        ));
        return analysisTask;
    }

    public AnalysisTask createPhase2Task(AnalysisTask phase1Task, String selectedProductId) {
        OffsetDateTime now = OffsetDateTime.now();
        AnalysisTask phase2Task = AnalysisTask.builder()
                .taskId("phase2-" + UUID.randomUUID())
                .parentTaskId(phase1Task.getTaskId())
                .phase(TaskPhase.PHASE2)
                .status(TaskStatus.CREATED)
                .keyword(phase1Task.getKeyword())
                .market(phase1Task.getMarket())
                .requestedLimit(phase1Task.getRequestedLimit())
                .constraints(phase1Task.getConstraints())
                .targetProfitMargin(phase1Task.getTargetProfitMargin())
                .selectedProductId(selectedProductId)
                .mode(phase1Task.getMode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        phase2Task.getLogs().add(new TaskLogEntry(
                now,
                "phase2.create",
                TaskLogLevel.INFO,
                "Phase2 任务已创建，开始针对选中商品执行深度寻源。",
                "phase2-api"
        ));
        return phase2Task;
    }

    private List<TaskConstraint> toConstraints(Phase1CreateTaskRequest request) {
        if (request.constraints() == null || request.constraints().isEmpty()) {
            return List.of();
        }
        return request.constraints().stream()
                .map(constraint -> new TaskConstraint(
                        constraint.field(),
                        constraint.operator(),
                        constraint.value()
                ))
                .toList();
    }
}
