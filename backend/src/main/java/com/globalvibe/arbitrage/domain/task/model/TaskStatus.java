package com.globalvibe.arbitrage.domain.task.model;

public enum TaskStatus {
    CREATED,
    QUEUED,
    RUNNING,
    WAITING_USER_SELECTION,
    ANALYZING_SOURCE,
    REPORT_READY,
    FAILED,
    FALLBACK_MOCK
}
