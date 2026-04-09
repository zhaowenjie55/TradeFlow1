package com.globalvibe.arbitrage.domain.task.model;

public enum TaskStatus {
    CREATED,
    QUEUED,
    RUNNING,
    WAITING_USER_SELECTION,
    WAITING_1688_VERIFICATION,
    ANALYZING_SOURCE,
    REPORT_READY,
    FAILED,
    FALLBACK_MOCK
}
