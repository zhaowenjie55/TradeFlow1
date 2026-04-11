package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.config.PricingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingEngineTest {

    @Test
    void calculatesPricingBreakdownFromBenchmarkAndShipping() {
        PricingProperties pricingProperties = new PricingProperties();
        PricingEngine pricingEngine = new PricingEngine(pricingProperties);

        PricingEngine.PricingBreakdown breakdown = pricingEngine.calculate(
                new BigDecimal("18.80"),
                new BigDecimal("23.00"),
                new BigDecimal("3.00")
        );

        assertEquals(new BigDecimal("18.80"), breakdown.amazonPriceUsd());
        assertEquals(new BigDecimal("7.20"), breakdown.usdToCnyRate());
        assertEquals(new BigDecimal("135.36"), breakdown.amazonPriceRmb());
        assertEquals(new BigDecimal("23.00"), breakdown.sourcingCost());
        assertEquals(new BigDecimal("3.00"), breakdown.domesticShippingCost());
        assertEquals(new BigDecimal("16.24"), breakdown.logisticsCost());
        assertEquals(new BigDecimal("20.30"), breakdown.platformFee());
        assertEquals(new BigDecimal("4.06"), breakdown.exchangeRateCost());
        assertEquals(new BigDecimal("66.60"), breakdown.totalCost());
        assertEquals(new BigDecimal("68.76"), breakdown.estimatedProfit());
        assertEquals(new BigDecimal("50.80"), breakdown.expectedMargin());
    }

    @Test
    void fallsBackToConfiguredSourcingRateWhenBenchmarkMissing() {
        PricingProperties pricingProperties = new PricingProperties();
        PricingEngine pricingEngine = new PricingEngine(pricingProperties);

        PricingEngine.PricingBreakdown breakdown = pricingEngine.calculate(
                new BigDecimal("10.00"),
                null,
                new BigDecimal("6.00")
        );

        assertEquals(new BigDecimal("72.00"), breakdown.amazonPriceRmb());
        assertEquals(new BigDecimal("32.40"), breakdown.sourcingCost());
        assertEquals(new BigDecimal("6.00"), breakdown.domesticShippingCost());
        assertEquals(new BigDecimal("8.64"), breakdown.logisticsCost());
        assertEquals(new BigDecimal("10.80"), breakdown.platformFee());
        assertEquals(new BigDecimal("2.16"), breakdown.exchangeRateCost());
        assertEquals(new BigDecimal("60.00"), breakdown.totalCost());
        assertEquals(new BigDecimal("12.00"), breakdown.estimatedProfit());
        assertEquals(new BigDecimal("16.67"), breakdown.expectedMargin());
    }
}
