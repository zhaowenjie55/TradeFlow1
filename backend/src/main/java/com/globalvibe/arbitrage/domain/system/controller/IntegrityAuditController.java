package com.globalvibe.arbitrage.domain.system.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.system.dto.IntegrityAuditResponse;
import com.globalvibe.arbitrage.domain.system.service.RelationalIntegrityAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/integrity")
public class IntegrityAuditController {

    private final RelationalIntegrityAuditService relationalIntegrityAuditService;

    public IntegrityAuditController(RelationalIntegrityAuditService relationalIntegrityAuditService) {
        this.relationalIntegrityAuditService = relationalIntegrityAuditService;
    }

    @GetMapping("/relational")
    public ApiResponse<IntegrityAuditResponse> auditRelationalIntegrity() {
        return ApiResponse.success(relationalIntegrityAuditService.audit());
    }
}
