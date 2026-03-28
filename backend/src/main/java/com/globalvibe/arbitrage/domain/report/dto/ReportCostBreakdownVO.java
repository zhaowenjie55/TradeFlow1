package com.globalvibe.arbitrage.domain.report.dto;

import java.math.BigDecimal;

public record ReportCostBreakdownVO(
        BigDecimal sourcingCost,
        BigDecimal domesticShippingCost,
        BigDecimal logisticsCost,
        BigDecimal platformFee,
        BigDecimal exchangeRateCost,
        BigDecimal totalCost,
        BigDecimal targetSellingPrice,
        BigDecimal estimatedProfit
) {
}
