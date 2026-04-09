package com.globalvibe.arbitrage.domain.asr.dto;

public record VoiceQueryPreviewResponse(
        AsrTranscriptionResult transcript,
        String translatedText,
        TranscriptIntentResult intent,
        String normalizedKeyword
) {
}

