package com.globalvibe.arbitrage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    private BigDecimal usdToCnyRate = new BigDecimal("7.20");
    private BigDecimal crossBorderLogisticsRate = new BigDecimal("0.12");
    private BigDecimal platformFeeRate = new BigDecimal("0.15");
    private BigDecimal exchangeLossRate = new BigDecimal("0.03");
    private BigDecimal fallbackSourcingRate = new BigDecimal("0.45");
}
