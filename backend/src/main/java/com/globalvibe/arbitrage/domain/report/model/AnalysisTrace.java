package com.globalvibe.arbitrage.domain.report.model;

public record AnalysisTrace(
        AnalysisTraceRewrite rewrite,
        AnalysisTraceRetrieval retrieval,
        AnalysisTracePricing pricing,
        AnalysisTraceLlm llm
) {
}
