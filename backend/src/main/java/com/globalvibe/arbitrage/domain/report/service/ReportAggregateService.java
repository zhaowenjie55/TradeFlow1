package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.analysis.policy.FallbackPolicy;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import com.globalvibe.arbitrage.domain.report.model.ReportProvenance;
import com.globalvibe.arbitrage.domain.report.repository.ReportAggregateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ReportAggregateService {

    private final ReportAggregateRepository reportAggregateRepository;
    private final ReportMarkdownRenderer reportMarkdownRenderer;
    private final FallbackPolicy fallbackPolicy;

    public ReportAggregateService(
            ReportAggregateRepository reportAggregateRepository,
            ReportMarkdownRenderer reportMarkdownRenderer,
            FallbackPolicy fallbackPolicy
    ) {
        this.reportAggregateRepository = reportAggregateRepository;
        this.reportMarkdownRenderer = reportMarkdownRenderer;
        this.fallbackPolicy = fallbackPolicy;
    }

    public ReportAggregate save(String taskId, ArbitrageReport report) {
        return reportAggregateRepository.save(ReportAggregate.builder()
                .taskId(taskId)
                .reportId(report.reportId())
                .estimatedProfit(report.costBreakdown() != null ? report.costBreakdown().estimatedProfit() : BigDecimal.ZERO)
                .estimatedMargin(report.expectedMargin())
                .provenance(deriveProvenance(report))
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

    private ReportProvenance deriveProvenance(ArbitrageReport report) {
        String rewriteProvider = report.analysisTrace() != null && report.analysisTrace().rewrite() != null
                ? report.analysisTrace().rewrite().provider()
                : null;
        String rewriteModel = stringAuditValue(report, "rewriteModel");
        String retrievalSource = report.analysisTrace() != null && report.analysisTrace().retrieval() != null
                ? report.analysisTrace().retrieval().matchSource()
                : null;
        DomesticProductMatch primaryDomesticMatch = report.domesticMatches() == null || report.domesticMatches().isEmpty()
                ? null
                : report.domesticMatches().get(0);
        String detailSource = primaryDomesticMatch != null ? primaryDomesticMatch.detailSource() : null;
        String llmProvider = report.analysisTrace() != null && report.analysisTrace().llm() != null
                ? report.analysisTrace().llm().provider()
                : null;
        String llmModel = report.analysisTrace() != null && report.analysisTrace().llm() != null
                ? report.analysisTrace().llm().model()
                : null;

        boolean rewriteFallbackUsed = booleanAuditValue(report, "rewriteFallbackUsed");
        boolean retrievalFallbackUsed = booleanAuditValue(report, "retrievalFallbackUsed");
        boolean narrativeFallbackUsed = booleanAuditValue(report, "narrativeFallbackUsed");
        boolean fallbackUsed = fallbackPolicy.shouldMarkFallback(
                rewriteFallbackUsed,
                retrievalFallbackUsed,
                narrativeFallbackUsed,
                retrievalSource,
                detailSource
        );

        String fallbackReason = Stream.of(
                        stringAuditValue(report, "rewriteFallbackReason"),
                        stringAuditValue(report, "retrievalFallbackReason"),
                        stringAuditValue(report, "narrativeFallbackReason")
                )
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));

        return new ReportProvenance(
                rewriteProvider,
                rewriteModel,
                retrievalSource,
                detailSource,
                fallbackUsed,
                fallbackReason.isBlank() ? null : fallbackReason,
                llmProvider,
                llmModel,
                fallbackPolicy.deriveQualityTier(
                        retrievalSource,
                        detailSource,
                        rewriteFallbackUsed,
                        retrievalFallbackUsed,
                        narrativeFallbackUsed
                ),
                "v0"
        );
    }

    private boolean booleanAuditValue(ArbitrageReport report, String key) {
        Object value = report.auditData() == null ? null : report.auditData().get(key);
        return value instanceof Boolean boolValue
                ? boolValue
                : value != null && Boolean.parseBoolean(value.toString());
    }

    private String stringAuditValue(ArbitrageReport report, String key) {
        Object value = report.auditData() == null ? null : report.auditData().get(key);
        return value == null ? null : value.toString();
    }
}
