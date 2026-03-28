package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;

import java.time.OffsetDateTime;

public record Phase1CreateTaskResponse(
        String taskId,
        TaskPhase phase,
        TaskStatus status,
        TaskMode mode,
        OffsetDateTime createdAt
) {
}
