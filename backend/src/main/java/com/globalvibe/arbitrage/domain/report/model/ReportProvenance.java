package com.globalvibe.arbitrage.domain.report.model;

public record ReportProvenance(
        String rewriteProvider,
        String rewriteModel,
        String retrievalSource,
        String detailSource,
        boolean fallbackUsed,
        String fallbackReason,
        String llmProvider,
        String llmModel,
        String qualityTier,
        String pricingConfigVersion
) {
}
