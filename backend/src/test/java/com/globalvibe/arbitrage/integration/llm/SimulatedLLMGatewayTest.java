package com.globalvibe.arbitrage.integration.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedLLMGatewayTest {

    @Test
    void shouldRewriteCoffeeIntoDomesticKeywords() {
        SimulatedLLMGateway gateway = new SimulatedLLMGateway();

        LLMGateway.RewriteResult result = gateway.rewriteTitle(
                "Kauai Hawaiian Ground Coffee, Vanilla Macadamia Nut Flavor",
                "fallback"
        );

        assertEquals("夏威夷咖啡粉", result.rewrittenText());
        assertTrue(result.keywords().contains("咖啡粉"));
        assertTrue(result.keywords().contains("夏威夷咖啡"));
        assertTrue(result.keywords().contains("香草咖啡"));
        assertTrue(result.keywords().contains("夏威夷果咖啡"));
        assertFalse(result.keywords().contains("kauai"));
    }
}
