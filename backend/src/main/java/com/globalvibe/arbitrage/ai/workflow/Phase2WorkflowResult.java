package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;

import java.util.List;

public record Phase2WorkflowResult(
        ArbitrageReport report,
        List<TaskLogEntry> logs,
        QueryRewrite queryRewrite,
        List<CandidateMatchRecord> candidateMatches,
        boolean fallbackUsed
) {
}
