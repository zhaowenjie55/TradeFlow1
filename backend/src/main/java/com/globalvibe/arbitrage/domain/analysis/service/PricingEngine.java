package com.globalvibe.arbitrage.domain.analysis.service;

import com.globalvibe.arbitrage.config.PricingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PricingEngine {

    private final PricingProperties pricingProperties;

    public PricingEngine(PricingProperties pricingProperties) {
        this.pricingProperties = pricingProperties;
    }

    public PricingBreakdown calculate(
            BigDecimal amazonPriceUsd,
            BigDecimal benchmarkSourcingCost,
            BigDecimal domesticShippingCost
    ) {
        BigDecimal usdPrice = scale(zeroIfNull(amazonPriceUsd));
        BigDecimal usdToCnyRate = pricingProperties.getUsdToCnyRate();
        BigDecimal amazonPriceRmb = scale(usdPrice.multiply(usdToCnyRate));

        BigDecimal sourcingCost = benchmarkSourcingCost != null
                ? scale(benchmarkSourcingCost)
                : scale(amazonPriceRmb.multiply(pricingProperties.getFallbackSourcingRate()));
        BigDecimal normalizedDomesticShippingCost = scale(zeroIfNull(domesticShippingCost));
        BigDecimal logisticsCost = scale(amazonPriceRmb.multiply(pricingProperties.getCrossBorderLogisticsRate()));
        BigDecimal platformFee = scale(amazonPriceRmb.multiply(pricingProperties.getPlatformFeeRate()));
        BigDecimal exchangeRateCost = scale(amazonPriceRmb.multiply(pricingProperties.getExchangeLossRate()));
        BigDecimal totalCost = scale(sourcingCost
                .add(normalizedDomesticShippingCost)
                .add(logisticsCost)
                .add(platformFee)
                .add(exchangeRateCost));
        BigDecimal estimatedProfit = scale(amazonPriceRmb.subtract(totalCost));
        BigDecimal expectedMargin = amazonPriceRmb.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : scale(estimatedProfit.divide(amazonPriceRmb, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));

        return new PricingBreakdown(
                usdPrice,
                scale(usdToCnyRate),
                amazonPriceRmb,
                sourcingCost,
                normalizedDomesticShippingCost,
                logisticsCost,
                platformFee,
                exchangeRateCost,
                totalCost,
                estimatedProfit,
                expectedMargin
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record PricingBreakdown(
            BigDecimal amazonPriceUsd,
            BigDecimal usdToCnyRate,
            BigDecimal amazonPriceRmb,
            BigDecimal sourcingCost,
            BigDecimal domesticShippingCost,
            BigDecimal logisticsCost,
            BigDecimal platformFee,
            BigDecimal exchangeRateCost,
            BigDecimal totalCost,
            BigDecimal estimatedProfit,
            BigDecimal expectedMargin
    ) {
    }
}
