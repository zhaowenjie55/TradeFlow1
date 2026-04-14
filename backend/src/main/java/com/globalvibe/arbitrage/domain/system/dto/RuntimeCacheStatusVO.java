package com.globalvibe.arbitrage.domain.system.dto;

public record RuntimeCacheStatusVO(
        boolean configured,
        boolean reachable,
        String status,
        String message
) {
}
