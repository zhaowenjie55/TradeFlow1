package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public record CreateAnalysisTaskRequest(
        @NotBlank(message = "keyword 不能为空") String keyword,
        @Min(value = 1, message = "limit 必须大于 0") Integer limit,
        BigDecimal targetProfitMargin,
        List<TaskConstraintRequest> constraints,
        TaskMode mode
) {
}
