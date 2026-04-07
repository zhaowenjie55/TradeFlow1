package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.analysis.service.ProductAnalysisService;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.service.DomesticMatchService;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
import com.globalvibe.arbitrage.domain.product.service.ProductCatalogSyncService;
import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.domain.search.service.QueryRewriteService;
import com.globalvibe.arbitrage.domain.task.model.AnalysisTask;
import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;
import com.globalvibe.arbitrage.domain.task.model.TaskLogLevel;
import com.globalvibe.arbitrage.domain.task.service.InvalidTaskContextException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class SourcingPhase2Workflow implements Phase2Workflow {

    private final ProductAnalysisService productAnalysisService;
    private final QueryRewriteService queryRewriteService;
    private final DomesticMatchService domesticMatchService;
    private final ProductRepository productRepository;
    private final ProductCatalogSyncService productCatalogSyncService;

    public SourcingPhase2Workflow(
            ProductAnalysisService productAnalysisService,
            QueryRewriteService queryRewriteService,
            DomesticMatchService domesticMatchService,
            ProductRepository productRepository,
            ProductCatalogSyncService productCatalogSyncService
    ) {
        this.productAnalysisService = productAnalysisService;
        this.queryRewriteService = queryRewriteService;
        this.domesticMatchService = domesticMatchService;
        this.productRepository = productRepository;
        this.productCatalogSyncService = productCatalogSyncService;
    }

    @Override
    public Phase2WorkflowResult run(AnalysisTask phase2Task, AnalysisTask phase1Task, String productId) {
        CandidateProduct candidate = phase1Task.getCandidates().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new InvalidTaskContextException("所选商品不属于当前任务，请重新开始分析。"));
        String candidateId = phase1Task.getTaskId() + ":" + candidate.productId();

        QueryRewriteService.RewriteExecutionResult rewriteExecutionResult = queryRewriteService.rewrite(
                phase2Task.getTaskId(),
                candidateId,
                candidate.productId(),
                candidate.title(),
                phase2Task.getMode()
        );
        QueryRewrite queryRewrite = rewriteExecutionResult.queryRewrite();

        DomesticMatchService.MatchExecutionResult matchExecutionResult = domesticMatchService.match(
                phase2Task.getTaskId(),
                candidateId,
                candidate,
                queryRewrite,
                5
        );
        List<CandidateMatchRecord> matches = matchExecutionResult.matches();
        DetailHydrationResult detailHydrationResult = hydrateDomesticDetails(matches);
        ProductDetailSnapshot detail1688 = detailHydrationResult.primaryDetail();

        ArbitrageReport report = productAnalysisService.buildReport(
                "report-" + phase2Task.getTaskId(),
                candidate,
                detail1688,
                queryRewrite,
                matches
        );

        return new Phase2WorkflowResult(
                report,
                List.of(
                        log("phase2.rewrite", "已完成真实 GLM 改写，并生成国内检索词。"),
                        log("phase2.domestic-search", matchExecutionResult.fallbackUsed()
                                ? "国内实时货源检索未命中，已切换到本地商品库兜底匹配。"
                                : "已完成 1688 实时货源检索，并结合本地商品库完成混合匹配。"),
                        log("phase2.product-detail", detailHydrationResult.loadedCount() > 0
                                ? "已加载 " + detailHydrationResult.loadedCount() + " 条国内商品详情快照，补入品牌、属性与 SKU 信息。"
                                : "未命中商品详情快照，当前报告基于商品库标题与价格数据生成。"),
                        log("phase2.pricing", "已完成成本公式测算与利润估算。"),
                        log("phase2.report", "结构化报告已生成。")
                ),
                queryRewrite,
                matches,
                matchExecutionResult.fallbackUsed()
        );
    }

    private TaskLogEntry log(String stage, String message) {
        return new TaskLogEntry(
                OffsetDateTime.now(),
                stage,
                TaskLogLevel.INFO,
                message,
                "phase2-sourcing-workflow"
        );
    }

    private DetailHydrationResult hydrateDomesticDetails(List<CandidateMatchRecord> matches) {
        if (matches.isEmpty()) {
            return new DetailHydrationResult(null, 0);
        }
        List<CandidateMatchRecord> topMatches = matches.stream().limit(3).toList();
        List<ProductDetailSnapshot> snapshots = new ArrayList<>();
        for (CandidateMatchRecord match : topMatches) {
            String externalItemId = match.externalItemId();
            if (externalItemId == null || externalItemId.isBlank()) {
                continue;
            }
            ProductDetailSnapshot existingDetail = productRepository.findDetailByProductId(externalItemId).orElse(null);
            if (existingDetail != null) {
                snapshots.add(existingDetail);
                continue;
            }
            try {
                productCatalogSyncService.sync1688Detail(externalItemId).ifPresent(snapshots::add);
            } catch (RuntimeException ignored) {
                // Fall back to search-only reporting when detail hydration fails.
            }
        }
        return new DetailHydrationResult(snapshots.isEmpty() ? null : snapshots.get(0), snapshots.size());
    }

    private record DetailHydrationResult(ProductDetailSnapshot primaryDetail, int loadedCount) {
    }
}
