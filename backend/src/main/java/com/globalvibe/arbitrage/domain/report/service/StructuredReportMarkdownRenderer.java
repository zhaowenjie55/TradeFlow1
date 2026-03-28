package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import org.springframework.stereotype.Component;

@Component
public class StructuredReportMarkdownRenderer implements ReportMarkdownRenderer {

    @Override
    public String render(ArbitrageReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(report.title()).append("\n\n");
        builder.append("- 市场: ").append(report.market()).append("\n");
        builder.append("- 决策: ").append(report.decision()).append("\n");
        builder.append("- 风险等级: ").append(report.riskLevel()).append("\n");
        builder.append("- 预计利润率: ").append(report.expectedMargin()).append("%\n");
        if (report.costBreakdown() != null) {
            builder.append("- 目标售价: ").append(report.costBreakdown().targetSellingPrice()).append("\n");
            builder.append("- 总成本: ").append(report.costBreakdown().totalCost()).append("\n");
            builder.append("- 预计利润: ").append(report.costBreakdown().estimatedProfit()).append("\n");
        }
        if (report.summary() != null && report.summary().insightParams() != null && report.summary().insightParams().containsKey("message")) {
            builder.append("\n## Agent 总结\n\n");
            builder.append(report.summary().insightParams().get("message")).append("\n");
        }
        if (report.riskAssessment() != null && report.riskAssessment().notes() != null && !report.riskAssessment().notes().isEmpty()) {
            builder.append("\n## 风险备注\n");
            report.riskAssessment().notes().forEach(item -> builder.append("- ").append(item).append("\n"));
        }
        if (report.domesticMatches() != null && !report.domesticMatches().isEmpty()) {
            builder.append("\n## 参考货源\n");
            report.domesticMatches().forEach(match -> {
                builder.append("- ")
                        .append(match.platform())
                        .append(" / ")
                        .append(match.title())
                        .append(" / ")
                        .append(match.price())
                        .append("\n");
            });
        }
        if (report.recommendations() != null && !report.recommendations().isEmpty()) {
            builder.append("\n## 建议\n");
            report.recommendations().forEach(item -> builder.append("- ").append(item).append("\n"));
        }
        return builder.toString();
    }
}
