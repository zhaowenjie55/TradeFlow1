package com.globalvibe.arbitrage.domain.analysis.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRunRequest(
        @NotBlank(message = "externalItemId must not be blank")
        String externalItemId
) {
}
