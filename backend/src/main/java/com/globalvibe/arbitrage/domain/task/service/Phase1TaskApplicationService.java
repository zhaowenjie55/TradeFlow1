package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class Phase1TaskApplicationService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase1TaskProcessor phase1TaskProcessor;
    private final AnalysisTaskFactory analysisTaskFactory;
    private final TaskStatusTransitionPolicy taskStatusTransitionPolicy;

    public Phase1TaskApplicationService(
            AnalysisTaskRepository analysisTaskRepository,
            Phase1TaskProcessor phase1TaskProcessor,
            AnalysisTaskFactory analysisTaskFactory,
            TaskStatusTransitionPolicy taskStatusTransitionPolicy
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase1TaskProcessor = phase1TaskProcessor;
        this.analysisTaskFactory = analysisTaskFactory;
        this.taskStatusTransitionPolicy = taskStatusTransitionPolicy;
    }

    public Phase1CreateTaskResponse retryTask(String taskId) {
        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));

        if (task.getPhase() != TaskPhase.PHASE1) {
            throw new InvalidTaskContextException("当前任务不是一阶段任务，无法重试。");
        }
        if (task.getStatus() != TaskStatus.FAILED) {
            throw new InvalidTaskContextException("当前任务不处于失败状态，无法重试。");
        }

        taskStatusTransitionPolicy.assertAllowed(task.getStatus(), TaskStatus.QUEUED);
        task.setStatus(TaskStatus.QUEUED);
        task.setUpdatedAt(OffsetDateTime.now());
        task.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                "phase1.retry",
                TaskLogLevel.INFO,
                "任务重试已发起，正在重新执行一阶段海外候选检索。",
                "phase1-task-application-service"
        ));
        analysisTaskRepository.save(task);
        phase1TaskProcessor.processAsync(task.getTaskId());

        return new Phase1CreateTaskResponse(
                task.getTaskId(),
                task.getPhase(),
                task.getStatus(),
                task.getMode(),
                task.getCreatedAt()
        );
    }

    public Phase1CreateTaskResponse createTask(Phase1CreateTaskRequest request) {
        AnalysisTask analysisTask = analysisTaskFactory.createPhase1Task(request);

        analysisTaskRepository.save(analysisTask);
        phase1TaskProcessor.processAsync(analysisTask.getTaskId());
        return new Phase1CreateTaskResponse(
                analysisTask.getTaskId(),
                analysisTask.getPhase(),
                analysisTask.getStatus(),
                analysisTask.getMode(),
                analysisTask.getCreatedAt()
        );
    }
}
