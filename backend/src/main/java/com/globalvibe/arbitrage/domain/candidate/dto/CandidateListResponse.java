package com.globalvibe.arbitrage.domain.candidate.dto;

import java.util.List;

public record CandidateListResponse(
        String taskId,
        int total,
        List<CandidateSnapshotVO> items
) {
}
