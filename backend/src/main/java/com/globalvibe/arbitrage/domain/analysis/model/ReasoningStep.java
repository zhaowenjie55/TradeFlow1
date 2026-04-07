package com.globalvibe.arbitrage.domain.analysis.model;

public record ReasoningStep(
        String stepName,
        String inputSummary,
        String decision,
        String explanation
) {
}
