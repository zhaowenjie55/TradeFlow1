package com.globalvibe.arbitrage.domain.asr.dto;

public record MediaAnalysisResponse(
        AsrTranscriptionResult transcript,
        TranscriptIntentResult intent
) {
}

