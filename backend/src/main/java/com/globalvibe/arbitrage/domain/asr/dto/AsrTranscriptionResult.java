package com.globalvibe.arbitrage.domain.asr.dto;

import java.util.List;

public record AsrTranscriptionResult(
        boolean success,
        String language,
        double duration,
        String text,
        List<AsrSegment> segments
) {
}

