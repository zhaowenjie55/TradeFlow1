package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisResultVO;
import com.globalvibe.arbitrage.domain.report.service.ReportAggregateService;
import org.springframework.stereotype.Service;

@Service
public class AnalysisQueryService {

    private final AnalysisViewAssembler analysisViewAssembler;
    private final ReportAggregateService reportAggregateService;

    public AnalysisQueryService(
            AnalysisViewAssembler analysisViewAssembler,
            ReportAggregateService reportAggregateService
    ) {
        this.analysisViewAssembler = analysisViewAssembler;
        this.reportAggregateService = reportAggregateService;
    }

    public AnalysisResultVO getByTaskId(String taskId) {
        return reportAggregateService.findByTaskId(taskId)
                .filter(aggregate -> aggregate.report() != null)
                .map(analysisViewAssembler::toView)
                .orElseThrow(() -> new AnalysisResultNotFoundException(taskId));
    }
}
