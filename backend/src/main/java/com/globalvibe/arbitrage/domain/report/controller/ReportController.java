package com.globalvibe.arbitrage.domain.report.controller;

import com.globalvibe.arbitrage.common.api.ApiResponse;
import com.globalvibe.arbitrage.domain.report.dto.ReportDetailVO;
import com.globalvibe.arbitrage.domain.report.dto.ReportListResponse;
import com.globalvibe.arbitrage.domain.report.service.ReportQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportQueryService reportQueryService;

    public ReportController(ReportQueryService reportQueryService) {
        this.reportQueryService = reportQueryService;
    }

    @GetMapping("/list")
    public ApiResponse<ReportListResponse> list() {
        return ApiResponse.success(reportQueryService.listReports());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ReportDetailVO> detailByTaskId(@PathVariable String taskId) {
        return ApiResponse.success(reportQueryService.getReportByTaskId(taskId));
    }

    @GetMapping("/by-report/{reportId}")
    public ApiResponse<ReportDetailVO> detailByReportId(@PathVariable String reportId) {
        return ApiResponse.success(reportQueryService.getReportByReportId(reportId));
    }
}
