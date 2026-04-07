package com.globalvibe.arbitrage.domain.analysis.model;

import java.util.List;

public record AnalysisTrace(
        List<ReasoningStep> steps
) {
}
