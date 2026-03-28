package com.globalvibe.arbitrage.domain.task.dto;

import com.globalvibe.arbitrage.domain.task.model.TaskMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record Phase1CreateTaskRequest(
        @NotBlank(message = "keyword 不能为空") String keyword,
        @NotBlank(message = "market 不能为空") String market,
        @Valid List<TaskConstraintRequest> constraints,
        @Min(value = 1, message = "limit 必须大于 0")
        Integer limit,
        @NotNull(message = "targetProfitMargin 不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "targetProfitMargin 必须大于 0")
        BigDecimal targetProfitMargin,
        TaskMode mode
) {
}
