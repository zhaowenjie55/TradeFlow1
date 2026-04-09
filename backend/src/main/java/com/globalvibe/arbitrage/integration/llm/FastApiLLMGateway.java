package com.globalvibe.arbitrage.integration.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class FastApiLLMGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FastApiLLMGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            ObjectMapper objectMapper
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(integrationGatewayProperties.getLlm().getConnectTimeoutMillis()))
                .build();
    }

    public LLMGateway.RewriteResult rewriteTitle(String sourceTitle) {
        RewriteResponsePayload response = post("/rewrite", new RewriteRequestPayload(sourceTitle, Map.of()), RewriteResponsePayload.class);
        return new LLMGateway.RewriteResult(
                response.rewrittenText(),
                response.keywords() == null ? List.of() : response.keywords(),
                response.fallbackUsed(),
                response.provider(),
                response.model(),
                response.fallbackReason(),
                parseTimestamp(response.generatedAt())
        );
    }

    public LLMGateway.ReportNarrativeResult generateReportNarrative(LLMGateway.ReportNarrativeRequest request) {
        ReportNarrativeResponsePayload response = post("/report-narrative", new ReportNarrativeRequestPayload(
                request.productTitle(),
                request.market(),
                request.rewrittenQuery(),
                request.rewrittenKeywords(),
                request.decision(),
                request.riskLevel(),
                request.amazonPriceUsd(),
                request.amazonPriceRmb(),
                request.sourcingCost(),
                request.domesticShippingCost(),
                request.logisticsCost(),
                request.platformFee(),
                request.exchangeRateCost(),
                request.totalCost(),
                request.estimatedProfit(),
                request.estimatedMargin(),
                request.benchmarkTitle(),
                request.domesticMatchTitles()
        ), ReportNarrativeResponsePayload.class);
        return new LLMGateway.ReportNarrativeResult(
                response.summaryText(),
                response.recommendations() == null ? List.of() : response.recommendations(),
                response.riskNotes() == null ? List.of() : response.riskNotes(),
                response.fallbackUsed(),
                response.provider(),
                response.model(),
                response.fallbackReason(),
                parseTimestamp(response.generatedAt())
        );
    }

    public LLMGateway.ReasoningResult generateReasoning(LLMGateway.ReasoningRequest request) {
        ReasoningResponsePayload response = post("/reasoning", new ReasoningRequestPayload(
                request.stepName(),
                request.prompt(),
                request.context()
        ), ReasoningResponsePayload.class);
        return new LLMGateway.ReasoningResult(
                response.decision(),
                response.explanation(),
                response.confidenceScore(),
                response.fallbackUsed(),
                response.provider(),
                response.model(),
                response.fallbackReason(),
                parseTimestamp(response.generatedAt())
        );
    }

    public LLMGateway.TranscriptIntentResult analyzeTranscript(LLMGateway.TranscriptIntentRequest request) {
        TranscriptIntentResponsePayload response = post("/transcript-intent", new TranscriptIntentRequestPayload(
                request.transcript(),
                request.sourceType()
        ), TranscriptIntentResponsePayload.class);
        return new LLMGateway.TranscriptIntentResult(
                response.intent(),
                response.category(),
                response.market(),
                response.priceLevel(),
                response.sourcing(),
                response.keywords() == null ? List.of() : response.keywords(),
                response.sellingPoints() == null ? List.of() : response.sellingPoints(),
                response.painPoints() == null ? List.of() : response.painPoints(),
                response.useCases() == null ? List.of() : response.useCases(),
                response.targetAudience() == null ? List.of() : response.targetAudience(),
                response.fallbackUsed(),
                response.provider(),
                response.model(),
                response.fallbackReason(),
                parseTimestamp(response.generatedAt())
        );
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        String endpointBase = integrationGatewayProperties.getLlm().getEndpointBase();
        if (endpointBase == null || endpointBase.isBlank()) {
            throw new IllegalStateException("FastAPI LLM endpoint base is not configured.");
        }
        byte[] responseBody;
        int statusCode;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointBase + path))
                    .timeout(Duration.ofMillis(integrationGatewayProperties.getLlm().getReadTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            statusCode = response.statusCode();
            responseBody = response.body();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call FastAPI LLM gateway.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FastAPI LLM gateway call was interrupted.", ex);
        }
        if (statusCode < 200 || statusCode >= 300) {
            String rawBody = responseBody == null ? "" : new String(responseBody, StandardCharsets.UTF_8);
            throw new IllegalStateException("FastAPI LLM gateway returned status " + statusCode + ": " + rawBody);
        }
        if (responseBody == null || responseBody.length == 0) {
            throw new IllegalStateException("FastAPI LLM gateway returned empty response.");
        }
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException ex) {
            String rawBody = new String(responseBody, StandardCharsets.UTF_8);
            throw new IllegalStateException("FastAPI LLM gateway returned invalid JSON: " + rawBody, ex);
        }
    }

    private OffsetDateTime parseTimestamp(String generatedAt) {
        return generatedAt == null || generatedAt.isBlank() ? OffsetDateTime.now() : OffsetDateTime.parse(generatedAt);
    }

    private record RewriteRequestPayload(
            String sourceTitle,
            Map<String, Object> context
    ) {
    }

    private record RewriteResponsePayload(
            String rewrittenText,
            List<String> keywords,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            String generatedAt
    ) {
    }

    private record ReportNarrativeRequestPayload(
            String productTitle,
            String market,
            String rewrittenQuery,
            List<String> rewrittenKeywords,
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
            List<String> domesticMatchTitles
    ) {
    }

    private record ReportNarrativeResponsePayload(
            String summaryText,
            List<String> recommendations,
            List<String> riskNotes,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            String generatedAt
    ) {
    }

    private record ReasoningRequestPayload(
            String stepName,
            String prompt,
            Map<String, Object> context
    ) {
    }

    private record ReasoningResponsePayload(
            String decision,
            String explanation,
            double confidenceScore,
            boolean fallbackUsed,
            String provider,
            String model,
            String fallbackReason,
            String generatedAt
    ) {
    }

    private record TranscriptIntentRequestPayload(
            String transcript,
            String sourceType
    ) {
    }

    private record TranscriptIntentResponsePayload(
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
            String generatedAt
    ) {
    }
}
