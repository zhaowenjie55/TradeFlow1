package com.globalvibe.arbitrage.domain.report.dto;

import java.util.List;

public record AnalysisTraceRewriteVO(
        String sourceTitle,
        String rewrittenText,
        List<String> keywords,
        String provider
) {
}
