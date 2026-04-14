package com.globalvibe.arbitrage.integration.llm;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SimulatedLLMGateway {

    private static final Set<String> ENGLISH_STOP_WORDS = Set.of(
            "the", "and", "with", "for", "from", "into", "award", "winner", "innovation", "multiple",
            "pets", "pet", "cat", "dog", "dogs", "cats", "automatic", "portable", "replacement", "filters",
            "filter", "grey", "gray", "plastic", "stainless", "steel", "oz", "l", "ml", "pack", "new",
            "version", "upgrade", "premium", "large", "small"
    );

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "kitchen storage", List.of("kitchen storage", "kitchen organizer", "cabinet organizer"),
            "pet fountain", List.of("pet fountain", "cat water fountain", "dog water dispenser"),
            "wireless earbuds", List.of("wireless earbuds", "bluetooth earbuds", "true wireless earbuds"),
            "desk organizer", List.of("desk organizer", "desktop organizer", "acrylic organizer")
    );
    private static final Set<String> TRANSCRIPT_STOP_WORDS = Set.of(
            "i", "want", "need", "find", "show", "me", "some", "a", "an", "the", "to", "for", "from",
            "that", "can", "be", "is", "are", "with", "and", "or", "of", "in", "on", "my", "please",
            "cheap", "budget", "trending", "products", "product", "sourcing", "source"
    );
    private static final Pattern HAN_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}");

    public LLMGateway.RewriteResult rewriteTitle(String sourceTitle, String fallbackReason) {
        String normalized = sourceTitle == null ? "" : sourceTitle.toLowerCase(Locale.ROOT);
        if (normalized.contains("coffee")) {
            return simulatedRewrite(
                    buildCoffeeRewrite(normalized),
                    buildCoffeeKeywords(normalized),
                    fallbackReason
            );
        }
        if (normalized.contains("cat water fountain")
                || normalized.contains("pet fountain")
                || normalized.contains("dog water dispenser")
                || normalized.contains("water fountain")) {
            LinkedHashSet<String> keywords = new LinkedHashSet<>(List.of(
                    "宠物饮水机",
                    "猫饮水机",
                    "自动饮水器",
                    "循环饮水器",
                    "宠物饮水器"
            ));
            if (normalized.contains("filter")
                    && !normalized.contains("fountain")
                    && !normalized.contains("dispenser")) {
                keywords.add("宠物饮水机滤芯");
            }
            return simulatedRewrite("宠物自动饮水机", new ArrayList<>(keywords), fallbackReason);
        }
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

        RewriteHeuristic heuristic = buildHeuristicRewrite(sourceTitle);
        return simulatedRewrite(heuristic.rewrittenText(), heuristic.keywords(), fallbackReason);
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

    public LLMGateway.TranscriptIntentResult analyzeTranscript(
            LLMGateway.TranscriptIntentRequest request,
            String fallbackReason
    ) {
        String transcript = request.transcript() == null ? "" : request.transcript().trim();
        String normalized = normalizeTranscriptForIntent(transcript.toLowerCase(Locale.ROOT));

        String category = detectCategory(normalized);
        List<String> keywords = deriveTranscriptKeywords(category, normalized);
        List<String> sellingPoints = deriveSellingPoints(normalized, category);
        List<String> painPoints = derivePainPoints(normalized);
        List<String> useCases = deriveUseCases(normalized, category);
        List<String> targetAudience = deriveTargetAudience(normalized, category);

        return new LLMGateway.TranscriptIntentResult(
                normalized.contains("source") || normalized.contains("1688") || normalized.contains("find")
                        ? "product_sourcing"
                        : "media_analysis",
                category,
                detectMarket(normalized),
                detectPriceLevel(normalized),
                detectSourcing(normalized),
                keywords,
                sellingPoints,
                painPoints,
                useCases,
                targetAudience,
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

    private RewriteHeuristic buildHeuristicRewrite(String sourceTitle) {
        if (sourceTitle == null || sourceTitle.isBlank()) {
            return new RewriteHeuristic("跨境选品候选商品", List.of("跨境选品", "国内货源"));
        }
        if (containsHan(sourceTitle)) {
            String normalized = sourceTitle.replaceAll("\\s+", " ").trim();
            return new RewriteHeuristic(
                    normalized,
                    List.of(normalized)
            );
        }

        String normalized = sourceTitle.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        LinkedHashSet<String> chineseKeywords = new LinkedHashSet<>();
        addPhraseIfMatched(chineseKeywords, normalized, "water fountain", "饮水机");
        addPhraseIfMatched(chineseKeywords, normalized, "water dispenser", "饮水器");
        addPhraseIfMatched(chineseKeywords, normalized, "fountain", "饮水机");
        addPhraseIfMatched(chineseKeywords, normalized, "organizer", "收纳架");
        addPhraseIfMatched(chineseKeywords, normalized, "storage", "收纳");
        addPhraseIfMatched(chineseKeywords, normalized, "display", "展示架");
        addPhraseIfMatched(chineseKeywords, normalized, "blender", "榨汁机");
        addPhraseIfMatched(chineseKeywords, normalized, "mouse", "无线鼠标");
        addPhraseIfMatched(chineseKeywords, normalized, "lamp", "台灯");
        addPhraseIfMatched(chineseKeywords, normalized, "earbud", "蓝牙耳机");
        addPhraseIfMatched(chineseKeywords, normalized, "headphone", "蓝牙耳机");
        addPhraseIfMatched(chineseKeywords, normalized, "pet", "宠物");
        addPhraseIfMatched(chineseKeywords, normalized, "cat", "猫");
        addPhraseIfMatched(chineseKeywords, normalized, "dog", "狗");
        addPhraseIfMatched(chineseKeywords, normalized, "filter", "滤芯");
        addPhraseIfMatched(chineseKeywords, normalized, "automatic", "自动");
        addPhraseIfMatched(chineseKeywords, normalized, "acrylic", "亚克力");

        if (chineseKeywords.contains("宠物") && chineseKeywords.contains("饮水机")) {
            chineseKeywords.remove("猫");
            chineseKeywords.remove("狗");
            chineseKeywords.add("猫饮水机");
            chineseKeywords.add("自动饮水器");
        }

        if (!chineseKeywords.isEmpty()) {
            String rewritten = String.join("", chineseKeywords.stream().limit(3).toList());
            return new RewriteHeuristic(rewritten, new ArrayList<>(chineseKeywords));
        }

        LinkedHashSet<String> fallbackKeywords = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank() || token.length() < 3 || ENGLISH_STOP_WORDS.contains(token) || token.matches("\\d+")) {
                continue;
            }
            fallbackKeywords.add(token);
            if (fallbackKeywords.size() >= 4) {
                break;
            }
        }
        if (fallbackKeywords.isEmpty()) {
            return new RewriteHeuristic("跨境选品候选商品", List.of("跨境选品", "国内货源"));
        }
        String rewritten = String.join(" ", fallbackKeywords);
        return new RewriteHeuristic(rewritten, new ArrayList<>(fallbackKeywords));
    }

    private String buildCoffeeRewrite(String normalized) {
        if (normalized.contains("ground")) {
            if (normalized.contains("hawaiian")) {
                return "夏威夷咖啡粉";
            }
            return "研磨咖啡粉";
        }
        if (normalized.contains("bean")) {
            if (normalized.contains("hawaiian")) {
                return "夏威夷咖啡豆";
            }
            return "咖啡豆";
        }
        if (normalized.contains("instant")) {
            return "速溶咖啡";
        }
        if (normalized.contains("hawaiian")) {
            return "夏威夷咖啡";
        }
        return "咖啡";
    }

    private List<String> buildCoffeeKeywords(String normalized) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (normalized.contains("hawaiian")) {
            keywords.add("夏威夷咖啡");
        }
        if (normalized.contains("ground")) {
            keywords.add("咖啡粉");
            keywords.add("研磨咖啡");
        } else if (normalized.contains("bean")) {
            keywords.add("咖啡豆");
        } else if (normalized.contains("instant")) {
            keywords.add("速溶咖啡");
        }
        if (normalized.contains("vanilla")) {
            keywords.add("香草咖啡");
        }
        if (normalized.contains("macadamia")) {
            keywords.add("夏威夷果咖啡");
        }
        keywords.add("咖啡");
        return new ArrayList<>(keywords);
    }

    private void addPhraseIfMatched(LinkedHashSet<String> keywords, String normalizedSource, String englishPhrase, String chinesePhrase) {
        if (normalizedSource.contains(englishPhrase)) {
            keywords.add(chinesePhrase);
        }
    }

    private boolean containsHan(String value) {
        return value != null && value.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private record RewriteHeuristic(String rewrittenText, List<String> keywords) {
    }

    private String detectCategory(String normalized) {
        if (normalized.contains("kitchen")) {
            return "kitchen storage";
        }
        if (normalized.contains("earbud") || normalized.contains("headphone")) {
            return "wireless earbuds";
        }
        if (normalized.contains("pet fountain")
                || normalized.contains("cat water fountain")
                || normalized.contains("dog water dispenser")
                || normalized.contains("cat water")
                || (normalized.contains("water") && normalized.contains("dispenser"))) {
            return "pet fountain";
        }
        if (normalized.contains("organizer") || normalized.contains("storage")) {
            return "desk organizer";
        }
        return null;
    }

    private String detectMarket(String normalized) {
        if (normalized.contains("united states") || normalized.contains(" us ") || normalized.contains("u.s.") || normalized.contains("america")) {
            return "US";
        }
        if (normalized.contains("china") || normalized.contains("cn")) {
            return "CN";
        }
        if (normalized.contains("europe") || normalized.contains("eu")) {
            return "EU";
        }
        return "unknown";
    }

    private String detectPriceLevel(String normalized) {
        if (normalized.contains("cheap") || normalized.contains("budget") || normalized.contains("low cost")) {
            return "low";
        }
        if (normalized.contains("premium") || normalized.contains("luxury")) {
            return "premium";
        }
        if (normalized.isBlank()) {
            return "unknown";
        }
        return "mid";
    }

    private String detectSourcing(String normalized) {
        if (normalized.contains("1688")) {
            return "1688";
        }
        if (normalized.contains("taobao")) {
            return "taobao";
        }
        if (normalized.contains("amazon")) {
            return "amazon";
        }
        return "unknown";
    }

    private List<String> deriveTranscriptKeywords(String category, String normalized) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (category != null && !category.isBlank()) {
            keywords.addAll(CATEGORY_KEYWORDS.getOrDefault(category, List.of(category)));
        }
        keywords.addAll(extractTranscriptKeywords(normalized));
        if (normalized.contains("cheap")) {
            keywords.add("budget");
        }
        if (normalized.contains("trending")) {
            keywords.add("trending");
        }
        return keywords.stream().filter(value -> value != null && !value.isBlank()).limit(6).toList();
    }

    private List<String> deriveSellingPoints(String normalized, String category) {
        List<String> points = new ArrayList<>();
        if (normalized.contains("trending")) {
            points.add("具备趋势热度信号");
        }
        if ("wireless earbuds".equals(category)) {
            points.add("无线便携，适合高频消费电子场景");
        }
        if ("pet fountain".equals(category)) {
            points.add("自动循环饮水提升宠物喂养便利性");
        }
        if (points.isEmpty()) {
            points.add("转写内容强调了明确的产品需求与采购目的");
        }
        return points;
    }

    private List<String> derivePainPoints(String normalized) {
        List<String> pains = new ArrayList<>();
        if (normalized.contains("cheap") || normalized.contains("budget")) {
            pains.add("用户对采购成本敏感");
        }
        if (normalized.contains("source") || normalized.contains("sourced")) {
            pains.add("需要稳定的国内供货渠道");
        }
        if (pains.isEmpty()) {
            pains.add("需要把口语化需求压缩成可检索的结构化关键词");
        }
        return pains;
    }

    private List<String> deriveUseCases(String normalized, String category) {
        List<String> cases = new ArrayList<>();
        if ("kitchen storage".equals(category)) {
            cases.add("厨房收纳与整理");
        }
        if ("wireless earbuds".equals(category)) {
            cases.add("通勤与运动佩戴");
        }
        if ("pet fountain".equals(category)) {
            cases.add("家庭宠物日常喂养");
        }
        if (cases.isEmpty()) {
            cases.add("跨境选品与国内寻源");
        }
        return cases;
    }

    private List<String> deriveTargetAudience(String normalized, String category) {
        List<String> audience = new ArrayList<>();
        if ("pet fountain".equals(category)) {
            audience.add("猫狗等宠物家庭");
        }
        if ("wireless earbuds".equals(category)) {
            audience.add("消费电子用户");
        }
        if (normalized.contains("us")) {
            audience.add("美国市场用户");
        }
        if (audience.isEmpty()) {
            audience.add("跨境电商卖家");
        }
        return audience;
    }

    private List<String> extractTranscriptKeywords(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }

        if (containsHan(normalized)) {
            LinkedHashSet<String> hanKeywords = new LinkedHashSet<>();
            Matcher matcher = HAN_SEGMENT_PATTERN.matcher(normalized);
            while (matcher.find()) {
                String candidate = matcher.group().trim();
                if (!candidate.isBlank()) {
                    hanKeywords.add(candidate);
                }
                if (hanKeywords.size() >= 4) {
                    break;
                }
            }
            return new ArrayList<>(hanKeywords);
        }

        LinkedHashSet<String> phraseKeywords = new LinkedHashSet<>();
        String cleaned = normalized
                .replaceAll("[^a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String[] tokens = cleaned.split("\\s+");

        List<String> meaningfulTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank() || token.length() < 3 || token.matches("\\d+") || TRANSCRIPT_STOP_WORDS.contains(token)) {
                continue;
            }
            meaningfulTokens.add(token);
        }

        for (int i = 0; i < meaningfulTokens.size(); i++) {
            phraseKeywords.add(meaningfulTokens.get(i));
            if (i + 1 < meaningfulTokens.size()) {
                phraseKeywords.add(meaningfulTokens.get(i) + " " + meaningfulTokens.get(i + 1));
            }
            if (phraseKeywords.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(phraseKeywords);
    }

    private String normalizeTranscriptForIntent(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }

        String corrected = normalized
                .replaceAll("[^\\p{IsHan}a-z0-9\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        corrected = corrected.replaceAll("\\bcatch water fountain\\b", "cat water fountain");
        corrected = corrected.replaceAll("\\bcatch water dispenser\\b", "cat water dispenser");
        corrected = corrected.replaceAll("\\bcatch water\\b", "cat water");
        corrected = corrected.replaceAll("\\bcatch fountain\\b", "cat fountain");
        corrected = corrected.replaceAll("\\bear buds\\b", "earbuds");
        corrected = corrected.replaceAll("\\bair buds\\b", "earbuds");
        corrected = corrected.replaceAll("\\bblue tooth\\b", "bluetooth");
        corrected = corrected.replaceAll("\\s+", " ").trim();

        if (corrected.contains("cat water") && corrected.contains("dispenser") && !corrected.contains("cat water fountain")) {
            corrected = corrected + " cat water fountain";
        }
        if (corrected.contains("cat water") && !corrected.contains("pet fountain")) {
            corrected = corrected + " pet fountain";
        }

        return corrected;
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
