# Task Status Ownership

This document records the legal runtime transitions for the task-based analysis pipeline.
The enum already exists in `TaskStatus`; this file documents which services own the
transitions and what each status means operationally.

## States

- `CREATED`
  - Meaning: task row exists but processing has not started yet.
  - Entered by: task creation APIs / application services.
  - Leaves to: `QUEUED`, `RUNNING`, `FAILED`.

- `QUEUED`
  - Meaning: task is ready for processing and waiting for an executor.
  - Entered by: phase processors, resume flow from verification wait.
  - Leaves to: `RUNNING`, `FAILED`.

- `RUNNING`
  - Meaning: a workflow is actively processing the task.
  - Entered by: `Phase1TaskProcessor`, `Phase2TaskProcessor`.
  - Leaves to: `ANALYZING_SOURCE`, `WAITING_USER_SELECTION`, `FAILED`.

- `WAITING_USER_SELECTION`
  - Meaning: phase 1 has produced candidates and is waiting for the user to choose one.
  - Entered by: `Phase1TaskProcessor`.
  - Leaves to: `CREATED`, `QUEUED`, `FAILED`.

- `ANALYZING_SOURCE`
  - Meaning: phase 2 has begun and source hydration/matching/report work is in progress.
  - Entered by: `Phase2TaskProcessor`.
  - Leaves to: `WAITING_1688_VERIFICATION`, `REPORT_READY`, `FAILED`.

- `WAITING_1688_VERIFICATION`
  - Meaning: source verification is required before continuing.
  - Entered by: `Phase2TaskProcessor`.
  - Leaves to: `QUEUED`, `FAILED`.

- `REPORT_READY`
  - Meaning: report has been generated and persisted.
  - Entered by: `Phase2TaskProcessor`.
  - Leaves to: none in normal production flow.

- `FAILED`
  - Meaning: task terminated due to an unrecoverable error.
  - Entered by: phase processors / application services on terminal failure.
  - Leaves to: none in normal production flow.

- `FALLBACK_MOCK`
  - Meaning: legacy mock/fallback state retained for compatibility only.
  - Entered by: should not be reachable in production flow.
  - Leaves to: none.

## Enforcement

Runtime enforcement lives in:

- `TaskStatusTransitionPolicy`
- `Phase1TaskProcessor`
- `Phase2TaskProcessor`
- `Phase2TaskApplicationService`

Tests covering the legal/illegal transition set live in:

- `TaskStatusTransitionPolicyTest`

## Retry Rules

- `WAITING_1688_VERIFICATION -> QUEUED`
  - user-driven retry after verification is supplied
- `QUEUED -> RUNNING`
  - executor-driven retry when processing restarts
- `FAILED`
  - no automatic retry defined yet; retries should create an explicit new execution path
