package com.globalvibe.arbitrage.domain.match.service;

import com.globalvibe.arbitrage.domain.match.dto.CandidateMatchListResponse;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.repository.CandidateMatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidateMatchQueryService {

    private final CandidateMatchRepository candidateMatchRepository;
    private final CandidateMatchViewAssembler candidateMatchViewAssembler;

    public CandidateMatchQueryService(
            CandidateMatchRepository candidateMatchRepository,
            CandidateMatchViewAssembler candidateMatchViewAssembler
    ) {
        this.candidateMatchRepository = candidateMatchRepository;
        this.candidateMatchViewAssembler = candidateMatchViewAssembler;
    }

    public CandidateMatchListResponse listByCandidateId(String candidateId, int limit) {
        List<CandidateMatchRecord> matches = candidateMatchRepository.findTopByCandidateId(candidateId, limit);
        if (matches.isEmpty()) {
            matches = candidateMatchRepository.findTopBySourceProductId(candidateId, limit);
        }
        return new CandidateMatchListResponse(
                matches.stream()
                        .map(candidateMatchViewAssembler::toView)
                        .toList()
        );
    }

    public CandidateMatchListResponse listByTaskId(String taskId, int limitPerCandidate) {
        return new CandidateMatchListResponse(
                candidateMatchRepository.findTopByTaskId(taskId, limitPerCandidate).stream()
                        .map(candidateMatchViewAssembler::toView)
                        .toList()
        );
    }
}
