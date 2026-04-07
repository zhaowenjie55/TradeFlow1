package com.globalvibe.arbitrage.domain.detail.dto;

import jakarta.validation.constraints.NotBlank;

public record DetailRequest(
        @NotBlank(message = "externalItemId must not be blank")
        String externalItemId
) {
}
