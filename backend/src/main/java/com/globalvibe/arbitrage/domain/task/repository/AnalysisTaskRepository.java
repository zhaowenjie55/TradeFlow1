package com.globalvibe.arbitrage.domain.task.repository;

import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;

import java.util.List;
import java.util.Optional;

public interface AnalysisTaskRepository {

    AnalysisTask save(AnalysisTask analysisTask);

    Optional<AnalysisTask> findById(String taskId);

    List<AnalysisTask> findAll();
}
