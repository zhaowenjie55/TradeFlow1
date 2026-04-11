package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.dto.AnalysisTraceLlmVO;
import com.globalvibe.arbitrage.domain.report.dto.AnalysisTracePricingVO;
import com.globalvibe.arbitrage.domain.report.dto.AnalysisTraceRetrievalVO;
import com.globalvibe.arbitrage.domain.report.dto.AnalysisTraceRewriteVO;
import com.globalvibe.arbitrage.domain.report.dto.AnalysisTraceVO;
import com.globalvibe.arbitrage.domain.report.dto.DomesticProductMatchVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportCostBreakdownVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportDownloadDocumentVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportListItemResponse;
import com.globalvibe.arbitrage.domain.report.dto.ReportProvenanceVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportRiskAssessmentVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportSummaryVO;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.AnalysisTrace;
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

    private final ReportPdfRenderer reportPdfRenderer;

    public ReportViewAssembler(ReportPdfRenderer reportPdfRenderer) {
        this.reportPdfRenderer = reportPdfRenderer;
    }

    public ReportListItemResponse toListItem(String taskId, ArbitrageReport report) {
        return new ReportListItemResponse(
                taskId,
                report.reportId(),
                report.productId(),
                report.title(),
                report.decision(),
                report.expectedMargin(),
                report.riskLevel(),
                null,
                false,
                null,
                null,
                report.generatedAt()
        );
    }

    public ReportListItemResponse toListItem(ReportAggregate aggregate) {
        ArbitrageReport report = aggregate.report();
        return new ReportListItemResponse(
                aggregate.taskId(),
                report.reportId(),
                report.productId(),
                report.title(),
                report.decision(),
                report.expectedMargin(),
                report.riskLevel(),
                aggregate.provenance() != null ? aggregate.provenance().qualityTier() : null,
                aggregate.provenance() != null && aggregate.provenance().fallbackUsed(),
                aggregate.provenance() != null ? aggregate.provenance().retrievalSource() : null,
                aggregate.provenance() != null ? aggregate.provenance().detailSource() : null,
                report.generatedAt()
        );
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
                toAnalysisTrace(report.analysisTrace()),
                null,
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
                toAnalysisTrace(report.analysisTrace()),
                toProvenance(aggregate.provenance()),
                buildDownloadDocument(report, aggregate.reportMarkdown())
        );
    }

    private ReportDownloadDocumentVO buildDownloadDocument(ArbitrageReport report, String reportMarkdown) {
        return reportPdfRenderer.render(report, reportMarkdown);
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
                match.reason(),
                match.matchSource(),
                match.detailReady(),
                match.detailSource(),
                match.retrievalTerms(),
                match.scoreBreakdown(),
                match.evidence()
        );
    }

    private AnalysisTraceVO toAnalysisTrace(AnalysisTrace analysisTrace) {
        if (analysisTrace == null) {
            return null;
        }
        return new AnalysisTraceVO(
                analysisTrace.rewrite() == null ? null : new AnalysisTraceRewriteVO(
                        analysisTrace.rewrite().sourceTitle(),
                        analysisTrace.rewrite().rewrittenText(),
                        analysisTrace.rewrite().keywords(),
                        analysisTrace.rewrite().provider()
                ),
                analysisTrace.retrieval() == null ? null : new AnalysisTraceRetrievalVO(
                        analysisTrace.retrieval().retrievalTerms(),
                        analysisTrace.retrieval().matchSource(),
                        analysisTrace.retrieval().scoreBreakdown(),
                        analysisTrace.retrieval().evidence()
                ),
                analysisTrace.pricing() == null ? null : new AnalysisTracePricingVO(
                        analysisTrace.pricing().currency(),
                        analysisTrace.pricing().usdToCnyRate(),
                        analysisTrace.pricing().formulaLines(),
                        analysisTrace.pricing().assumptions()
                ),
                analysisTrace.llm() == null ? null : new AnalysisTraceLlmVO(
                        analysisTrace.llm().provider(),
                        analysisTrace.llm().model(),
                        analysisTrace.llm().generatedAt()
                )
        );
    }

    private ReportProvenanceVO toProvenance(com.globalvibe.arbitrage.domain.report.model.ReportProvenance provenance) {
        if (provenance == null) {
            return null;
        }
        return new ReportProvenanceVO(
                provenance.rewriteProvider(),
                provenance.rewriteModel(),
                provenance.retrievalSource(),
                provenance.detailSource(),
                provenance.fallbackUsed(),
                provenance.fallbackReason(),
                provenance.llmProvider(),
                provenance.llmModel(),
                provenance.qualityTier(),
                provenance.pricingConfigVersion()
        );
    }
}
