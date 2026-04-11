package com.globalvibe.arbitrage.integration.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;

@Component
public class HttpLLMGateway {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;

    private static final String REWRITE_SYSTEM_PROMPT = """
            你是一个跨境电商寻源助手。
            你的任务是把 Amazon 英文标题改写为适合 1688 搜索的中文关键词。
            你必须先去掉品牌名、营销词、奖项、容量、颜色、材质、包装数量等噪音，只保留商品品类词和核心功能词。
            结果必须适合中文电商平台检索，不要照搬英文长标题。
            例如：
            输入: "Veken Innovation Award Winner 95oz 2.8L Pet Fountain Automatic Cat Water Fountain Dog Water Dispenser..."
            输出:
            {
              "rewritten_text": "宠物自动饮水机",
              "keywords": ["宠物饮水机", "猫饮水机", "自动饮水器", "循环饮水器"]
            }
            输出必须是严格 JSON，不要输出 Markdown，不要输出解释。
            JSON 结构固定为：
            {
              "rewritten_text": "适合 1688 搜索的中文主标题",
              "keywords": ["扩展词1", "扩展词2", "扩展词3"]
            }
            rewritten_text 必须是简洁中文短语。
            keywords 必须是非空字符串数组，且每一项都必须是简洁中文搜索词。
            """;

    private static final String REPORT_SYSTEM_PROMPT = """
            你是一个跨境套利分析助手。
            你的任务是根据结构化套利上下文生成摘要、建议、风险说明。
            输出必须是严格 JSON，不要输出 Markdown，不要输出解释。
            JSON 结构固定为：
            {
              "summary_text": "中文摘要",
              "recommendations": ["建议1", "建议2", "建议3"],
              "risk_notes": ["风险1", "风险2", "风险3"]
            }
            三个字段都必须存在，且不能为空。
            所有金额都按人民币理解。
            """;

    private static final String REASONING_SYSTEM_PROMPT = """
            You are an e-commerce arbitrage expert.
            You must reason clearly and return strict JSON only.
            Do not output Markdown.
            Return exactly:
            {
              "decision": "short decision for this step",
              "explanation": "clear explanation based on provided facts",
              "confidence_score": 0.0
            }
            confidence_score must be between 0 and 1.
            """;

    private static final String TRANSCRIPT_INTENT_SYSTEM_PROMPT = """
            You are an e-commerce sourcing analyst.
            Convert the transcript into structured sourcing intent for downstream search and analysis.
            Do not echo the full transcript.
            Return strict JSON only.
            JSON schema:
            {
              "intent": "product_sourcing | media_analysis | unknown",
              "category": "short category phrase",
              "market": "US | CN | EU | GLOBAL | unknown",
              "price_level": "low | mid | premium | unknown",
              "sourcing": "1688 | taobao | amazon | unknown",
              "keywords": ["keyword 1", "keyword 2"],
              "selling_points": ["point 1", "point 2"],
              "pain_points": ["pain 1", "pain 2"],
              "use_cases": ["case 1", "case 2"],
              "target_audience": ["audience 1", "audience 2"]
            }
            keywords must be concise search-friendly phrases.
            All list fields must always be arrays, even if empty.
            """;

    private final RestClient restClient;
    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final ObjectMapper objectMapper;

    public HttpLLMGateway(
            RestClient.Builder restClientBuilder,
            IntegrationGatewayProperties integrationGatewayProperties,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.objectMapper = objectMapper;
    }

    public LLMGateway.RewriteResult rewriteTitle(String sourceTitle) {
        JsonNode content = invokeChat(List.of(
                message("system", REWRITE_SYSTEM_PROMPT),
                message("user", sourceTitle == null ? "" : sourceTitle)
        ));

        String rewrittenText = requiredText(content, "rewritten_text");
        List<String> keywords = requiredStringList(content, "keywords");
        return new LLMGateway.RewriteResult(
                rewrittenText,
                keywords,
                false,
                "GLM_CHAT",
                integrationGatewayProperties.getLlm().getModel(),
                null,
                OffsetDateTime.now()
        );
    }

    public LLMGateway.ReportNarrativeResult generateReportNarrative(LLMGateway.ReportNarrativeRequest request) {
        JsonNode content = invokeChat(List.of(
                message("system", REPORT_SYSTEM_PROMPT),
                message("user", toJson(buildNarrativeContext(request)))
        ));

        String summaryText = requiredText(content, "summary_text");
        List<String> recommendations = requiredStringList(content, "recommendations");
        List<String> riskNotes = requiredStringList(content, "risk_notes");
        return new LLMGateway.ReportNarrativeResult(
                summaryText,
                recommendations,
                riskNotes,
                false,
                "GLM_CHAT",
                integrationGatewayProperties.getLlm().getModel(),
                null,
                OffsetDateTime.now()
        );
    }

    public LLMGateway.ReasoningResult generateReasoning(LLMGateway.ReasoningRequest request) {
        JsonNode content = invokeChat(List.of(
                message("system", REASONING_SYSTEM_PROMPT),
                message("user", buildReasoningUserPrompt(request))
        ));

        String decision = requiredText(content, "decision");
        String explanation = requiredText(content, "explanation");
        double confidenceScore = requiredConfidence(content.path("confidence_score"));

        return new LLMGateway.ReasoningResult(
                decision,
                explanation,
                confidenceScore,
                false,
                "GLM_CHAT",
                integrationGatewayProperties.getLlm().getModel(),
                null,
                OffsetDateTime.now()
        );
    }

    public LLMGateway.TranscriptIntentResult analyzeTranscript(LLMGateway.TranscriptIntentRequest request) {
        JsonNode content = invokeChat(List.of(
                message("system", TRANSCRIPT_INTENT_SYSTEM_PROMPT),
                message("user", buildTranscriptIntentUserPrompt(request))
        ));

        return new LLMGateway.TranscriptIntentResult(
                requiredText(content, "intent"),
                optionalText(content, "category"),
                optionalText(content, "market"),
                optionalText(content, "price_level"),
                optionalText(content, "sourcing"),
                optionalStringList(content, "keywords"),
                optionalStringList(content, "selling_points"),
                optionalStringList(content, "pain_points"),
                optionalStringList(content, "use_cases"),
                optionalStringList(content, "target_audience"),
                false,
                "GLM_CHAT",
                integrationGatewayProperties.getLlm().getModel(),
                null,
                OffsetDateTime.now()
        );
    }

    private JsonNode invokeChat(List<Map<String, String>> messages) {
        IntegrationGatewayProperties.LlmProperties llm = integrationGatewayProperties.getLlm();
        if (!llm.isEnabled()) {
            throw new IllegalStateException("LLM chat gateway is disabled.");
        }
        if (llm.getChatEndpoint() == null || llm.getChatEndpoint().isBlank() || llm.getApiKey() == null || llm.getApiKey().isBlank()) {
            throw new IllegalStateException("LLM chat endpoint/api key is not configured.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", llm.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", llm.getTemperature());

        JsonNode response = restClient.post()
                .uri(llm.getChatEndpoint())
                .headers(headers -> applyApiKey(headers, llm.getApiKey()))
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("LLM chat gateway returned empty response.");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new IllegalStateException("LLM chat gateway returned empty content.");
        }

        String content = contentNode.isTextual() ? contentNode.asText() : contentNode.toString();
        String normalized = stripMarkdownFence(content);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("LLM chat gateway returned non-JSON response.", ex);
        }
    }

    private Map<String, Object> buildNarrativeContext(LLMGateway.ReportNarrativeRequest request) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("product_title", request.productTitle());
        context.put("market", request.market());
        context.put("rewritten_query", request.rewrittenQuery());
        context.put("rewritten_keywords", request.rewrittenKeywords());
        context.put("decision", request.decision());
        context.put("risk_level", request.riskLevel());
        context.put("amazon_price_usd", request.amazonPriceUsd());
        context.put("amazon_price_rmb", request.amazonPriceRmb());
        context.put("sourcing_cost", request.sourcingCost());
        context.put("domestic_shipping_cost", request.domesticShippingCost());
        context.put("logistics_cost", request.logisticsCost());
        context.put("platform_fee", request.platformFee());
        context.put("exchange_rate_cost", request.exchangeRateCost());
        context.put("total_cost", request.totalCost());
        context.put("estimated_profit", request.estimatedProfit());
        context.put("estimated_margin", request.estimatedMargin());
        context.put("benchmark_title", request.benchmarkTitle());
        context.put("domestic_match_titles", request.domesticMatchTitles());
        return context;
    }

    private String buildReasoningUserPrompt(LLMGateway.ReasoningRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("step_name", request.stepName());
        payload.put("task_prompt", request.prompt());
        payload.put("context", request.context() == null ? Map.of() : request.context());
        return toJson(payload);
    }

    private String buildTranscriptIntentUserPrompt(LLMGateway.TranscriptIntentRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source_type", request.sourceType());
        payload.put("transcript", request.transcript());
        return toJson(payload);
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").trim();
        if (value.isBlank()) {
            throw new IllegalStateException("LLM chat gateway returned blank field: " + fieldName);
        }
        return value;
    }

    private List<String> requiredStringList(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (!valueNode.isArray()) {
            throw new IllegalStateException("LLM chat gateway returned invalid array field: " + fieldName);
        }
        List<String> items = new ArrayList<>();
        valueNode.forEach(item -> {
            String value = item == null ? "" : item.asText("").trim();
            if (!value.isBlank()) {
                items.add(value);
            }
        });
        if (items.isEmpty()) {
            throw new IllegalStateException("LLM chat gateway returned empty array field: " + fieldName);
        }
        return items;
    }

    private String optionalText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").trim();
        return value.isBlank() ? null : value;
    }

    private List<String> optionalStringList(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (!valueNode.isArray()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        valueNode.forEach(item -> {
            String value = item == null ? "" : item.asText("").trim();
            if (!value.isBlank()) {
                items.add(value);
            }
        });
        return items;
    }

    private double requiredConfidence(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            throw new IllegalStateException("LLM chat gateway returned missing confidence_score");
        }
        double value = node.isNumber() ? node.asDouble() : parseDouble(node.asText());
        if (Double.isNaN(value) || value < 0D || value > 1D) {
            throw new IllegalStateException("LLM chat gateway returned invalid confidence_score");
        }
        return value;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private Map<String, String> message(String role, String content) {
        return Map.of(
                "role", role,
                "content", content == null ? "" : content
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize LLM context.", ex);
        }
    }

    private String stripMarkdownFence(String content) {
        String normalized = content == null ? "" : content.trim();
        if (!normalized.startsWith("```")) {
            return normalized;
        }
        int firstLineBreak = normalized.indexOf('\n');
        int lastFence = normalized.lastIndexOf("```");
        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return normalized.replace("```", "").trim();
        }
        return normalized.substring(firstLineBreak + 1, lastFence).trim();
    }

    private void applyApiKey(org.springframework.http.HttpHeaders headers, String apiKey) {
        headers.setBearerAuth(apiKey);
        headers.set("Content-Type", "application/json");
    }
}
