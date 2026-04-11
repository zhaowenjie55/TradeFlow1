package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class MatchSelectionPolicy {

    public List<CandidateMatchRecord> selectTopMatches(List<CandidateMatchRecord> domesticMatches, int limit) {
        if (domesticMatches == null || domesticMatches.isEmpty()) {
            return List.of();
        }
        return domesticMatches.stream()
                .sorted(Comparator
                        .comparing(CandidateMatchRecord::similarityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CandidateMatchRecord::price, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }
}
