package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalysisTaskStatusQueryService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final AnalysisTaskStatusViewAssembler analysisTaskStatusViewAssembler;

    public AnalysisTaskStatusQueryService(
            AnalysisTaskRepository analysisTaskRepository,
            AnalysisTaskStatusViewAssembler analysisTaskStatusViewAssembler
    ) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.analysisTaskStatusViewAssembler = analysisTaskStatusViewAssembler;
    }

    public Object getTaskStatus(String taskId) {
        AnalysisTask analysisTask = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));
        return analysisTask.getPhase() == TaskPhase.PHASE1
                ? analysisTaskStatusViewAssembler.toPhase1Response(analysisTask)
                : analysisTaskStatusViewAssembler.toPhase2Response(analysisTask);
    }
}
