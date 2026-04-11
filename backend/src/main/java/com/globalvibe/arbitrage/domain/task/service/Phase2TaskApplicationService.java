package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class Phase2TaskApplicationService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase2TaskProcessor phase2TaskProcessor;
    private final AnalysisTaskFactory analysisTaskFactory;
    private final TaskStatusTransitionPolicy taskStatusTransitionPolicy;

    public Phase2TaskApplicationService(
            AnalysisTaskRepository analysisTaskRepository,
            Phase2TaskProcessor phase2TaskProcessor,
            AnalysisTaskFactory analysisTaskFactory,
            TaskStatusTransitionPolicy taskStatusTransitionPolicy
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase2TaskProcessor = phase2TaskProcessor;
        this.analysisTaskFactory = analysisTaskFactory;
        this.taskStatusTransitionPolicy = taskStatusTransitionPolicy;
    }

    public Phase2CreateTaskResponse createTask(Phase2CreateTaskRequest request) {
        AnalysisTask phase1Task = analysisTaskRepository.findById(request.phase1TaskId())
                .orElseThrow(() -> new AnalysisTaskNotFoundException(request.phase1TaskId()));

        CandidateProduct selectedProduct = phase1Task.getCandidates().stream()
                .filter(item -> item.productId().equals(request.productId()))
                .findFirst()
                .orElseThrow(() -> new InvalidTaskContextException("所选商品不属于当前任务，请重新发起 Phase1 分析。"));

        AnalysisTask phase2Task = analysisTaskFactory.createPhase2Task(phase1Task, selectedProduct.productId());

        analysisTaskRepository.save(phase2Task);
        phase2TaskProcessor.processAsync(phase2Task.getTaskId());

        return new Phase2CreateTaskResponse(
                phase2Task.getTaskId(),
                phase2Task.getPhase(),
                phase2Task.getStatus(),
                phase2Task.getMode(),
                phase1Task.getTaskId(),
                selectedProduct.productId(),
                phase2Task.getCreatedAt()
        );
    }

    public Phase2CreateTaskResponse resumeTask(String taskId) {
        AnalysisTask phase2Task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));

        if (phase2Task.getPhase() != TaskPhase.PHASE2) {
            throw new InvalidTaskContextException("当前任务不是二阶段任务，无法继续抓取。");
        }
        if (phase2Task.getStatus() != TaskStatus.WAITING_1688_VERIFICATION) {
            throw new InvalidTaskContextException("当前任务不处于等待 1688 验证状态，无法继续抓取。");
        }

        taskStatusTransitionPolicy.assertAllowed(phase2Task.getStatus(), TaskStatus.QUEUED);
        phase2Task.setStatus(TaskStatus.QUEUED);
        phase2Task.setUpdatedAt(OffsetDateTime.now());
        phase2Task.getLogs().add(new TaskLogEntry(
                OffsetDateTime.now(),
                "phase2.resume",
                TaskLogLevel.INFO,
                "已收到继续抓取指令，正在重新尝试 1688 检索。",
                "phase2-task-application-service"
        ));
        analysisTaskRepository.save(phase2Task);
        phase2TaskProcessor.processAsync(phase2Task.getTaskId());

        return new Phase2CreateTaskResponse(
                phase2Task.getTaskId(),
                phase2Task.getPhase(),
                phase2Task.getStatus(),
                phase2Task.getMode(),
                phase2Task.getParentTaskId(),
                phase2Task.getSelectedProductId(),
                phase2Task.getCreatedAt()
        );
    }
}
