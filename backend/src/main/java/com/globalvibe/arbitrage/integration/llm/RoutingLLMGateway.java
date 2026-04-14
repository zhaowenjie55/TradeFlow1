package com.globalvibe.arbitrage.integration.llm;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingLLMGateway implements LLMGateway {

    private static final Logger log = LoggerFactory.getLogger(RoutingLLMGateway.class);

    private final IntegrationGatewayProperties integrationGatewayProperties;
    private final FastApiLLMGateway fastApiLLMGateway;
    private final SimulatedLLMGateway simulatedLLMGateway;

    public RoutingLLMGateway(
            IntegrationGatewayProperties integrationGatewayProperties,
            FastApiLLMGateway fastApiLLMGateway,
            SimulatedLLMGateway simulatedLLMGateway
    ) {
        this.integrationGatewayProperties = integrationGatewayProperties;
        this.fastApiLLMGateway = fastApiLLMGateway;
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
            return fastApiLLMGateway.rewriteTitle(sourceTitle);
        } catch (RuntimeException ex) {
            log.info("LLM rewriteTitle failed, falling back to simulated: {}", ex.getMessage());
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
            return fastApiLLMGateway.generateReportNarrative(request);
        } catch (RuntimeException ex) {
            log.info("LLM generateReportNarrative failed, falling back to simulated: {}", ex.getMessage());
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
            return fastApiLLMGateway.generateReasoning(request);
        } catch (RuntimeException ex) {
            log.info("LLM generateReasoning failed, falling back to simulated: {}", ex.getMessage());
            return simulatedLLMGateway.generateReasoning(request, ex.getMessage());
        }
    }

    @Override
    public TranscriptIntentResult analyzeTranscript(TranscriptIntentRequest request) {
        if (!integrationGatewayProperties.getLlm().isEnabled()) {
            return simulatedLLMGateway.analyzeTranscript(request, "LLM transcript 分析接口未启用，已切换到模拟模式。");
        }
        if (integrationGatewayProperties.getLlm().isForceSimulated()) {
            return simulatedLLMGateway.analyzeTranscript(request, "LLM transcript 分析接口被配置为模拟模式。");
        }
        try {
            return fastApiLLMGateway.analyzeTranscript(request);
        } catch (RuntimeException ex) {
            log.info("LLM analyzeTranscript failed, falling back to simulated: {}", ex.getMessage());
            return simulatedLLMGateway.analyzeTranscript(request, ex.getMessage());
        }
    }
}
