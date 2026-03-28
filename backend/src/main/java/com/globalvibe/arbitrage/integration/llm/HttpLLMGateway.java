package com.globalvibe.arbitrage.integration.llm;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class HttpLLMGateway {

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;

    public HttpLLMGateway(IntegrationGatewayProperties integrationGatewayProperties) {
        this.restClient = RestClient.builder().build();
        this.integrationGatewayProperties = integrationGatewayProperties;
    }

    @SuppressWarnings("unchecked")
    public LLMGateway.RewriteResult rewriteTitle(String sourceTitle) {
        String endpoint = integrationGatewayProperties.getLlm().getRewriteEndpoint();
        if (!integrationGatewayProperties.getLlm().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("LLM rewrite endpoint is not configured.");
        }

        Map<String, Object> response = restClient.post()
                .uri(endpoint)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getLlm().getApiKey()))
                .body(Map.of("title", sourceTitle))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("LLM gateway returned empty response.");
        }

        Object rewrittenValue = response.containsKey("rewritten_text")
                ? response.get("rewritten_text")
                : response.get("rewrittenText");
        String rewrittenText = rewrittenValue == null ? "" : rewrittenValue.toString().trim();
        if (rewrittenText.isBlank()) {
            throw new IllegalStateException("LLM gateway returned blank rewritten text.");
        }

        Object keywordsValue = response.get("keywords");
        List<String> keywords = new ArrayList<>();
        if (keywordsValue instanceof List<?> list) {
            list.stream().filter(item -> item != null && !item.toString().isBlank())
                    .map(Object::toString)
                    .forEach(keywords::add);
        }

        if (keywords.isEmpty()) {
            keywords.add(rewrittenText);
        }

        return new LLMGateway.RewriteResult(rewrittenText, keywords, false, "HTTP_LLM", null);
    }

    @SuppressWarnings("unchecked")
    public LLMGateway.ReportNarrativeResult generateReportNarrative(LLMGateway.ReportNarrativeRequest request) {
        String endpoint = integrationGatewayProperties.getLlm().getAnalysisEndpoint();
        if (!integrationGatewayProperties.getLlm().isEnabled() || endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("LLM analysis endpoint is not configured.");
        }

        Map<String, Object> response = restClient.post()
                .uri(endpoint)
                .headers(headers -> applyApiKey(headers, integrationGatewayProperties.getLlm().getApiKey()))
                .body(Map.of(
                        "task", "arbitrage_report",
                        "product_title", request.productTitle(),
                        "market", request.market(),
                        "rewritten_query", request.rewrittenQuery(),
                        "decision", request.decision(),
                        "risk_level", request.riskLevel(),
                        "target_selling_price", request.targetSellingPrice(),
                        "total_cost", request.totalCost(),
                        "estimated_profit", request.estimatedProfit(),
                        "estimated_margin", request.estimatedMargin(),
                        "benchmark_title", request.benchmarkTitle(),
                        "domestic_match_titles", request.domesticMatchTitles()
                ))
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("LLM analysis endpoint returned empty response.");
        }

        Object summaryValue = response.containsKey("summary_text")
                ? response.get("summary_text")
                : response.get("summaryText");
        String summaryText = summaryValue == null ? "" : summaryValue.toString().trim();
        if (summaryText.isBlank()) {
            throw new IllegalStateException("LLM analysis endpoint returned blank summary.");
        }

        return new LLMGateway.ReportNarrativeResult(
                summaryText,
                readStringList(response.get("recommendations")),
                readStringList(response.containsKey("risk_notes") ? response.get("risk_notes") : response.get("riskNotes")),
                false,
                "HTTP_LLM",
                null
        );
    }

    private void applyApiKey(org.springframework.http.HttpHeaders headers, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
    }

    private List<String> readStringList(Object value) {
        List<String> items = new ArrayList<>();
        if (value instanceof List<?> list) {
            list.stream()
                    .filter(item -> item != null && !item.toString().isBlank())
                    .map(Object::toString)
                    .forEach(items::add);
        }
        return items;
    }
}
