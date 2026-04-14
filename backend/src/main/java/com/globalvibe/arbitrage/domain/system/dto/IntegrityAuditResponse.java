package com.globalvibe.arbitrage.domain.system.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record IntegrityAuditResponse(
        boolean allClear,
        boolean allConstraintsValidated,
        long totalOrphanCount,
        OffsetDateTime generatedAt,
        List<IntegrityOrphanCheckVO> orphanChecks,
        List<IntegrityConstraintStatusVO> constraintStatuses
) {
}
