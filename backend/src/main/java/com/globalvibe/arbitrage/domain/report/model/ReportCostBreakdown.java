package com.globalvibe.arbitrage.domain.report.model;

import java.math.BigDecimal;

public record ReportCostBreakdown(
        BigDecimal sourcingCost,
        BigDecimal logisticsCost,
        BigDecimal platformFee,
        BigDecimal exchangeRateCost,
        BigDecimal totalCost,
        BigDecimal targetSellingPrice,
        BigDecimal estimatedProfit
) {
}
