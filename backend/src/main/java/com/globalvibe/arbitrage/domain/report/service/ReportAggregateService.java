package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import com.globalvibe.arbitrage.domain.report.repository.ReportAggregateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportAggregateService {

    private final ReportAggregateRepository reportAggregateRepository;
    private final ReportMarkdownRenderer reportMarkdownRenderer;

    public ReportAggregateService(
            ReportAggregateRepository reportAggregateRepository,
            ReportMarkdownRenderer reportMarkdownRenderer
    ) {
        this.reportAggregateRepository = reportAggregateRepository;
        this.reportMarkdownRenderer = reportMarkdownRenderer;
    }

    public ReportAggregate save(String taskId, ArbitrageReport report) {
        return reportAggregateRepository.save(ReportAggregate.builder()
                .taskId(taskId)
                .reportId(report.reportId())
                .estimatedProfit(report.costBreakdown() != null ? report.costBreakdown().estimatedProfit() : BigDecimal.ZERO)
                .estimatedMargin(report.expectedMargin())
                .reportMarkdown(reportMarkdownRenderer.render(report))
                .report(report)
                .createdAt(report.generatedAt() != null ? report.generatedAt() : OffsetDateTime.now())
                .build());
    }

    public Optional<ReportAggregate> findByTaskId(String taskId) {
        return reportAggregateRepository.findByTaskId(taskId);
    }

    public Optional<ReportAggregate> findByReportId(String reportId) {
        return reportAggregateRepository.findByReportId(reportId);
    }

    public List<ReportAggregate> findAll() {
        return reportAggregateRepository.findAll();
    }
}
