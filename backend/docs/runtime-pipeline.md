# TradeFlow Runtime Pipeline

This backend has one supported product analysis path. The legacy direct analysis route
`POST /api/analysis/run` has been removed. Production analysis is task-based only.

## Official flow

1. `POST /api/analysis/tasks`
   - handled by `Phase1TaskApplicationService`
   - creates a phase-1 `AnalysisTask`

2. `Phase1TaskProcessor`
   - loads the task from `AnalysisTaskRepository`
   - executes `DiscoveryPhase1Workflow`
   - persists:
     - search runs/results
     - rewritten queries
     - phase-1 candidates
     - task logs/status

3. `POST /api/analysis/tasks/{taskId}/selection`
   - handled by `Phase2TaskApplicationService`
   - creates a phase-2 task for the selected candidate

4. `Phase2TaskProcessor`
   - executes `SourcingPhase2Workflow`
   - hydrates domestic details
   - runs `DomesticMatchService`
   - calls `ProductAnalysisService.buildReport(...)`

5. `ProductAnalysisService`
   - computes pricing and margin trace
   - assembles analysis trace
   - produces `ArbitrageReport`

6. `ReportAggregateService` / `PostgresReportAggregateRepository`
   - persists the report to `gv_analysis_report`
   - exposes report lookup by task id / report id

7. `ReportViewAssembler`
   - converts persisted `ArbitrageReport` into the frontend-facing DTO
   - attaches downloadable export artifacts such as the generated PDF

## Fallbacks

Fallback behavior is part of the task workflow, not a separate product path. The main
stages that may use fallback sources are:

- overseas discovery
- domestic match retrieval
- domestic detail hydration
- LLM narrative generation

Fallback usage should be visible through task logs and report provenance fields.

## Related classes

- `DiscoveryPhase1Workflow`
- `SourcingPhase2Workflow`
- `DomesticMatchService`
- `ProductAnalysisService`
- `ReportAggregateService`
- `ReportViewAssembler`
