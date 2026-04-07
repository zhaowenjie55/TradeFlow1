package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
            builder.append("- Amazon 售价（人民币）: ").append(currency(report.costBreakdown().targetSellingPrice())).append("\n");
            builder.append("- 采购成本: ").append(currency(report.costBreakdown().sourcingCost())).append("\n");
            builder.append("- 国内运费: ").append(currency(report.costBreakdown().domesticShippingCost())).append("\n");
            builder.append("- 跨境物流成本: ").append(currency(report.costBreakdown().logisticsCost())).append("\n");
            builder.append("- 平台费: ").append(currency(report.costBreakdown().platformFee())).append("\n");
            builder.append("- 汇损成本: ").append(currency(report.costBreakdown().exchangeRateCost())).append("\n");
            builder.append("- 总成本: ").append(currency(report.costBreakdown().totalCost())).append("\n");
            builder.append("- 预计利润: ").append(currency(report.costBreakdown().estimatedProfit())).append("\n");
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
                builder.append("  - 详情状态: ")
                        .append(match.detailReady() ? "READY" : "SEARCH_ONLY")
                        .append(" / ")
                        .append(match.detailSource() == null ? "UNKNOWN" : match.detailSource())
                        .append("\n");
                if (match.reason() != null && !match.reason().isBlank()) {
                    builder.append("  - 说明: ").append(match.reason()).append("\n");
                }
                if (match.scoreBreakdown() != null && !match.scoreBreakdown().isEmpty()) {
                    builder.append("  - 分数拆解: ").append(match.scoreBreakdown()).append("\n");
                }
            });
        }
        if (report.analysisTrace() != null) {
            builder.append("\n## Analysis Trace\n");
            if (report.analysisTrace().rewrite() != null) {
                builder.append("- 改写词: ").append(report.analysisTrace().rewrite().rewrittenText()).append("\n");
                builder.append("- 改写关键词: ").append(report.analysisTrace().rewrite().keywords()).append("\n");
                builder.append("- 改写提供方: ").append(report.analysisTrace().rewrite().provider()).append("\n");
            }
            if (report.analysisTrace().retrieval() != null) {
                builder.append("- 检索词: ").append(report.analysisTrace().retrieval().retrievalTerms()).append("\n");
                builder.append("- 匹配来源: ").append(report.analysisTrace().retrieval().matchSource()).append("\n");
                builder.append("- 检索证据: ").append(report.analysisTrace().retrieval().evidence()).append("\n");
            }
            if (report.analysisTrace().pricing() != null) {
                builder.append("- 定价假设: ").append(report.analysisTrace().pricing().assumptions()).append("\n");
                report.analysisTrace().pricing().formulaLines()
                        .forEach(line -> builder.append("  - ").append(line).append("\n"));
            }
            if (report.analysisTrace().llm() != null) {
                builder.append("- LLM: ")
                        .append(report.analysisTrace().llm().provider())
                        .append(" / ")
                        .append(report.analysisTrace().llm().model())
                        .append(" / ")
                        .append(report.analysisTrace().llm().generatedAt())
                        .append("\n");
            }
        }
        if (report.recommendations() != null && !report.recommendations().isEmpty()) {
            builder.append("\n## 建议\n");
            report.recommendations().forEach(item -> builder.append("- ").append(item).append("\n"));
        }
        return builder.toString();
    }

    private String currency(BigDecimal value) {
        if (value == null) {
            return "¥0.00";
        }
        return "¥" + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
