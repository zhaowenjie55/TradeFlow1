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
            return simulatedLLMGateway.rewriteTitle(sourceTitle, "LLM 改写接口未启用，已切换到模拟模式。");
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.rewriteTitle(sourceTitle, "LLM 改写接口被配置为模拟模式。");
        }
        try {
            return httpLLMGateway.rewriteTitle(sourceTitle);
        } catch (RuntimeException ex) {
            return simulatedLLMGateway.rewriteTitle(sourceTitle, ex.getMessage());
        }
    }

    @Override
    public ReportNarrativeResult generateReportNarrative(ReportNarrativeRequest request) {
        if (!integrationGatewayProperties.getLlm().isEnabled()) {
            return simulatedLLMGateway.generateReportNarrative(request, "LLM 报告接口未启用，已切换到模拟模式。");
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.generateReportNarrative(request, "LLM 报告接口被配置为模拟模式。");
        }
        try {
            return httpLLMGateway.generateReportNarrative(request);
        } catch (RuntimeException ex) {
            return simulatedLLMGateway.generateReportNarrative(request, ex.getMessage());
        }
    }

    @Override
    public ReasoningResult generateReasoning(ReasoningRequest request) {
        if (!integrationGatewayProperties.getLlm().isEnabled()) {
            return simulatedLLMGateway.generateReasoning(request, "LLM 推理接口未启用，已切换到模拟模式。");
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.generateReasoning(request, "LLM 推理接口被配置为模拟模式。");
        }
        try {
            return httpLLMGateway.generateReasoning(request);
        } catch (RuntimeException ex) {
            return simulatedLLMGateway.generateReasoning(request, ex.getMessage());
        }
    }
}
