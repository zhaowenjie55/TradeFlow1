package com.globalvibe.arbitrage.ai.workflow;

import com.globalvibe.arbitrage.domain.task.model.TaskLogEntry;

import java.util.List;

public class Phase1WorkflowFailedException extends RuntimeException {

    private final List<TaskLogEntry> logs;

    public Phase1WorkflowFailedException(String message, List<TaskLogEntry> logs) {
        super(message);
        this.logs = List.copyOf(logs);
    }

    public List<TaskLogEntry> logs() {
        return logs;
    }
}
