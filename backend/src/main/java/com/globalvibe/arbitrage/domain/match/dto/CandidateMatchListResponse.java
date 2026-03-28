package com.globalvibe.arbitrage.domain.match.dto;

import java.util.List;

public record CandidateMatchListResponse(
        List<CandidateMatchVO> items
) {
}
