package com.globalvibe.arbitrage.domain.task.model;

public record TaskConstraint(
        String field,
        String operator,
        String value
) {
}
