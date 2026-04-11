package com.globalvibe.arbitrage.domain.task.service;

import com.globalvibe.arbitrage.domain.task.model.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStatusTransitionPolicyTest {

    private final TaskStatusTransitionPolicy policy = new TaskStatusTransitionPolicy();

    @Test
    void allowsKnownWorkflowTransitions() {
        assertTrue(policy.canTransition(TaskStatus.CREATED, TaskStatus.RUNNING));
        assertTrue(policy.canTransition(TaskStatus.RUNNING, TaskStatus.ANALYZING_SOURCE));
        assertTrue(policy.canTransition(TaskStatus.RUNNING, TaskStatus.WAITING_USER_SELECTION));
        assertTrue(policy.canTransition(TaskStatus.ANALYZING_SOURCE, TaskStatus.REPORT_READY));
        assertTrue(policy.canTransition(TaskStatus.WAITING_1688_VERIFICATION, TaskStatus.QUEUED));
        assertDoesNotThrow(() -> policy.assertAllowed(TaskStatus.ANALYZING_SOURCE, TaskStatus.WAITING_1688_VERIFICATION));
    }

    @Test
    void rejectsIllegalBackwardTransitions() {
        assertFalse(policy.canTransition(TaskStatus.REPORT_READY, TaskStatus.RUNNING));
        assertFalse(policy.canTransition(TaskStatus.WAITING_USER_SELECTION, TaskStatus.REPORT_READY));
        assertFalse(policy.canTransition(TaskStatus.CREATED, TaskStatus.REPORT_READY));
        assertThrows(IllegalStateException.class,
                () -> policy.assertAllowed(TaskStatus.REPORT_READY, TaskStatus.RUNNING));
    }
}
