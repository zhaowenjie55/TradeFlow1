package com.globalvibe.arbitrage.integration.llm;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingLLMGatewayTest {

    @Test
    void shouldFailWhenForceSimulatedEnabled() {
        IntegrationGatewayProperties properties = new IntegrationGatewayProperties();
        properties.getLlm().setEnabled(true);
        properties.getLlm().setForceSimulated(true);

        HttpLLMGateway httpGateway = mock(HttpLLMGateway.class);

        RoutingLLMGateway gateway = new RoutingLLMGateway(properties, httpGateway);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> gateway.rewriteTitle("Acrylic Desktop Organizer"));

        assertEquals("LLM 改写接口被配置为模拟模式，当前链路要求真实 GLM。", error.getMessage());
        verify(httpGateway, never()).rewriteTitle("Acrylic Desktop Organizer");
    }

    @Test
    void shouldUseHttpGatewayWhenConfigured() {
        IntegrationGatewayProperties properties = new IntegrationGatewayProperties();
        properties.getLlm().setEnabled(true);
        properties.getLlm().setForceSimulated(false);

        HttpLLMGateway httpGateway = mock(HttpLLMGateway.class);
        when(httpGateway.rewriteTitle("Acrylic Desktop Organizer")).thenReturn(new LLMGateway.RewriteResult(
                "亚克力透明收纳架",
                java.util.List.of("亚克力透明收纳架"),
                false,
                "GLM_CHAT",
                "glm-5",
                null,
                java.time.OffsetDateTime.now()
        ));

        RoutingLLMGateway gateway = new RoutingLLMGateway(properties, httpGateway);
        LLMGateway.RewriteResult result = gateway.rewriteTitle("Acrylic Desktop Organizer");

        assertEquals("GLM_CHAT", result.provider());
        assertEquals("glm-5", result.model());
    }
}
