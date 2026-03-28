package com.globalvibe.arbitrage.integration.llm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
                normalizeFallbackReason(fallbackReason)
        );
    }

    private LLMGateway.RewriteResult simulatedRewrite(String rewrittenText, List<String> keywords, String fallbackReason) {
        return new LLMGateway.RewriteResult(
                rewrittenText,
                keywords,
                true,
                "SIMULATED_LLM",
                normalizeFallbackReason(fallbackReason)
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
                + "，当前更适合作为可解释、可演示的机会案例，最终预估利润约为 "
                + profit
                + "。";
    }

    private List<String> buildRecommendations(LLMGateway.ReportNarrativeRequest request, String benchmark) {
        String profit = formatCurrency(request.estimatedProfit(), "¥0.00");
        return List.of(
                "优先围绕 “" + request.rewrittenQuery() + "” 继续核对国内主卖点，确保展示中的标题改写和寻源逻辑一致。",
                "以 " + benchmark + " 作为当前对标货源，补充 MOQ、发货时效和可定制能力，增强报告说服力。",
                "将 " + profit + " 的预估利润作为演示口径，同时强调它基于模拟实时链路和数据库 fallback 结果生成。"
        );
    }

    private List<String> buildRiskNotes(LLMGateway.ReportNarrativeRequest request, String fallbackReason) {
        String normalizedReason = normalizeFallbackReason(fallbackReason);
        return List.of(
                "当前结论优先服务于黑客松演示，真实上架前仍需补充运费、税费与平台政策校验。",
                "系统已采用模拟实时获取加数据库 fallback 的模式，重点展示的是 agent 分析链路而非爬虫能力。",
                normalizedReason
        );
    }

    private String normalizeFallbackReason(String fallbackReason) {
        if (fallbackReason == null || fallbackReason.isBlank()) {
            return "LLM 实时接口未启用，已回退到结构化叙事模板。";
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
