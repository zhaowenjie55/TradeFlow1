package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskRequest;
import com.globalvibe.arbitrage.domain.task.dto.Phase1CreateTaskResponse;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class Phase1TaskApplicationService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final Phase1TaskProcessor phase1TaskProcessor;
    private final AnalysisTaskFactory analysisTaskFactory;

    public Phase1TaskApplicationService(
            AnalysisTaskRepository analysisTaskRepository,
            Phase1TaskProcessor phase1TaskProcessor,
            AnalysisTaskFactory analysisTaskFactory
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.phase1TaskProcessor = phase1TaskProcessor;
        this.analysisTaskFactory = analysisTaskFactory;
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
