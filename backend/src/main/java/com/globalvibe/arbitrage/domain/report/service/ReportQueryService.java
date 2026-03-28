package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.dto.ReportListResponse;
import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import org.springframework.stereotype.Service;

@Service
public class ReportQueryService {

    private final ReportAggregateService reportAggregateService;
    private final ReportViewAssembler reportViewAssembler;

    public ReportQueryService(
            ReportAggregateService reportAggregateService,
            ReportViewAssembler reportViewAssembler
    ) {
        this.reportAggregateService = reportAggregateService;
        this.reportViewAssembler = reportViewAssembler;
    }

    public ReportListResponse listReports() {
        return new ReportListResponse(
                reportAggregateService.findAll().stream()
                        .filter(aggregate -> aggregate.report() != null)
                        .map(reportViewAssembler::toListItem)
                        .toList()
        );
    }

    public ReportDetailVO getReportByTaskId(String taskId) {
        return reportAggregateService.findByTaskId(taskId)
                .filter(aggregate -> aggregate.report() != null)
                .map(reportViewAssembler::toDetail)
                .orElseThrow(() -> new ReportNotFoundException(taskId));
    }

    public ReportDetailVO getReportByReportId(String reportId) {
        return reportAggregateService.findByReportId(reportId)
                .filter(aggregate -> aggregate.report() != null)
                .map(reportViewAssembler::toDetail)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
    }
}
