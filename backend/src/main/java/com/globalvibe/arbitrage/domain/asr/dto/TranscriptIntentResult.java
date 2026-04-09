package com.globalvibe.arbitrage.domain.asr.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record TranscriptIntentResult(
        String intent,
        String category,
        String market,
        String priceLevel,
        String sourcing,
        List<String> keywords,
        List<String> sellingPoints,
        List<String> painPoints,
        List<String> useCases,
        List<String> targetAudience,
        boolean fallbackUsed,
        String provider,
        String model,
        String fallbackReason,
        OffsetDateTime generatedAt
) {
}

