package com.globalvibe.arbitrage.domain.task.dto;

import java.util.List;

public record TaskHistoryResponse(
        List<TaskHistoryItemResponse> items
) {
}
