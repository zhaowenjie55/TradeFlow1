package com.globalvibe.arbitrage.domain.asr.dto;

import com.globalvibe.arbitrage.domain.search.dto.SearchResponse;

public record VoiceQueryResponse(
        AsrTranscriptionResult transcript,
        String translatedText,
        TranscriptIntentResult intent,
        String normalizedKeyword,
        SearchResponse searchResults
) {
}
