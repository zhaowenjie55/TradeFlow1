package com.globalvibe.arbitrage.integration.llm;

public interface LLMGateway {

    RewriteResult rewriteTitle(String sourceTitle);

    ReportNarrativeResult generateReportNarrative(ReportNarrativeRequest request);

    record RewriteResult(
            String rewrittenText,
            java.util.List<String> keywords,
            boolean fallbackUsed,
            String provider,
            String fallbackReason
    ) {
    }

    record ReportNarrativeRequest(
            String productTitle,
            String market,
            String rewrittenQuery,
            String decision,
            String riskLevel,
            java.math.BigDecimal targetSellingPrice,
            java.math.BigDecimal totalCost,
            java.math.BigDecimal estimatedProfit,
            java.math.BigDecimal estimatedMargin,
            String benchmarkTitle,
            java.util.List<String> domesticMatchTitles
    ) {
    }

    record ReportNarrativeResult(
            String summaryText,
            java.util.List<String> recommendations,
            java.util.List<String> riskNotes,
            boolean fallbackUsed,
            String provider,
            String fallbackReason
    ) {
    }
}
