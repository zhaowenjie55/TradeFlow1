package com.globalvibe.arbitrage.domain.analysis.policy;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class FallbackPolicy {

    private final Map<FallbackStage, List<String>> precedenceByStage = new EnumMap<>(FallbackStage.class);

    public FallbackPolicy() {
        precedenceByStage.put(FallbackStage.REWRITE, List.of("LIVE_LLM", "REWRITE_HISTORY"));
        precedenceByStage.put(FallbackStage.RETRIEVAL, List.of("REALTIME", "LOCAL_CATALOG"));
        precedenceByStage.put(FallbackStage.DETAIL_HYDRATION, List.of("REALTIME_DETAIL", "DETAIL_SNAPSHOT", "SEARCH_RESULT_ONLY"));
        precedenceByStage.put(FallbackStage.NARRATIVE, List.of("LIVE_LLM", "SIMULATED"));
    }

    public List<String> precedence(FallbackStage stage) {
        return precedenceByStage.getOrDefault(stage, List.of());
    }

    public boolean isFallbackRetrievalSource(String retrievalSource) {
        return containsAny(retrievalSource, "FALLBACK", "CATALOG");
    }

    public boolean isFallbackDetailSource(String detailSource) {
        return containsAny(detailSource, "SNAPSHOT", "SEARCH_RESULT_ONLY");
    }

    public boolean shouldMarkFallback(
            boolean rewriteFallbackUsed,
            boolean retrievalFallbackUsed,
            boolean narrativeFallbackUsed,
            String retrievalSource,
            String detailSource
    ) {
        return rewriteFallbackUsed
                || retrievalFallbackUsed
                || narrativeFallbackUsed
                || isFallbackRetrievalSource(retrievalSource)
                || isFallbackDetailSource(detailSource);
    }

    public String deriveQualityTier(
            String retrievalSource,
            String detailSource,
            boolean rewriteFallbackUsed,
            boolean retrievalFallbackUsed,
            boolean narrativeFallbackUsed
    ) {
        if (narrativeFallbackUsed) {
            return "LLM_FALLBACK_ASSISTED";
        }
        boolean realtimeRetrieval = containsAny(retrievalSource, "REALTIME");
        boolean realtimeDetail = "DOMESTIC_REALTIME_DETAIL".equals(detailSource);
        if (realtimeRetrieval && realtimeDetail && !rewriteFallbackUsed && !retrievalFallbackUsed) {
            return "REALTIME_CONFIRMED";
        }
        if (realtimeRetrieval) {
            return "REALTIME_HYBRID";
        }
        return "SNAPSHOT_FALLBACK";
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
