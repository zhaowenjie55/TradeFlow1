package com.globalvibe.arbitrage.domain.analysis.dto;

import com.globalvibe.arbitrage.domain.analysis.model.AnalysisReport;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisTrace;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisTaskStatus;

public record AnalysisRunResponse(
        String taskId,
        AnalysisTaskStatus status,
        AnalysisReport report,
        AnalysisTrace trace
) {
}
