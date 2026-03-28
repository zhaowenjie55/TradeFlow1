package com.globalvibe.arbitrage.domain.analysis.service;

public class AnalysisResultNotFoundException extends RuntimeException {

    public AnalysisResultNotFoundException(String taskId) {
        super("未找到任务分析结果: " + taskId);
    }
}
