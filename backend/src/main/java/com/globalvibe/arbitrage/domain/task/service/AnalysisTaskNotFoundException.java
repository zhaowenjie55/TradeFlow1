package com.globalvibe.arbitrage.domain.task.service;

public class AnalysisTaskNotFoundException extends RuntimeException {

    public AnalysisTaskNotFoundException(String taskId) {
        super("任务不存在: " + taskId);
    }
}
