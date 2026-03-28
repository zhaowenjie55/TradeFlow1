package com.globalvibe.arbitrage.domain.task.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectCandidateRequest(
        @NotBlank(message = "productId 不能为空") String productId
) {
}
