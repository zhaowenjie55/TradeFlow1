package com.globalvibe.arbitrage.domain.candidate.service;

import com.globalvibe.arbitrage.domain.candidate.repository.CandidateSnapshotRepository;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateSnapshot;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateSnapshotService {

    private final CandidateSnapshotRepository candidateSnapshotRepository;

    public CandidateSnapshotService(CandidateSnapshotRepository candidateSnapshotRepository) {
        this.candidateSnapshotRepository = candidateSnapshotRepository;
    }

    public void replaceForTask(AnalysisTask analysisTask) {
        List<CandidateSnapshot> snapshots = analysisTask.getCandidates().stream()
                .map(candidate -> CandidateSnapshot.builder()
                        .taskId(analysisTask.getTaskId())
                        .productId(candidate.productId())
                        .title(candidate.title())
                        .imageUrl(candidate.imageUrl())
                        .market(candidate.market())
                        .overseasPrice(candidate.overseasPrice())
                        .estimatedMargin(candidate.estimatedMargin())
                        .riskTag(candidate.riskTag())
                        .recommendationReason(candidate.recommendationReason())
                        .suggestSecondPhase(candidate.suggestSecondPhase())
                        .link(candidate.link())
                        .createdAt(analysisTask.getUpdatedAt())
                        .build())
                .toList();
        candidateSnapshotRepository.replaceForTask(analysisTask.getTaskId(), snapshots);
    }
}
