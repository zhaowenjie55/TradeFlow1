package com.globalvibe.arbitrage.domain.analysis.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FallbackPolicyTest {

    private final FallbackPolicy fallbackPolicy = new FallbackPolicy();

    @Test
    void shouldDeriveRealtimeConfirmedWhenNoFallbackSignalsExist() {
        assertEquals(
                "REALTIME_CONFIRMED",
                fallbackPolicy.deriveQualityTier(
                        "DOMESTIC_REALTIME_HYBRID",
                        "DOMESTIC_REALTIME_DETAIL",
                        false,
                        false,
                        false
                )
        );
        assertFalse(fallbackPolicy.shouldMarkFallback(false, false, false, "DOMESTIC_REALTIME_HYBRID", "DOMESTIC_REALTIME_DETAIL"));
    }

    @Test
    void shouldDeriveSnapshotFallbackWhenSnapshotSignalsExist() {
        assertEquals(
                "SNAPSHOT_FALLBACK",
                fallbackPolicy.deriveQualityTier(
                        "CATALOG_TEXT",
                        "DETAIL_SNAPSHOT",
                        false,
                        true,
                        false
                )
        );
        assertTrue(fallbackPolicy.shouldMarkFallback(false, true, false, "CATALOG_TEXT", "DETAIL_SNAPSHOT"));
    }

    @Test
    void shouldDeriveLlmFallbackAssistedWhenNarrativeFallbackIsUsed() {
        assertEquals(
                "LLM_FALLBACK_ASSISTED",
                fallbackPolicy.deriveQualityTier(
                        "DOMESTIC_REALTIME_HYBRID",
                        "DOMESTIC_REALTIME_DETAIL",
                        false,
                        false,
                        true
                )
        );
    }
}
