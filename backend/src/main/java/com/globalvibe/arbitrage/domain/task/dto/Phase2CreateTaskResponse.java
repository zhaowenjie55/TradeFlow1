package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;

import java.time.OffsetDateTime;

public record Phase2CreateTaskResponse(
        String taskId,
        TaskPhase phase,
        TaskStatus status,
        TaskMode mode,
        String phase1TaskId,
        String productId,
        OffsetDateTime createdAt
) {
}
