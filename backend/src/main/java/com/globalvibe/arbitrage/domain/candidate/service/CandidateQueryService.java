package com.globalvibe.arbitrage.domain.candidate.service;

import com.globalvibe.arbitrage.domain.candidate.dto.CandidateListResponse;
import com.globalvibe.arbitrage.domain.candidate.repository.CandidateSnapshotRepository;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.repository.AnalysisTaskRepository;
import com.globalvibe.arbitrage.domain.task.service.AnalysisTaskNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CandidateQueryService {

    private final CandidateSnapshotRepository candidateSnapshotRepository;
    private final AnalysisTaskRepository analysisTaskRepository;
    private final CandidateSnapshotService candidateSnapshotService;
    private final CandidateViewAssembler candidateViewAssembler;

    public CandidateQueryService(
            CandidateSnapshotRepository candidateSnapshotRepository,
            AnalysisTaskRepository analysisTaskRepository,
            CandidateSnapshotService candidateSnapshotService,
            CandidateViewAssembler candidateViewAssembler
    ) {
        this.candidateSnapshotRepository = candidateSnapshotRepository;
        this.analysisTaskRepository = analysisTaskRepository;
        this.candidateSnapshotService = candidateSnapshotService;
        this.candidateViewAssembler = candidateViewAssembler;
    }

    public CandidateListResponse listByTaskId(String taskId) {
        AnalysisTask analysisTask = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new AnalysisTaskNotFoundException(taskId));

        if (candidateSnapshotRepository.findByTaskId(taskId).isEmpty() && !analysisTask.getCandidates().isEmpty()) {
            candidateSnapshotService.replaceForTask(analysisTask);
        }

        return candidateViewAssembler.toListResponse(taskId, candidateSnapshotRepository.findByTaskId(taskId));
    }
}
