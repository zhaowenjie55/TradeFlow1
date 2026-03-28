package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.analysis.service.ProductAnalysisService;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.service.DomesticMatchService;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.service.DomesticProductFallbackService;
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
import java.util.List;

@Component
public class SourcingPhase2Workflow implements Phase2Workflow {

    private final ProductCatalogSyncService productCatalogSyncService;
    private final DomesticProductFallbackService domesticProductFallbackService;
    private final ProductAnalysisService productAnalysisService;
    private final QueryRewriteService queryRewriteService;
    private final DomesticMatchService domesticMatchService;

    public SourcingPhase2Workflow(
            ProductCatalogSyncService productCatalogSyncService,
            DomesticProductFallbackService domesticProductFallbackService,
            ProductAnalysisService productAnalysisService,
            QueryRewriteService queryRewriteService,
            DomesticMatchService domesticMatchService
    ) {
        this.productCatalogSyncService = productCatalogSyncService;
        this.domesticProductFallbackService = domesticProductFallbackService;
        this.productAnalysisService = productAnalysisService;
        this.queryRewriteService = queryRewriteService;
        this.domesticMatchService = domesticMatchService;
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
                candidate.title()
        );
        QueryRewrite queryRewrite = rewriteExecutionResult.queryRewrite();
        DomesticMatchService.MatchExecutionResult matchExecutionResult = domesticMatchService.match(
                phase1Task.getTaskId(),
                candidateId,
                candidate.productId(),
                candidate.title(),
                queryRewrite,
                5
        );
        List<CandidateMatchRecord> matches = matchExecutionResult.matches();
        DetailLookupResult detailLookupResult = matches.isEmpty()
                ? new DetailLookupResult(null, false)
                : resolveDomesticDetail(matches.getFirst().externalItemId());
        ProductDetailSnapshot taobaoDetail = detailLookupResult.detailSnapshot();
        ArbitrageReport report = productAnalysisService.buildReport(
                "report-" + phase2Task.getTaskId(),
                candidate,
                taobaoDetail,
                queryRewrite.rewrittenText(),
                matches
        );

        return new Phase2WorkflowResult(
                report,
                List.of(
                        log("phase2.rewrite", rewriteExecutionResult.fallbackUsed()
                                ? "LLM 改写失败，已回退到历史改写结果。"
                                : "已完成 Amazon 标题到国内搜索词的改写。"),
                        log("phase2.domestic-search", matchExecutionResult.fallbackUsed()
                                ? "国内搜索未命中实时结果，已回退到数据库中的历史国内商品数据。"
                                : "已完成淘宝候选搜索与相似度匹配。"),
                        log("phase2.product-detail", detailLookupResult.fallbackUsed()
                                ? "淘宝详情读取失败，已回退到数据库中的历史详情快照。"
                                : taobaoDetail != null
                                ? "已加载淘宝详情数据，纳入品牌、属性与 SKU 信息。"
                                : "未命中淘宝详情数据，当前退回关键词搜索结果分析。"),
                        log("phase2.pricing", "已结合 Amazon 海外售价与国内采购价生成利润估算。"),
                        log("phase2.report", "二阶段分析已完成，报告已生成。")
                ),
                queryRewrite,
                matches,
                rewriteExecutionResult.fallbackUsed()
                        || matchExecutionResult.fallbackUsed()
                        || detailLookupResult.fallbackUsed()
        );
    }

    private DetailLookupResult resolveDomesticDetail(String productId) {
        try {
            ProductDetailSnapshot liveDetail = productCatalogSyncService.syncTaobaoDetail(productId).orElse(null);
            if (liveDetail != null) {
                return new DetailLookupResult(liveDetail, false);
            }
        } catch (RuntimeException ignored) {
            // Fall through to historical detail lookup.
        }
        return domesticProductFallbackService.findHistoricalDetail(productId)
                .map(detail -> new DetailLookupResult(detail, true))
                .orElseGet(() -> new DetailLookupResult(null, false));
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

    private record DetailLookupResult(
            ProductDetailSnapshot detailSnapshot,
            boolean fallbackUsed
    ) {
    }
}
