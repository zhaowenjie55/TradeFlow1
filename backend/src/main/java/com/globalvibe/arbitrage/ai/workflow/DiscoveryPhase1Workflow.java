package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.marketplace.model.MarketplaceType;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.search.service.AmazonCrawlerProductSearchService;
import com.globalvibe.arbitrage.domain.search.service.SearchHistoryFallbackService;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class DiscoveryPhase1Workflow implements Phase1Workflow {

    private final TaskExecutionProperties taskExecutionProperties;
    private final AmazonCrawlerProductSearchService amazonCrawlerProductSearchService;
    private final ProductRepository productRepository;
    private final SearchHistoryFallbackService searchHistoryFallbackService;

    public DiscoveryPhase1Workflow(
            TaskExecutionProperties taskExecutionProperties,
            AmazonCrawlerProductSearchService amazonCrawlerProductSearchService,
            ProductRepository productRepository,
            SearchHistoryFallbackService searchHistoryFallbackService
    ) {
        this.taskExecutionProperties = taskExecutionProperties;
        this.amazonCrawlerProductSearchService = amazonCrawlerProductSearchService;
        this.productRepository = productRepository;
        this.searchHistoryFallbackService = searchHistoryFallbackService;
    }

    @Override
    public Phase1WorkflowResult run(AnalysisTask analysisTask) {
        List<TaskLogEntry> logs = new ArrayList<>();
        logs.add(log("phase1.market-scan", "开始执行 Amazon 候选检索，优先尝试实时抓取。"));

        int candidateLimit = resolveCandidateLimit(analysisTask);
        int sourceFetchLimit = Math.max(candidateLimit * 3, candidateLimit);
        boolean attemptedLiveSearch = shouldAttemptLiveSearch(analysisTask);
        boolean liveSearchUsed = false;
        String liveSearchFailureReason = null;

        List<Product> sourceProducts = List.of();
        if (attemptedLiveSearch) {
            logs.add(log("phase1.live-search", "开始调用 Python crawler 执行 Amazon 实时检索。"));
            try {
                sourceProducts = amazonCrawlerProductSearchService.search(analysisTask.getKeyword(), 1);
                if (sourceProducts.isEmpty()) {
                    logs.add(warnLog("phase1.live-search.empty", "实时检索未返回商品，切换至本地商品库/历史快照兜底。"));
                } else {
                    liveSearchUsed = true;
                    logs.add(log("phase1.live-search.hit", "实时检索成功，已获取 Amazon 候选商品。"));
                }
            } catch (RuntimeException ex) {
                liveSearchFailureReason = summarizeLiveSearchFailure(ex);
                logs.add(warnLog("phase1.live-search.failed", "实时检索失败，切换至本地商品库/历史快照兜底: " + liveSearchFailureReason));
            }
        }

        if (sourceProducts.isEmpty()) {
            logs.add(log("phase1.catalog-search", "开始执行商品库关键词检索。"));
            sourceProducts = productRepository.searchByPlatformAndKeyword(
                    MarketplaceType.AMAZON,
                    analysisTask.getKeyword(),
                    sourceFetchLimit
            );
            if (!sourceProducts.isEmpty()) {
                logs.add(log("phase1.catalog-search.hit", "商品库检索成功，已补齐 Amazon 候选。"));
            }
        }

        if (sourceProducts.isEmpty() && shouldAttemptHistoryFallback(analysisTask)) {
            logs.add(log("phase1.fallback", "商品库未命中，改用历史搜索快照补齐候选。"));
            sourceProducts = searchHistoryFallbackService.findLatestAmazonProducts(
                    analysisTask.getKeyword(),
                    sourceFetchLimit
            );
            if (!sourceProducts.isEmpty()) {
                logs.add(log("phase1.fallback.hit", "历史搜索快照命中，已恢复 Amazon 候选。"));
            }
        }
        if (sourceProducts.isEmpty()) {
            throw buildFailure(analysisTask, logs, attemptedLiveSearch, liveSearchFailureReason);
        }

        boolean fallbackUsed = attemptedLiveSearch && !liveSearchUsed;

        logs.add(log("phase1.filter", "已按关键词相似度、原始排序、口碑强度和价格合理性完成候选排序。"));

        List<ScoredCandidate> scoredCandidates = scoreCandidates(sourceProducts, analysisTask);
        List<CandidateProduct> candidates = scoredCandidates.stream()
                .limit(candidateLimit)
                .map(this::toCandidateProduct)
                .toList();

        logs.add(log("phase1.output", "候选商品已生成，可进入二阶段寻源分析。"));
        return new Phase1WorkflowResult(
                candidates,
                logs,
                scoredCandidates.stream().map(ScoredCandidate::product).toList(),
                fallbackUsed
        );
    }

    private List<ScoredCandidate> scoreCandidates(List<Product> sourceProducts, AnalysisTask analysisTask) {
        BigDecimal minPrice = sourceProducts.stream()
                .map(Product::price)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = sourceProducts.stream()
                .map(Product::price)
                .filter(price -> price != null)
                .max(BigDecimal::compareTo)
                .orElse(minPrice);

        List<ScoredCandidate> scored = new ArrayList<>();
        for (int index = 0; index < sourceProducts.size(); index++) {
            Product product = sourceProducts.get(index);
            BigDecimal querySimilarity = ratioScore(tokens(analysisTask.getKeyword()), tokens(product.title()), 35);
            BigDecimal originalRankScore = scoreOriginalRank(index, sourceProducts.size());
            BigDecimal reputationScore = scoreReputation(product.rating(), product.reviews());
            BigDecimal priceReasonability = scorePriceReasonability(product.price(), minPrice, maxPrice);
            BigDecimal totalScore = querySimilarity
                    .add(originalRankScore)
                    .add(reputationScore)
                    .add(priceReasonability)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal estimatedMargin = estimateMargin(totalScore, analysisTask.getTargetProfitMargin());
            scored.add(new ScoredCandidate(
                    product,
                    querySimilarity,
                    originalRankScore,
                    reputationScore,
                    priceReasonability,
                    totalScore,
                    estimatedMargin,
                    resolveRiskTag(totalScore),
                    buildRecommendationReason(product, querySimilarity, originalRankScore, reputationScore, priceReasonability)
            ));
        }

        scored.sort(Comparator
                .comparing(ScoredCandidate::totalScore, Comparator.reverseOrder())
                .thenComparing(item -> item.product().price(), Comparator.nullsLast(Comparator.naturalOrder())));
        return scored;
    }

    private CandidateProduct toCandidateProduct(ScoredCandidate candidate) {
        return new CandidateProduct(
                candidate.product().id(),
                candidate.product().title(),
                candidate.product().image(),
                candidate.product().platform().value(),
                candidate.product().price(),
                candidate.estimatedMargin(),
                candidate.riskTag(),
                candidate.recommendationReason(),
                candidate.totalScore().compareTo(new BigDecimal("58")) >= 0,
                candidate.product().link()
        );
    }

    private BigDecimal scoreOriginalRank(int index, int totalSize) {
        if (totalSize <= 1) {
            return new BigDecimal("20.00");
        }
        BigDecimal remaining = BigDecimal.valueOf(totalSize - index);
        return remaining
                .divide(BigDecimal.valueOf(totalSize), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("20"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scoreReputation(Double rating, Integer reviews) {
        BigDecimal ratingScore = rating == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(Math.min(1D, Math.max(0D, rating / 5D)))
                .multiply(new BigDecimal("12"));
        double reviewStrength = reviews == null || reviews <= 0
                ? 0D
                : Math.min(1D, Math.log10(reviews + 1) / 3D);
        BigDecimal reviewScore = BigDecimal.valueOf(reviewStrength).multiply(new BigDecimal("13"));
        return ratingScore.add(reviewScore).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scorePriceReasonability(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (maxPrice.compareTo(minPrice) <= 0) {
            return new BigDecimal("20.00");
        }
        BigDecimal normalized = maxPrice.subtract(price)
                .divide(maxPrice.subtract(minPrice), 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return normalized.multiply(new BigDecimal("20")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateMargin(BigDecimal totalScore, BigDecimal targetProfitMargin) {
        BigDecimal base = (targetProfitMargin == null ? new BigDecimal("0.25") : targetProfitMargin)
                .multiply(new BigDecimal("100"));
        BigDecimal adjustment = totalScore.subtract(new BigDecimal("50"))
                .divide(new BigDecimal("5"), 2, RoundingMode.HALF_UP);
        BigDecimal estimated = base.add(adjustment);
        if (estimated.compareTo(new BigDecimal("3.00")) < 0) {
            return new BigDecimal("3.00");
        }
        return estimated.min(new BigDecimal("55.00")).setScale(1, RoundingMode.HALF_UP);
    }

    private String resolveRiskTag(BigDecimal totalScore) {
        if (totalScore.compareTo(new BigDecimal("70")) >= 0) {
            return "低风险";
        }
        if (totalScore.compareTo(new BigDecimal("52")) >= 0) {
            return "中风险";
        }
        return "待核验";
    }

    private String buildRecommendationReason(
            Product product,
            BigDecimal querySimilarity,
            BigDecimal originalRankScore,
            BigDecimal reputationScore,
            BigDecimal priceReasonability
    ) {
        List<String> reasons = new ArrayList<>();
        if (querySimilarity.compareTo(new BigDecimal("22")) >= 0) {
            reasons.add("关键词相关性较高");
        }
        if (originalRankScore.compareTo(new BigDecimal("14")) >= 0) {
            reasons.add("商品库排序靠前");
        }
        if (reputationScore.compareTo(new BigDecimal("16")) >= 0) {
            reasons.add("评分与评论量表现稳定");
        }
        if (priceReasonability.compareTo(new BigDecimal("10")) >= 0) {
            reasons.add("价格带具备进一步寻源空间");
        }
        if (reasons.isEmpty()) {
            reasons.add("已命中商品库候选，建议结合国内货源进一步核验");
        }
        return String.join("，", reasons) + "。";
    }

    private BigDecimal ratioScore(List<String> sourceTokens, List<String> targetTokens, int maxScore) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long overlap = sourceTokens.stream().distinct().filter(targetTokens::contains).count();
        BigDecimal ratio = BigDecimal.valueOf(overlap)
                .divide(BigDecimal.valueOf(Math.max(1, sourceTokens.stream().distinct().count())), 4, RoundingMode.HALF_UP);
        return ratio.multiply(BigDecimal.valueOf(maxScore)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}a-z0-9]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    private int resolveCandidateLimit(AnalysisTask analysisTask) {
        if (analysisTask.getRequestedLimit() != null && analysisTask.getRequestedLimit() > 0) {
            return analysisTask.getRequestedLimit();
        }
        return taskExecutionProperties.getPhase1CandidateLimit();
    }

    private boolean shouldAttemptLiveSearch(AnalysisTask analysisTask) {
        return analysisTask.getMode() != TaskMode.MOCK;
    }

    private boolean shouldAttemptHistoryFallback(AnalysisTask analysisTask) {
        return analysisTask.getMode() != TaskMode.MOCK;
    }

    private Phase1WorkflowFailedException buildFailure(
            AnalysisTask analysisTask,
            List<TaskLogEntry> logs,
            boolean attemptedLiveSearch,
            String liveSearchFailureReason
    ) {
        if (attemptedLiveSearch && liveSearchFailureReason != null) {
            return new Phase1WorkflowFailedException(
                    "Amazon 实时检索失败（" + liveSearchFailureReason + "），且本地商品库/历史快照未命中关键词“"
                            + analysisTask.getKeyword() + "”。请先修复 crawler / SerpApi 配置后重试。",
                    logs
            );
        }
        return new Phase1WorkflowFailedException(
                "未命中可用的 Amazon 候选，请调整关键词后重试。",
                logs
        );
    }

    private String summarizeLiveSearchFailure(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "下游服务未返回可识别的错误信息";
        }
        java.util.regex.Matcher detailMatcher = java.util.regex.Pattern
                .compile("\"detail\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(message);
        if (detailMatcher.find()) {
            return detailMatcher.group(1).trim();
        }
        String normalized = message
                .replaceAll("^\\d{3}\\s+[A-Za-z ]+:\\s*", "")
                .replace("\\\"", "\"")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() > 160) {
            return normalized.substring(0, 160) + "...";
        }
        return normalized;
    }

    private TaskLogEntry log(String stage, String message) {
        return log(stage, TaskLogLevel.INFO, message);
    }

    private TaskLogEntry warnLog(String stage, String message) {
        return log(stage, TaskLogLevel.WARN, message);
    }

    private TaskLogEntry log(String stage, TaskLogLevel level, String message) {
        return new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                level,
                message,
                "phase1-discovery-workflow"
        );
    }

    private record ScoredCandidate(
            Product product,
            BigDecimal querySimilarity,
            BigDecimal originalRankScore,
            BigDecimal reputationScore,
            BigDecimal priceReasonability,
            BigDecimal totalScore,
            BigDecimal estimatedMargin,
            String riskTag,
            String recommendationReason
    ) {
    }
}
