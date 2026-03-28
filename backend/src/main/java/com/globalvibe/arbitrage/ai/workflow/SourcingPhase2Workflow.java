package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.analysis.service.ProductAnalysisService;
import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.match.service.DomesticMatchService;
import com.globalvibe.arbitrage.domain.product.model.ProductDetailSnapshot;
import com.globalvibe.arbitrage.domain.product.repository.ProductRepository;
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

    private final ProductAnalysisService productAnalysisService;
    private final QueryRewriteService queryRewriteService;
    private final DomesticMatchService domesticMatchService;
    private final ProductRepository productRepository;

    public SourcingPhase2Workflow(
            ProductAnalysisService productAnalysisService,
            QueryRewriteService queryRewriteService,
            DomesticMatchService domesticMatchService,
            ProductRepository productRepository
    ) {
        this.productAnalysisService = productAnalysisService;
        this.queryRewriteService = queryRewriteService;
        this.domesticMatchService = domesticMatchService;
        this.productRepository = productRepository;
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
        ProductDetailSnapshot detail1688 = matches.isEmpty()
                ? null
                : productRepository.findDetailByProductId(matches.get(0).externalItemId()).orElse(null);

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
                        log("phase2.domestic-search", "已完成商品库混合检索与语义匹配。"),
                        log("phase2.product-detail", detail1688 != null
                                ? "已加载商品详情快照，补入品牌与属性信息。"
                                : "未命中商品详情快照，当前报告基于商品库标题与价格数据生成。"),
                        log("phase2.pricing", "已完成成本公式测算与利润估算。"),
                        log("phase2.report", "结构化报告已生成。")
                ),
                queryRewrite,
                matches,
                false
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
}
