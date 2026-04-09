package com.globalvibe.arbitrage.integration.llm;

public interface LLMGateway {

    RewriteResult rewriteTitle(String sourceTitle);

    ReportNarrativeResult generateReportNarrative(ReportNarrativeRequest request);

    ReasoningResult generateReasoning(ReasoningRequest request);

    TranscriptIntentResult analyzeTranscript(TranscriptIntentRequest request);

    record RewriteResult(
            String rewrittenText,
            java.util.List<String> keywords,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            java.time.OffsetDateTime generatedAt
    ) {
    }

    record ReportNarrativeRequest(
            String productTitle,
            String market,
            String rewrittenQuery,
            java.util.List<String> rewrittenKeywords,
            String decision,
            String riskLevel,
            java.math.BigDecimal amazonPriceUsd,
            java.math.BigDecimal amazonPriceRmb,
            java.math.BigDecimal sourcingCost,
            java.math.BigDecimal domesticShippingCost,
            java.math.BigDecimal logisticsCost,
            java.math.BigDecimal platformFee,
            java.math.BigDecimal exchangeRateCost,
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
            String model,
            String fallbackReason,
            java.time.OffsetDateTime generatedAt
    ) {
    }

    record ReasoningRequest(
            String stepName,
            String prompt,
            java.util.Map<String, Object> context
    ) {
    }

    record ReasoningResult(
            String decision,
            String explanation,
            double confidenceScore,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            java.time.OffsetDateTime generatedAt
    ) {
    }

    record TranscriptIntentRequest(
            String transcript,
            String sourceType
    ) {
    }

    record TranscriptIntentResult(
            String intent,
            String category,
            String market,
            String priceLevel,
            String sourcing,
            java.util.List<String> keywords,
            java.util.List<String> sellingPoints,
            java.util.List<String> painPoints,
            java.util.List<String> useCases,
            java.util.List<String> targetAudience,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            java.time.OffsetDateTime generatedAt
    ) {
    }
}
