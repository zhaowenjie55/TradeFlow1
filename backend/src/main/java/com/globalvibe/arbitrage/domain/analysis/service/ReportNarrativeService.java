package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.domain.candidate.model.CandidateProduct;
import com.globalvibe.arbitrage.domain.match.model.CandidateMatchRecord;
import com.globalvibe.arbitrage.domain.search.model.QueryRewrite;
import com.globalvibe.arbitrage.integration.llm.LLMGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportNarrativeService {

    private final LLMGateway llmGateway;

    public ReportNarrativeService(LLMGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    public LLMGateway.ReportNarrativeResult generate(
            CandidateProduct candidate,
            QueryRewrite queryRewrite,
            PricingEngine.PricingBreakdown pricing,
            String decision,
            String riskLevel,
            CandidateMatchRecord benchmark,
            List<CandidateMatchRecord> topDomesticMatches
    ) {
        String benchmarkTitle = benchmark != null ? benchmark.title() : "暂无稳定 1688 对标货源";
        return llmGateway.generateReportNarrative(
                new LLMGateway.ReportNarrativeRequest(
                        candidate.title(),
                        candidate.market(),
                        queryRewrite.rewrittenText(),
                        queryRewrite.keywords(),
                        decision,
                        riskLevel,
                        pricing.amazonPriceUsd(),
                        pricing.amazonPriceRmb(),
                        pricing.sourcingCost(),
                        pricing.domesticShippingCost(),
                        pricing.logisticsCost(),
                        pricing.platformFee(),
                        pricing.exchangeRateCost(),
                        pricing.totalCost(),
                        pricing.estimatedProfit(),
                        pricing.expectedMargin(),
                        benchmarkTitle,
                        topDomesticMatches == null ? List.of() : topDomesticMatches.stream().map(CandidateMatchRecord::title).toList()
                )
        );
    }
}
