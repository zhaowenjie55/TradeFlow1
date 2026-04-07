package com.globalvibe.arbitrage.domain.search.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for Amazon keyword search.
 */
public record SearchRequest(
        @NotBlank(message = "keyword must not be blank")
        String keyword,
        @Min(value = 1, message = "page must be greater than or equal to 1")
        int page
) {
}
