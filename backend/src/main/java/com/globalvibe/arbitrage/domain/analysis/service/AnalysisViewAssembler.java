package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisResultVO;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.report.model.DomesticProductMatch;
import com.globalvibe.arbitrage.domain.report.model.ReportAggregate;
import org.springframework.stereotype.Component;

@Component
public class AnalysisViewAssembler {

    public AnalysisResultVO toView(ReportAggregate reportAggregate) {
        ArbitrageReport report = reportAggregate.report();
        DomesticProductMatch benchmark = report.domesticMatches().isEmpty() ? null : report.domesticMatches().get(0);
        return new AnalysisResultVO(
                reportAggregate.taskId(),
                reportAggregate.reportId(),
                report.productId(),
                report.title(),
                benchmark != null ? benchmark.platformProductId() : null,
                benchmark != null ? benchmark.title() : null,
                benchmark != null ? benchmark.platform() : null,
                benchmark != null ? benchmark.price() : null,
                report.expectedMargin(),
                benchmark != null ? benchmark.similarityScore() : null,
                report.decision(),
                report.riskLevel(),
                report.summary() != null ? report.summary().insightKey() : null,
                reportAggregate.createdAt()
        );
    }
}
