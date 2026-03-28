package com.globalvibe.arbitrage.domain.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskConstraintRequest(
        @NotBlank(message = "constraint.field 不能为空") String field,
        @NotBlank(message = "constraint.operator 不能为空") String operator,
        @NotBlank(message = "constraint.value 不能为空") String value
) {
}
