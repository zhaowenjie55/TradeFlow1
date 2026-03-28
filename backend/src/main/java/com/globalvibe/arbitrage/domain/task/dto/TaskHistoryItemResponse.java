package com.globalvibe.arbitrage.domain.task.dto;

import java.time.OffsetDateTime;

public record TaskHistoryItemResponse(
        String taskId,
        String keyword,
        String market,
        String status,
        String mode,
        OffsetDateTime createdAt
) {
}
