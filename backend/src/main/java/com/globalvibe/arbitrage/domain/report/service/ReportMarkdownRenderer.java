package com.globalvibe.arbitrage.domain.report.service;

import com.globalvibe.arbitrage.domain.report.model.ArbitrageReport;

public interface ReportMarkdownRenderer {

    String render(ArbitrageReport report);
}
