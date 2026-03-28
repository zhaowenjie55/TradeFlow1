package com.globalvibe.arbitrage.domain.report.model;

import java.util.List;

public record AnalysisTraceRewrite(
        String sourceTitle,
        String rewrittenText,
        List<String> keywords,
        String provider
) {
}
