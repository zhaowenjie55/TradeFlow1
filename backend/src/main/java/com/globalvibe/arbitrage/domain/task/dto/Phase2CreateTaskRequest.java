package com.globalvibe.arbitrage.domain.task.dto;

import jakarta.validation.constraints.NotBlank;

public record Phase2CreateTaskRequest(
        @NotBlank(message = "phase1TaskId 不能为空") String phase1TaskId,
        @NotBlank(message = "productId 不能为空") String productId
) {
}
