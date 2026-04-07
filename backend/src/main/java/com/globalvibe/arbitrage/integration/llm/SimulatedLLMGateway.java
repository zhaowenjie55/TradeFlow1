package com.globalvibe.arbitrage.integration.llm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SimulatedLLMGateway {

    public LLMGateway.RewriteResult rewriteTitle(String sourceTitle, String fallbackReason) {
        String normalized = sourceTitle == null ? "" : sourceTitle.toLowerCase(Locale.ROOT);
        if (normalized.contains("mouse")) {
            return simulatedRewrite("充电无线鼠标", List.of("无线鼠标", "静音鼠标", "办公鼠标"), fallbackReason);
        }
        if (normalized.contains("blender")) {
            return simulatedRewrite("便携果汁机", List.of("便携榨汁杯", "迷你果汁机", "充电榨汁杯"), fallbackReason);
        }
        if (normalized.contains("lamp")) {
            return simulatedRewrite("护眼 LED 台灯", List.of("护眼台灯", "桌面台灯", "可调光台灯"), fallbackReason);
        }
        if (normalized.contains("earbud") || normalized.contains("headphone")) {
            return simulatedRewrite("无线蓝牙耳机", List.of("蓝牙耳机", "入耳式耳机", "运动耳机"), fallbackReason);
        }
        if (normalized.contains("acrylic") || normalized.contains("organizer")) {
            return simulatedRewrite(
                    "亚克力透明收纳架",
                    List.of("亚克力透明收纳架", "亚克力桌面收纳架", "亚克力展示架"),
                    fallbackReason
            );
        }

        String sanitized = sourceTitle == null ? "" : sourceTitle.replaceAll("[^\\p{IsHan}a-zA-Z0-9\\s-]", "").trim();
        String rewritten = sanitized.isBlank() ? "跨境选品候选商品" : sanitized + " 国内货源";
        return simulatedRewrite(rewritten, List.of(rewritten), fallbackReason);
    }

    public LLMGateway.ReportNarrativeResult generateReportNarrative(
            LLMGateway.ReportNarrativeRequest request,
            String fallbackReason
    ) {
        String benchmark = request.benchmarkTitle() == null || request.benchmarkTitle().isBlank()
                ? "当前数据库中最接近的历史货源"
                : request.benchmarkTitle();
        String summary = buildSummary(request, benchmark);
        List<String> recommendations = buildRecommendations(request, benchmark);
        List<String> riskNotes = buildRiskNotes(request, fallbackReason);
        return new LLMGateway.ReportNarrativeResult(
                summary,
                recommendations,
                riskNotes,
                true,
                "SIMULATED_LLM",
                "simulated-llm",
                normalizeFallbackReason(fallbackReason),
                OffsetDateTime.now()
        );
    }

    public LLMGateway.ReasoningResult generateReasoning(
            LLMGateway.ReasoningRequest request,
            String fallbackReason
    ) {
        String step = request.stepName() == null ? "unknown" : request.stepName();
        Map<String, Object> context = request.context() == null ? Map.of() : request.context();
        String decision;
        String explanation;
        double confidence;

        if ("matching".equals(step)) {
            Object best = context.get("best_match_title");
            decision = best == null ? "No high-confidence domestic match." : "Selected best domestic match.";
            explanation = "Rule-based scorer compared keyword overlap and price relationship, then chose the highest-score candidate.";
            confidence = best == null ? 0.35D : 0.72D;
        } else if ("profit_analysis".equals(step)) {
            decision = "Profit computed with deterministic formula.";
            explanation = "System used overseas price, domestic cost, fixed shipping, and platform fee ratio to estimate margin.";
            confidence = 0.8D;
        } else if ("risk_analysis".equals(step)) {
            decision = "Risk level derived from rule thresholds.";
            explanation = "System weighted match confidence, margin floor, competition count, and rating to produce risk classification.";
            confidence = 0.76D;
        } else if ("final_report".equals(step)) {
            decision = "Generated narrative summary from computed facts.";
            explanation = "Final explanation keeps deterministic outputs unchanged and only rewrites them for readability.";
            confidence = 0.74D;
        } else {
            decision = "Reasoning step completed.";
            explanation = "Fallback reasoning generated in simulated mode.";
            confidence = 0.6D;
        }

        return new LLMGateway.ReasoningResult(
                decision,
                explanation,
                confidence,
                true,
                "SIMULATED_LLM",
                "simulated-llm",
                normalizeFallbackReason(fallbackReason),
                OffsetDateTime.now()
        );
    }

    private LLMGateway.RewriteResult simulatedRewrite(String rewrittenText, List<String> keywords, String fallbackReason) {
        return new LLMGateway.RewriteResult(
                rewrittenText,
                keywords,
                true,
                "SIMULATED_LLM",
                "simulated-llm",
                normalizeFallbackReason(fallbackReason),
                OffsetDateTime.now()
        );
    }

    private String buildSummary(LLMGateway.ReportNarrativeRequest request, String benchmark) {
        String margin = formatNumber(request.estimatedMargin(), "--");
        String profit = formatCurrency(request.estimatedProfit(), "--");
        return "Agent 结合候选商品、历史货源和成本拆解后判断，"
                + request.productTitle()
                + " 在 "
                + request.market()
                + " 站点当前具备约 "
                + margin
                + "% 的利润空间，主要对标货源为 "
                + benchmark
                + "，当前可作为进一步人工复核与定向寻源的候选，最终预估利润约为 "
                + profit
                + "。";
    }

    private List<String> buildRecommendations(LLMGateway.ReportNarrativeRequest request, String benchmark) {
        String profit = formatCurrency(request.estimatedProfit(), "¥0.00");
        return List.of(
                "优先围绕 “" + request.rewrittenQuery() + "” 继续核对国内主卖点，确保展示中的标题改写和寻源逻辑一致。",
                "以 " + benchmark + " 作为当前对标货源，补充 MOQ、发货时效和可定制能力，增强报告说服力。",
                "将 " + profit + " 的预估利润作为当前试算结果，并结合 MOQ、头程、税费和平台政策做二次复核。"
        );
    }

    private List<String> buildRiskNotes(LLMGateway.ReportNarrativeRequest request, String fallbackReason) {
        String normalizedReason = normalizeFallbackReason(fallbackReason);
        return List.of(
                "当前结论优先服务于黑客松演示，真实上架前仍需补充运费、税费与平台政策校验。",
                "当前叙事由本地规则与模板化网关生成，不影响匹配、定价与利润测算的确定性结果。",
                normalizedReason
        );
    }

    private String normalizeFallbackReason(String fallbackReason) {
        if (fallbackReason == null || fallbackReason.isBlank()) {
            return "LLM 实时接口未启用，当前使用结构化叙事模板生成说明文案。";
        }
        return fallbackReason;
    }

    private String formatCurrency(BigDecimal value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return "¥" + value.stripTrailingZeros().toPlainString();
    }

    private String formatNumber(BigDecimal value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
