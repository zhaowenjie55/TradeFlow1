package com.globalvibe.arbitrage.domain.system.dto;

public record IntegrityConstraintStatusVO(
        String constraintName,
        boolean validated
) {
}
