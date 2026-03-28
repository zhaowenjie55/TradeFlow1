package com.globalvibe.arbitrage.domain.task.model;

import java.time.OffsetDateTime;

public record TaskLogEntry(
        OffsetDateTime timestamp,
        String stage,
        TaskLogLevel level,
        String message,
        String source
) {
}
