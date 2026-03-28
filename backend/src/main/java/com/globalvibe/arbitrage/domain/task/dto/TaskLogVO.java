package com.globalvibe.arbitrage.domain.task.dto;

import java.time.OffsetDateTime;

public record TaskLogVO(
        OffsetDateTime timestamp,
        String stage,
        String level,
        String message,
        String source
) {
}
s