package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.dto.TaskHistoryItemResponse;
import com.globalvibe.arbitrage.domain.task.dto.TaskHistoryResponse;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalysisTaskHistoryQueryService {

    private final AnalysisTaskRepository analysisTaskRepository;

    public AnalysisTaskHistoryQueryService(AnalysisTaskRepository analysisTaskRepository) {
        this.analysisTaskRepository = analysisTaskRepository;
    }

    public TaskHistoryResponse getHistory() {
        return new TaskHistoryResponse(
                analysisTaskRepository.findAll().stream()
                        .filter(task -> task.getPhase() == TaskPhase.PHASE1)
                        .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                        .map(this::toItem)
                        .toList()
        );
    }

    private TaskHistoryItemResponse toItem(AnalysisTask task) {
        return new TaskHistoryItemResponse(
                task.getTaskId(),
                task.getKeyword(),
                task.getMarket(),
                task.getStatus().name(),
                task.getMode().name(),
                task.getCreatedAt()
        );
    }
}
