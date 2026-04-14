package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class TaskStatusTransitionPolicy {

    private final Map<TaskStatus, Set<TaskStatus>> allowedTransitions = new EnumMap<>(TaskStatus.class);

    public TaskStatusTransitionPolicy() {
        allow(TaskStatus.CREATED, TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.FAILED);
        allow(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.FAILED);
        allow(TaskStatus.RUNNING, TaskStatus.ANALYZING_SOURCE, TaskStatus.WAITING_USER_SELECTION, TaskStatus.FAILED);
        allow(TaskStatus.ANALYZING_SOURCE, TaskStatus.WAITING_1688_VERIFICATION, TaskStatus.REPORT_READY, TaskStatus.FAILED);
        allow(TaskStatus.WAITING_1688_VERIFICATION, TaskStatus.QUEUED, TaskStatus.FAILED);
        allow(TaskStatus.WAITING_USER_SELECTION, TaskStatus.CREATED, TaskStatus.QUEUED, TaskStatus.FAILED);
        allow(TaskStatus.REPORT_READY);
        allow(TaskStatus.FAILED, TaskStatus.QUEUED);
        allow(TaskStatus.FALLBACK_MOCK);
    }

    public boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        return allowedTransitions.getOrDefault(from, Set.of()).contains(to);
    }

    public void assertAllowed(TaskStatus from, TaskStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal task status transition: " + from + " -> " + to);
        }
    }

    private void allow(TaskStatus from, TaskStatus... to) {
        if (to.length == 0) {
            allowedTransitions.put(from, Set.of());
            return;
        }
        EnumSet<TaskStatus> transitions = EnumSet.noneOf(TaskStatus.class);
        for (TaskStatus status : to) {
            transitions.add(status);
        }
        allowedTransitions.put(from, transitions);
    }
}
