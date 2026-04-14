package com.globalvibe.arbitrage.domain.system.dto;

public record IntegrityOrphanCheckVO(
        String checkName,
        long orphanCount
) {
}
