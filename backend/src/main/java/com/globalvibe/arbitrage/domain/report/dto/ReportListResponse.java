package com.globalvibe.arbitrage.domain.report.dto;

import java.util.List;

public record ReportListResponse(
        List<ReportListItemResponse> items
) {
}
