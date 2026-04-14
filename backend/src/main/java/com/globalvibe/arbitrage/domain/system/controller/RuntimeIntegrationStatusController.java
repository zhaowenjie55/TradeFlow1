package com.globalvibe.arbitrage.domain.system.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.system.dto.RuntimeIntegrationStatusResponse;
import com.globalvibe.arbitrage.domain.system.service.RuntimeIntegrationStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/runtime")
public class RuntimeIntegrationStatusController {

    private final RuntimeIntegrationStatusService runtimeIntegrationStatusService;

    public RuntimeIntegrationStatusController(RuntimeIntegrationStatusService runtimeIntegrationStatusService) {
        this.runtimeIntegrationStatusService = runtimeIntegrationStatusService;
    }

    @GetMapping("/integrations")
    public ApiResponse<RuntimeIntegrationStatusResponse> integrations() {
        return ApiResponse.success(runtimeIntegrationStatusService.getStatus());
    }
}
