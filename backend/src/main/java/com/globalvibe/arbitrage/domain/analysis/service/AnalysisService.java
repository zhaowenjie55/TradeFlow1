package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisRunRequest;
import com.globalvibe.arbitrage.domain.analysis.dto.AnalysisRunResponse;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisReport;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisTaskStatus;
import com.globalvibe.arbitrage.domain.analysis.model.AnalysisTrace;
import com.globalvibe.arbitrage.domain.analysis.model.CandidateMatch;
import com.globalvibe.arbitrage.domain.analysis.model.ReasoningStep;
import com.globalvibe.arbitrage.domain.analysis.model.RiskLevel;
import com.globalvibe.arbitrage.domain.detail.dto.DetailRequest;
import com.globalvibe.arbitrage.domain.detail.dto.ProductDetailResponse;
import com.globalvibe.arbitrage.domain.detail.service.DetailService;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AnalysisService {

    private static final BigDecimal SHIPPING_COST = new BigDecimal("2.50");
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");
    private static final int MAX_MATCHES = 5;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    private final DetailService detailService;
    private final LLMGateway llmGateway;

    public AnalysisService(DetailService detailService, LLMGateway llmGateway) {
        this.detailService = detailService;
        this.llmGateway = llmGateway;
    }

    public AnalysisRunResponse runAnalysis(AnalysisRunRequest request) {
        String taskId = "task-" + UUID.randomUUID();
        AnalysisTask task = new AnalysisTask(
                taskId,
                request.externalItemId(),
                AnalysisTaskStatus.RUNNING,
                OffsetDateTime.now()
        );

        ProductDetailResponse overseas = detailService.getAmazonDetail(new DetailRequest(request.externalItemId()));
        List<CandidateMatch> matches = matchDomesticProducts(overseas).stream()
                .sorted(Comparator
                        .comparingDouble(CandidateMatch::similarityScore).reversed()
                        .thenComparing(CandidateMatch::price))
                .limit(MAX_MATCHES)
                .toList();

        BigDecimal overseasPrice = parsePrice(overseas.price());
        CandidateMatch bestMatch = matches.isEmpty() ? null : matches.get(0);

        List<ReasoningStep> traceSteps = new ArrayList<>();
        traceSteps.add(reasonMatching(overseas, matches, bestMatch));

        ProfitResult profitResult = computeProfit(overseasPrice, bestMatch);
        traceSteps.add(reasonProfit(overseasPrice, bestMatch, profitResult));

        RiskAssessment risk = evaluateRisk(overseas, bestMatch, matches, profitResult.marginPercent());
        traceSteps.add(reasonRisk(bestMatch, matches, overseas, profitResult, risk));

        String markdownBody = buildMarkdown(overseas, matches, profitResult, risk);
        ReasoningStep finalStep = reasonFinalReport(overseas, profitResult, risk, markdownBody);
        traceSteps.add(finalStep);

        AnalysisReport report = new AnalysisReport(
                task.id(),
                buildSummary(risk.riskLevel(), profitResult.marginPercent()),
                profitResult.profit(),
                risk.riskLevel(),
                calculateConfidenceScore(bestMatch, matches.size()),
                appendNarrativeConclusion(markdownBody, finalStep.explanation()),
                risk.reasons()
        );

        return new AnalysisRunResponse(task.id(), AnalysisTaskStatus.DONE, report, new AnalysisTrace(traceSteps));
    }

    private ReasoningStep reasonMatching(
            ProductDetailResponse overseas,
            List<CandidateMatch> matches,
            CandidateMatch bestMatch
    ) {
        String inputSummary = "Evaluate domestic matches for overseas product and choose the best candidate.";
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("overseas_title", overseas.title());
        context.put("overseas_price", overseas.price());
        context.put("candidate_count", matches.size());
        context.put("top_candidates", matches.stream().limit(3).map(this::toMatchContext).toList());
        context.put("best_match_title", bestMatch == null ? null : bestMatch.title());

        return reasoningStep(
                "matching",
                inputSummary,
                """
                You are an e-commerce arbitrage expert.
                Task:
                1. Choose whether the selected best match is reliable.
                2. Explain why this match is or is not suitable.
                Return concise decision and explanation.
                """,
                context
        );
    }

    private ReasoningStep reasonProfit(
            BigDecimal overseasPrice,
            CandidateMatch bestMatch,
            ProfitResult profitResult
    ) {
        String inputSummary = "Interpret deterministic profit calculation.";
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("formula", "profit = overseas_price - domestic_price - shipping_cost - platform_fee");
        context.put("overseas_price", overseasPrice);
        context.put("domestic_price", bestMatch == null ? null : bestMatch.price());
        context.put("shipping_cost", SHIPPING_COST);
        context.put("platform_fee_rate", PLATFORM_FEE_RATE);
        context.put("estimated_profit", profitResult.profit());
        context.put("margin_percent", profitResult.marginPercent());

        return reasoningStep(
                "profit_analysis",
                inputSummary,
                """
                Explain whether the computed margin is attractive or weak for arbitrage.
                Use only the provided numbers and keep reasoning concrete.
                """,
                context
        );
    }

    private ReasoningStep reasonRisk(
            CandidateMatch bestMatch,
            List<CandidateMatch> matches,
            ProductDetailResponse overseas,
            ProfitResult profitResult,
            RiskAssessment riskAssessment
    ) {
        String inputSummary = "Explain rule-based risk classification.";
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("best_match_score", bestMatch == null ? null : bestMatch.similarityScore());
        context.put("match_count", matches.size());
        context.put("margin_percent", profitResult.marginPercent());
        context.put("overseas_rating", overseas.rating());
        context.put("rule_risk_level", riskAssessment.riskLevel().name());
        context.put("rule_reasons", riskAssessment.reasons());

        return reasoningStep(
                "risk_analysis",
                inputSummary,
                """
                Explain the risk level and main uncertainty drivers.
                Keep explanation aligned with deterministic rules.
                """,
                context
        );
    }

    private ReasoningStep reasonFinalReport(
            ProductDetailResponse overseas,
            ProfitResult profitResult,
            RiskAssessment riskAssessment,
            String markdown
    ) {
        String inputSummary = "Produce concise natural-language final assessment.";
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("product_title", overseas.title());
        context.put("margin_percent", profitResult.marginPercent());
        context.put("risk_level", riskAssessment.riskLevel().name());
        context.put("current_report_markdown", markdown);

        return reasoningStep(
                "final_report",
                inputSummary,
                """
                Rewrite the conclusion in persuasive but factual language.
                Do not change facts. Focus on clarity for business decision-making.
                """,
                context
        );
    }

    private ReasoningStep reasoningStep(
            String stepName,
            String inputSummary,
            String prompt,
            Map<String, Object> context
    ) {
        LLMGateway.ReasoningResult reasoning = llmGateway.generateReasoning(
                new LLMGateway.ReasoningRequest(stepName, prompt, context)
        );
        return new ReasoningStep(
                stepName,
                inputSummary,
                reasoning.decision(),
                reasoning.explanation()
        );
    }

    private Map<String, Object> toMatchContext(CandidateMatch match) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("domestic_item_id", match.domesticItemId());
        data.put("title", match.title());
        data.put("price", match.price());
        data.put("similarity_score", round(match.similarityScore(), 4));
        data.put("platform", match.platform());
        return data;
    }

    private List<CandidateMatch> matchDomesticProducts(ProductDetailResponse overseas) {
        List<DomesticItem> domesticItems = mockDomesticDataset();
        List<String> overseasTokens = tokenize((overseas.title() == null ? "" : overseas.title()) + " " +
                (overseas.description() == null ? "" : overseas.description()));
        BigDecimal overseasPrice = parsePrice(overseas.price());

        List<CandidateMatch> matches = new ArrayList<>();
        for (DomesticItem item : domesticItems) {
            double tokenScore = tokenSimilarity(overseasTokens, tokenize(item.title()));
            double containsBoost = containsBoost(overseas.title(), item.title());
            double priceScore = priceSimilarity(overseasPrice, item.price());
            double finalScore = clamp((tokenScore * 0.55) + (priceScore * 0.35) + (containsBoost * 0.10));

            if (finalScore >= 0.20) {
                matches.add(new CandidateMatch(
                        item.id(),
                        item.title(),
                        item.price(),
                        finalScore,
                        item.platform(),
                        item.imageUrl()
                ));
            }
        }
        return matches;
    }

    private ProfitResult computeProfit(BigDecimal overseasPrice, CandidateMatch bestMatch) {
        if (bestMatch == null) {
            return new ProfitResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal domesticPrice = bestMatch.price();
        BigDecimal platformFee = overseasPrice.multiply(PLATFORM_FEE_RATE);
        BigDecimal profit = overseasPrice
                .subtract(domesticPrice)
                .subtract(SHIPPING_COST)
                .subtract(platformFee)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal marginPercent = overseasPrice.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : profit.divide(overseasPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        return new ProfitResult(profit, marginPercent);
    }

    private RiskAssessment evaluateRisk(
            ProductDetailResponse overseas,
            CandidateMatch bestMatch,
            List<CandidateMatch> matches,
            BigDecimal marginPercent
    ) {
        List<String> reasons = new ArrayList<>();
        int score = 0;

        if (bestMatch == null) {
            reasons.add("No clear domestic match found.");
            score += 3;
        } else if (bestMatch.similarityScore() < 0.45) {
            reasons.add("Match confidence is low.");
            score += 2;
        }

        if (marginPercent.compareTo(new BigDecimal("12")) < 0) {
            reasons.add("Price difference is small, margin is thin.");
            score += 2;
        }

        if (matches.size() >= 5) {
            reasons.add("Competitive domestic supply is high.");
            score += 1;
        }

        Double rating = overseas.rating();
        if (rating != null && rating < 4.0D) {
            reasons.add("Overseas rating is relatively low.");
            score += 1;
        }

        RiskLevel level;
        if (score >= 4) {
            level = RiskLevel.HIGH;
        } else if (score >= 2) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }

        if (reasons.isEmpty()) {
            reasons.add("Stable price spread and strong match quality.");
        }
        return new RiskAssessment(level, reasons);
    }

    private double calculateConfidenceScore(CandidateMatch bestMatch, int matchCount) {
        if (bestMatch == null) {
            return 0.2D;
        }
        double score = (bestMatch.similarityScore() * 0.8D) + (Math.min(matchCount, 5) / 5.0D * 0.2D);
        return Math.max(0D, Math.min(1D, round(score, 2)));
    }

    private String buildSummary(RiskLevel riskLevel, BigDecimal marginPercent) {
        return "Estimated margin " + marginPercent + "% with " + riskLevel.name() + " risk.";
    }

    private String buildMarkdown(
            ProductDetailResponse overseas,
            List<CandidateMatch> matches,
            ProfitResult profit,
            RiskAssessment risk
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Product Analysis Report\n\n");
        builder.append("## Product\n");
        builder.append(overseas.title() == null ? "Unknown product" : overseas.title()).append("\n\n");
        builder.append("## Matches\n");
        if (matches.isEmpty()) {
            builder.append("- No strong domestic matches found.\n");
        } else {
            for (CandidateMatch match : matches) {
                builder.append("- ")
                        .append(match.title())
                        .append(" (")
                        .append(match.platform())
                        .append(") - $")
                        .append(match.price())
                        .append(" | score: ")
                        .append(round(match.similarityScore(), 2))
                        .append("\n");
            }
        }
        builder.append("\n## Profit\n");
        builder.append("Estimated profit: $").append(profit.profit()).append("\n\n");
        builder.append("Estimated margin: ").append(profit.marginPercent()).append("%\n\n");
        builder.append("## Risks\n");
        for (String reason : risk.reasons()) {
            builder.append("- ").append(reason).append("\n");
        }
        builder.append("\n## Conclusion\n");
        builder.append("This product has ").append(risk.riskLevel().name().toLowerCase(Locale.ROOT))
                .append(" arbitrage risk with current assumptions.\n");
        return builder.toString();
    }

    private String appendNarrativeConclusion(String markdown, String llmConclusion) {
        if (llmConclusion == null || llmConclusion.isBlank()) {
            return markdown;
        }
        return markdown + "\n### Agent Narrative\n" + llmConclusion.trim() + "\n";
    }

    private List<DomesticItem> mockDomesticDataset() {
        return List.of(
                new DomesticItem("1688-1001", "Portable USB Blender 380ml", new BigDecimal("8.20"), "1688", "https://img.example.com/1688-1001.jpg"),
                new DomesticItem("tb-2001", "Mini Fruit Juice Blender Cup", new BigDecimal("7.90"), "taobao", "https://img.example.com/tb-2001.jpg"),
                new DomesticItem("1688-1002", "Household Smoothie Mixer Bottle", new BigDecimal("10.10"), "1688", "https://img.example.com/1688-1002.jpg"),
                new DomesticItem("tb-2002", "Portable Electric Juicer Blender", new BigDecimal("9.50"), "taobao", "https://img.example.com/tb-2002.jpg"),
                new DomesticItem("1688-1003", "Travel Blender Cup Rechargeable", new BigDecimal("11.30"), "1688", "https://img.example.com/1688-1003.jpg"),
                new DomesticItem("tb-2003", "Kitchen Ice Crusher Blender", new BigDecimal("15.80"), "taobao", "https://img.example.com/tb-2003.jpg")
        );
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] parts = TOKEN_SPLIT.split(text.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 3) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private double tokenSimilarity(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0D;
        }
        long overlap = left.stream().filter(right::contains).distinct().count();
        long union = left.stream().distinct().count() + right.stream().distinct().count() - overlap;
        return union == 0 ? 0D : (double) overlap / union;
    }

    private double containsBoost(String overseasTitle, String domesticTitle) {
        if (overseasTitle == null || domesticTitle == null) {
            return 0D;
        }
        String left = overseasTitle.toLowerCase(Locale.ROOT);
        String right = domesticTitle.toLowerCase(Locale.ROOT);
        return left.contains(right) || right.contains(left) ? 1D : 0D;
    }

    private double priceSimilarity(BigDecimal overseasPrice, BigDecimal domesticPrice) {
        if (overseasPrice.compareTo(BigDecimal.ZERO) <= 0 || domesticPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0D;
        }
        BigDecimal ratio = domesticPrice.divide(overseasPrice, 4, RoundingMode.HALF_UP);
        double val = 1D - Math.abs(0.4D - ratio.doubleValue());
        return clamp(val);
    }

    private BigDecimal parsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = rawPrice.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private double clamp(double value) {
        if (value < 0D) {
            return 0D;
        }
        return Math.min(value, 1D);
    }

    private record DomesticItem(
            String id,
            String title,
            BigDecimal price,
            String platform,
            String imageUrl
    ) {
    }

    private record ProfitResult(
            BigDecimal profit,
            BigDecimal marginPercent
    ) {
    }

    private record RiskAssessment(
            RiskLevel riskLevel,
            List<String> reasons
    ) {
    }
}
