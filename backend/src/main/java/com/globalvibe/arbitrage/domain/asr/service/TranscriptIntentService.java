package com.globalvibe.arbitrage.domain.asr.service;

import com.globalvibe.arbitrage.domain.asr.dto.TranscriptIntentResult;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

@Service
public class TranscriptIntentService {

    private final LLMGateway llmGateway;

    public TranscriptIntentService(LLMGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public TranscriptIntentResult analyze(String transcript, String sourceType) {
        LLMGateway.TranscriptIntentResult result = llmGateway.analyzeTranscript(
                new LLMGateway.TranscriptIntentRequest(transcript, sourceType)
        );
        return new TranscriptIntentResult(
                result.intent(),
                result.category(),
                result.market(),
                result.priceLevel(),
                result.sourcing(),
                result.keywords(),
                result.sellingPoints(),
                result.painPoints(),
                result.useCases(),
                result.targetAudience(),
                result.fallbackUsed(),
                result.provider(),
                result.model(),
                result.fallbackReason(),
                result.generatedAt()
        );
    }
}

