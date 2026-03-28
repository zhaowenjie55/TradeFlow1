package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.dto.DomesticProductMatchVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportCostBreakdownVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportDownloadDocumentVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportListItemResponse;
import com.globalvibe.arbitrage.domain.report.dto.ReportRiskAssessmentVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportSummaryVO;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import com.globalvibe.arbitrage.domain.report.model.ReportCostBreakdown;
import com.globalvibe.arbitrage.domain.report.model.ReportRiskAssessment;
import com.globalvibe.arbitrage.domain.report.model.ReportSummary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReportViewAssembler {

    public ReportListItemResponse toListItem(String taskId, ArbitrageReport report) {
        return new ReportListItemResponse(
                taskId,
                report.reportId(),
                report.productId(),
                report.title(),
                report.decision(),
                report.expectedMargin(),
                report.riskLevel(),
                report.generatedAt()
        );
    }

    public ReportListItemResponse toListItem(ReportAggregate aggregate) {
        return toListItem(aggregate.taskId(), aggregate.report());
    }

    public ReportDetailVO toDetail(String taskId, ArbitrageReport report) {
        return new ReportDetailVO(
                taskId,
                report.reportId(),
                report.productId(),
                report.title(),
                report.market(),
                report.image(),
                report.decision(),
                report.riskLevel(),
                report.expectedMargin(),
                report.generatedAt(),
                toSummary(report.summary()),
                toCostBreakdown(report.costBreakdown()),
                toRiskAssessment(report.riskAssessment()),
                report.recommendations() == null ? List.of() : report.recommendations(),
                report.domesticMatches() == null ? List.of() : report.domesticMatches().stream().map(this::toDomesticMatch).toList(),
                null
        );
    }

    public ReportDetailVO toDetail(ReportAggregate aggregate) {
        ArbitrageReport report = aggregate.report();
        return new ReportDetailVO(
                aggregate.taskId(),
                report.reportId(),
                report.productId(),
                report.title(),
                report.market(),
                report.image(),
                report.decision(),
                report.riskLevel(),
                report.expectedMargin(),
                report.generatedAt(),
                toSummary(report.summary()),
                toCostBreakdown(report.costBreakdown()),
                toRiskAssessment(report.riskAssessment()),
                report.recommendations() == null ? List.of() : report.recommendations(),
                report.domesticMatches() == null ? List.of() : report.domesticMatches().stream().map(this::toDomesticMatch).toList(),
                buildDownloadDocument(report, aggregate.reportMarkdown())
        );
    }

    private ReportDownloadDocumentVO buildDownloadDocument(ArbitrageReport report, String reportMarkdown) {
        String fileName = sanitizeFileName(report.title()) + "-agent-report.md";
        return new ReportDownloadDocumentVO(
                fileName,
                "text/markdown;charset=utf-8",
                reportMarkdown == null ? "" : reportMarkdown
        );
    }

    private ReportSummaryVO toSummary(ReportSummary summary) {
        if (summary == null) {
            return new ReportSummaryVO("report.summary.pending", Map.of());
        }
        return new ReportSummaryVO(summary.insightKey(), summary.insightParams());
    }

    private ReportCostBreakdownVO toCostBreakdown(ReportCostBreakdown costBreakdown) {
        if (costBreakdown == null) {
            return new ReportCostBreakdownVO(null, null, null, null, null, null, null, null);
        }
        return new ReportCostBreakdownVO(
                costBreakdown.sourcingCost(),
                costBreakdown.domesticShippingCost(),
                costBreakdown.logisticsCost(),
                costBreakdown.platformFee(),
                costBreakdown.exchangeRateCost(),
                costBreakdown.totalCost(),
                costBreakdown.targetSellingPrice(),
                costBreakdown.estimatedProfit()
        );
    }

    private ReportRiskAssessmentVO toRiskAssessment(ReportRiskAssessment riskAssessment) {
        if (riskAssessment == null) {
            return new ReportRiskAssessmentVO(null, List.of(), null);
        }
        return new ReportRiskAssessmentVO(
                riskAssessment.score(),
                riskAssessment.factors(),
                riskAssessment.notes()
        );
    }

    private DomesticProductMatchVO toDomesticMatch(DomesticProductMatch match) {
        return new DomesticProductMatchVO(
                match.id(),
                match.platform(),
                match.platformProductId(),
                match.title(),
                match.price(),
                match.image(),
                match.similarityScore(),
                match.detailUrl(),
                match.searchUrl(),
                match.reason()
        );
    }

    private String sanitizeFileName(String value) {
        return value == null
                ? "globalvibe-report"
                : value.trim()
                .replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]+", "-")
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }
}
