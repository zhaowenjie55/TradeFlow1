package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.candidate.dto.CandidateVO;
import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import com.globalvibe.arbitrage.domain.task.model.TaskPhase;
import com.globalvibe.arbitrage.domain.task.model.TaskStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record Phase1TaskStatusResponse(
        String taskId,
        TaskPhase phase,
        TaskStatus status,
        String stage,
        int progress,
        boolean fallbackTriggered,
        String keyword,
        String market,
        BigDecimal targetProfitMargin,
        TaskMode mode,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<TaskLogVO> logs,
        List<PipelineStepVO> pipelineSteps,
        List<CandidateVO> candidates
) {
}
