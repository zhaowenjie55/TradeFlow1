package com.globalvibe.arbitrage.domain.candidate.service;

import com.globalvibe.arbitrage.domain.candidate.dto.CandidateListResponse;
import com.globalvibe.arbitrage.domain.candidate.dto.CandidateSnapshotVO;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateViewAssembler {

    public CandidateListResponse toListResponse(String taskId, List<CandidateSnapshot> snapshots) {
        return new CandidateListResponse(
                taskId,
                snapshots.size(),
                snapshots.stream().map(this::toView).toList()
        );
    }

    private CandidateSnapshotVO toView(CandidateSnapshot snapshot) {
        return new CandidateSnapshotVO(
                snapshot.taskId(),
                snapshot.productId(),
                snapshot.title(),
                snapshot.imageUrl(),
                snapshot.market(),
                snapshot.overseasPrice(),
                snapshot.estimatedMargin(),
                snapshot.riskTag(),
                snapshot.recommendationReason(),
                snapshot.suggestSecondPhase(),
                snapshot.createdAt()
        );
    }
}
