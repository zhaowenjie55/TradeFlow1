package com.globalvibe.arbitrage.domain.system.dto;

import java.time.OffsetDateTime;

public record RuntimeIntegrationStatusResponse(
        OffsetDateTime generatedAt,
        RuntimeCacheStatusVO redisCache,
        RuntimeDomesticSessionStatusVO domesticSession
) {
}
