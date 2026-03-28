package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record Phase2TaskStatusResponse(
        String taskId,
        TaskPhase phase,
        TaskStatus status,
        String stage,
        int progress,
        boolean fallbackTriggered,
        String phase1TaskId,
        String productId,
        String reportId,
        TaskMode mode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TaskLogVO> logs,
        List<PipelineStepVO> pipelineSteps,
        ReportDetailVO report
) {
}
