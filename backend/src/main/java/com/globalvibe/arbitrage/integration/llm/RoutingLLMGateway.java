package com.globalvibe.arbitrage.integration.llm;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingLLMGateway implements LLMGateway {

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final HttpLLMGateway httpLLMGateway;
    private final SimulatedLLMGateway simulatedLLMGateway;

    public RoutingLLMGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            HttpLLMGateway httpLLMGateway,
            SimulatedLLMGateway simulatedLLMGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.httpLLMGateway = httpLLMGateway;
        this.simulatedLLMGateway = simulatedLLMGateway;
    }

    @Override
    public RewriteResult rewriteTitle(String sourceTitle) {
        if (!integrationGatewayProperties.getLlm().isEnabled()) {
            return simulatedLLMGateway.rewriteTitle(
                    sourceTitle,
                    "LLM 改写接口未启用，已回退到内置改写策略。"
            );
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.rewriteTitle(
                    sourceTitle,
                    "LLM 改写接口已配置为强制模拟，已回退到内置改写策略。"
            );
        }
        try {
            return httpLLMGateway.rewriteTitle(sourceTitle);
        } catch (RuntimeException ex) {
            return simulatedLLMGateway.rewriteTitle(
                    sourceTitle,
                    "LLM 改写接口调用失败，已回退到内置改写策略: " + ex.getMessage()
            );
        }
    }

    @Override
    public ReportNarrativeResult generateReportNarrative(ReportNarrativeRequest request) {
        if (!integrationGatewayProperties.getLlm().isEnabled()) {
            return simulatedLLMGateway.generateReportNarrative(
                    request,
                    "LLM 报告接口未启用，已回退到内置 agent 叙事模板。"
            );
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.generateReportNarrative(
                    request,
                    "LLM 报告接口已配置为强制模拟，已回退到内置 agent 叙事模板。"
            );
        }
        try {
            return httpLLMGateway.generateReportNarrative(request);
        } catch (RuntimeException ex) {
            return simulatedLLMGateway.generateReportNarrative(
                request,
                    "LLM 报告接口调用失败，已回退到内置 agent 叙事模板: " + ex.getMessage()
            );
        }
    }
}
