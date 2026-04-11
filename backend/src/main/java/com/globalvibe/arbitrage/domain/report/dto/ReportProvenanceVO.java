package com.globalvibe.arbitrage.domain.report.dto;

public record ReportProvenanceVO(
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
