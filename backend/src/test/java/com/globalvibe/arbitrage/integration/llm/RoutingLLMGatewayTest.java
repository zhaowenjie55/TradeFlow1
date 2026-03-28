package com.globalvibe.arbitrage.integration.llm;

import com.globalvibe.arbitrage.config.IntegrationGatewayProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutingLLMGatewayTest {

    @Test
    void shouldUseSimulatedGatewayWhenForceSimulatedEnabled() {
        IntegrationGatewayProperties properties = new IntegrationGatewayProperties();
        properties.getLlm().setEnabled(true);
        properties.getLlm().setForceSimulated(true);

        HttpLLMGateway httpGateway = mock(HttpLLMGateway.class);
        SimulatedLLMGateway simulatedGateway = mock(SimulatedLLMGateway.class);
        when(simulatedGateway.rewriteTitle(any(), any())).thenReturn(new LLMGateway.RewriteResult(
                "亚克力透明收纳架",
                List.of("亚克力透明收纳架"),
                true,
                "SIMULATED_LLM",
                null
        ));

        RoutingLLMGateway gateway = new RoutingLLMGateway(properties, httpGateway, simulatedGateway);
        LLMGateway.RewriteResult result = gateway.rewriteTitle("Acrylic Desktop Organizer");

        assertEquals("SIMULATED_LLM", result.provider());
        verify(httpGateway, never()).rewriteTitle(any());
    }

    @Test
    void shouldFallbackToSimulatedGatewayWhenHttpGatewayFails() {
        IntegrationGatewayProperties properties = new IntegrationGatewayProperties();
        properties.getLlm().setEnabled(true);
        properties.getLlm().setForceSimulated(false);

        HttpLLMGateway httpGateway = mock(HttpLLMGateway.class);
        SimulatedLLMGateway simulatedGateway = mock(SimulatedLLMGateway.class);
        when(httpGateway.rewriteTitle(any())).thenThrow(new IllegalStateException("bad response"));
        when(simulatedGateway.rewriteTitle(any(), any())).thenReturn(new LLMGateway.RewriteResult(
                "亚克力透明收纳架",
                List.of("亚克力透明收纳架"),
                true,
                "SIMULATED_LLM",
                "bad response"
        ));

        RoutingLLMGateway gateway = new RoutingLLMGateway(properties, httpGateway, simulatedGateway);
        LLMGateway.RewriteResult result = gateway.rewriteTitle("Acrylic Desktop Organizer");

        assertEquals("SIMULATED_LLM", result.provider());
        verify(simulatedGateway).rewriteTitle(any(), any());
    }
}
