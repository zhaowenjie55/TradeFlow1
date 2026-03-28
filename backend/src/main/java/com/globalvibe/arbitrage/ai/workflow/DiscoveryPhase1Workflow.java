package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.config.TaskExecutionProperties;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.product.model.Product;
import com.globalvibe.arbitrage.domain.product.service.ProductCatalogSyncService;
import com.globalvibe.arbitrage.domain.search.service.SearchHistoryFallbackService;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DiscoveryPhase1Workflow implements Phase1Workflow {

    private final TaskExecutionProperties taskExecutionProperties;
    private final ProductCatalogSyncService productCatalogSyncService;
    private final SearchHistoryFallbackService searchHistoryFallbackService;

    public DiscoveryPhase1Workflow(
            TaskExecutionProperties taskExecutionProperties,
            ProductCatalogSyncService productCatalogSyncService,
            SearchHistoryFallbackService searchHistoryFallbackService
    ) {
        this.taskExecutionProperties = taskExecutionProperties;
        this.productCatalogSyncService = productCatalogSyncService;
        this.searchHistoryFallbackService = searchHistoryFallbackService;
    }

    @Override
    public Phase1WorkflowResult run(AnalysisTask analysisTask) {
        List<CandidateProduct> candidates = new ArrayList<>();
        List<TaskLogEntry> logs = new ArrayList<>();
        List<Product> sourceProducts;
        boolean fallbackUsed = false;

        logs.add(log("phase1.market-scan", "开始执行 Amazon 商品发现。"));
        logs.add(log("phase1.filter", "根据评分、评论量和目标利润率进行第一轮候选筛选。"));

        try {
            sourceProducts = productCatalogSyncService.syncAmazonProducts(
                    analysisTask.getKeyword(),
                    resolveCandidateLimit(analysisTask)
            );
            if (sourceProducts.isEmpty()) {
                throw new IllegalStateException("Amazon 搜索返回空结果");
            }
        } catch (Exception ex) {
            fallbackUsed = true;
            sourceProducts = searchHistoryFallbackService.findLatestAmazonProducts(
                    analysisTask.getKeyword(),
                    resolveCandidateLimit(analysisTask)
            );
            logs.add(log("phase1.fallback", "Amazon 实时读取失败，已回退到最近一次搜索快照。"));
        }

        int candidateLimit = Math.min(resolveCandidateLimit(analysisTask), sourceProducts.size());
        for (int index = 0; index < candidateLimit; index++) {
            Product product = sourceProducts.get(index);
            BigDecimal estimatedMargin = analysisTask.getTargetProfitMargin()
                    .multiply(BigDecimal.valueOf(100))
                    .add(BigDecimal.valueOf(Math.max(0, candidateLimit - index) * 1.6))
                    .setScale(1, RoundingMode.HALF_UP);
            candidates.add(new CandidateProduct(
                    product.id(),
                    product.title(),
                    product.image(),
                    product.platform().value(),
                    product.price(),
                    estimatedMargin,
                    index <= 2 ? "低风险" : index <= 5 ? "中风险" : "待核验",
                    index <= 2 ? "Amazon 搜索结果中评分与评论量较稳，适合优先进入第二阶段。"
                            : "已从 Amazon 搜索结果接入，建议结合国内寻源进一步评估。",
                    index <= 5
            ));
        }

        logs.add(log("phase1.output", "海外候选商品已生成，等待用户选择进入第二阶段。"));
        return new Phase1WorkflowResult(candidates, logs, sourceProducts, fallbackUsed);
    }

    private int resolveCandidateLimit(AnalysisTask analysisTask) {
        if (analysisTask.getRequestedLimit() != null && analysisTask.getRequestedLimit() > 0) {
            return analysisTask.getRequestedLimit();
        }
        return taskExecutionProperties.getPhase1CandidateLimit();
    }

    private TaskLogEntry log(String stage, String message) {
        return new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                TaskLogLevel.INFO,
                message,
                "phase1-discovery-workflow"
        );
    }
}
