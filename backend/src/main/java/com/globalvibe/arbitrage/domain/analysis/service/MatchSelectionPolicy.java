package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class MatchSelectionPolicy {

    public CandidateMatchRecord selectBenchmark(
            List<CandidateMatchRecord> domesticMatches,
            Map<String, ProductDetailSnapshot> detailSnapshots,
            boolean strictRealtime
    ) {
        if (domesticMatches == null || domesticMatches.isEmpty()) {
            return null;
        }
        Comparator<CandidateMatchRecord> comparator = benchmarkComparator(detailSnapshots);
        List<CandidateMatchRecord> qualified = domesticMatches.stream()
                .filter(match -> isQualifiedBenchmark(match, detailSnapshots, strictRealtime))
                .sorted(comparator)
                .toList();
        if (!qualified.isEmpty()) {
            return qualified.get(0);
        }
        if (strictRealtime) {
            return null;
        }
        return domesticMatches.stream()
                .sorted(comparator)
                .findFirst()
                .orElse(null);
    }

    public List<CandidateMatchRecord> selectTopMatches(
            List<CandidateMatchRecord> domesticMatches,
            Map<String, ProductDetailSnapshot> detailSnapshots,
            int limit
    ) {
        if (domesticMatches == null || domesticMatches.isEmpty()) {
            return List.of();
        }
        return domesticMatches.stream()
                .sorted(benchmarkComparator(detailSnapshots))
                .limit(limit)
                .toList();
    }

    private Comparator<CandidateMatchRecord> benchmarkComparator(Map<String, ProductDetailSnapshot> detailSnapshots) {
        return Comparator
                .comparing((CandidateMatchRecord match) -> isQualifiedBenchmark(match, detailSnapshots, false), Comparator.reverseOrder())
                .thenComparing((CandidateMatchRecord match) -> hasDetailSnapshot(match, detailSnapshots), Comparator.reverseOrder())
                .thenComparing((CandidateMatchRecord match) -> isRealtimeSource(match.matchSource()), Comparator.reverseOrder())
                .thenComparing((CandidateMatchRecord match) -> !match.fallbackUsed(), Comparator.reverseOrder())
                .thenComparing(this::categoryAlignmentScore, Comparator.reverseOrder())
                .thenComparing(this::rewriteCoverageScore, Comparator.reverseOrder())
                .thenComparing(this::attributeAlignmentScore, Comparator.reverseOrder())
                .thenComparing(this::titleOverlapScore, Comparator.reverseOrder())
                .thenComparing(this::accessoryPenaltyScore, Comparator.reverseOrder())
                .thenComparing(CandidateMatchRecord::similarityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateMatchRecord::price, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean isQualifiedBenchmark(
            CandidateMatchRecord match,
            Map<String, ProductDetailSnapshot> detailSnapshots,
            boolean strictRealtime
    ) {
        if (match == null) {
            return false;
        }
        if (accessoryPenaltyScore(match).compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (categoryAlignmentScore(match).compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (rewriteCoverageScore(match).compareTo(new BigDecimal("4.50")) < 0) {
            return false;
        }
        if (match.similarityScore() == null || match.similarityScore().compareTo(new BigDecimal("15.00")) < 0) {
            return false;
        }
        BigDecimal semanticAlignment = rewriteCoverageScore(match)
                .add(attributeAlignmentScore(match))
                .add(titleOverlapScore(match));
        if (semanticAlignment.compareTo(new BigDecimal("4.50")) < 0) {
            return false;
        }
        if (!strictRealtime) {
            return true;
        }
        // In REAL mode we require the match to come from a live 1688 search and have no
        // fallback — detail-snapshot is desirable but must not be a hard gate because
        // detail hydration can legitimately fail (page JS unavailable, timeout, etc.).
        // The comparator already ranks snapshot-backed matches first, so they are still
        // preferred when available.
        return isRealtimeSource(match.matchSource())
                && !match.fallbackUsed();
    }

    private boolean hasDetailSnapshot(CandidateMatchRecord match, Map<String, ProductDetailSnapshot> detailSnapshots) {
        if (match == null || detailSnapshots == null) {
            return false;
        }
        String externalItemId = match.externalItemId();
        return externalItemId != null && detailSnapshots.containsKey(externalItemId);
    }

    private boolean isRealtimeSource(String source) {
        return source != null && source.contains("REALTIME");
    }

    private BigDecimal titleOverlapScore(CandidateMatchRecord match) {
        return score(match, "titleOverlap");
    }

    private BigDecimal rewriteCoverageScore(CandidateMatchRecord match) {
        return score(match, "rewriteCoverage");
    }

    private BigDecimal attributeAlignmentScore(CandidateMatchRecord match) {
        return score(match, "attributeAlignment");
    }

    private BigDecimal categoryAlignmentScore(CandidateMatchRecord match) {
        return score(match, "categoryAlignment");
    }

    private BigDecimal accessoryPenaltyScore(CandidateMatchRecord match) {
        return score(match, "accessoryPenalty");
    }

    private BigDecimal score(CandidateMatchRecord match, String key) {
        if (match == null || match.scoreBreakdown() == null) {
            return BigDecimal.ZERO;
        }
        return match.scoreBreakdown().getOrDefault(key, BigDecimal.ZERO);
    }
}
