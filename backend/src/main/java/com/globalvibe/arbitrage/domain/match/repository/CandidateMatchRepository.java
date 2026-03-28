package com.globalvibe.arbitrage.domain.match.repository;

import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;

import java.util.List;

public interface CandidateMatchRepository {

    void replaceForCandidate(String candidateId, List<CandidateMatchRecord> matches);

    List<CandidateMatchRecord> findTopByCandidateId(String candidateId, int limit);

    List<CandidateMatchRecord> findTopByTaskId(String taskId, int limitPerCandidate);

    List<CandidateMatchRecord> findTopBySourceProductId(String sourceProductId, int limit);
}
