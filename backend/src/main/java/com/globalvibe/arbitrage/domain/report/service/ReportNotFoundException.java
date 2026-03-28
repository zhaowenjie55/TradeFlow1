package com.globalvibe.arbitrage.domain.report.service;

public class ReportNotFoundException extends RuntimeException {

    public ReportNotFoundException(String reportId) {
        super("报告不存在: " + reportId);
    }
}
