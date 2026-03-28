package com.globalvibe.arbitrage.domain.report.dto;

public record AnalysisTraceVO(
        AnalysisTraceRewriteVO rewrite,
        AnalysisTraceRetrievalVO retrieval,
        AnalysisTracePricingVO pricing,
        AnalysisTraceLlmVO llm
) {
}
