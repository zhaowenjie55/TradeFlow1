package com.globalvibe.arbitrage.domain.system.dto;

public record RuntimeDomesticSessionStatusVO(
        boolean configured,
        boolean reachable,
        boolean active,
        String mode,
        boolean verificationRequired,
        String lastVerificationUrl,
        int cooldownRemainingSeconds,
        String currentUrl,
        int idleTtlSeconds,
        String message
) {
}
