package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase2CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class Phase2TaskApplicationService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase2TaskProcessor phase2TaskProcessor;
    private final AnalysisTaskFactory analysisTaskFactory;

    public Phase2TaskApplicationService(
            AnalysisTaskRepository analysisTaskRepository,
            Phase2TaskProcessor phase2TaskProcessor,
            AnalysisTaskFactory analysisTaskFactory
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase2TaskProcessor = phase2TaskProcessor;
        this.analysisTaskFactory = analysisTaskFactory;
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
}
