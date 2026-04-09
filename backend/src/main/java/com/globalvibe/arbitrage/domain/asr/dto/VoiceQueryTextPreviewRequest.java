package com.globalvibe.arbitrage.domain.asr.dto;

public record VoiceQueryTextPreviewRequest(
        String transcript,
        String translatedText
) {
}
