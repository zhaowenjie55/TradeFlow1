package com.globalvibe.arbitrage.domain.candidate.repository;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateSnapshot;

import java.util.List;

public interface CandidateSnapshotRepository {

    void replaceForTask(String taskId, List<CandidateSnapshot> snapshots);

    List<CandidateSnapshot> findByTaskId(String taskId);
}
