# Analysis Agent (Stage 4)

## Purpose
This module upgrades the deterministic analysis pipeline with an LLM reasoning layer.

Design principle:
- Deterministic Java logic remains the source of truth for matching/profit/risk.
- LLM is used for explainability and narrative refinement.

## Request/Response
Endpoint:

```http
POST /api/analysis/run
```

Request:

```json
{
  "externalItemId": "B0XXXXXXX"
}
```

Response shape (simplified):

```json
{
  "success": true,
  "data": {
    "taskId": "task-...",
    "status": "DONE",
    "report": {
      "taskId": "task-...",
      "summary": "Estimated margin 31.20% with MEDIUM risk.",
      "profitEstimate": 7.80,
      "riskLevel": "MEDIUM",
      "confidenceScore": 0.74,
      "reportMarkdown": "# Product Analysis Report ...",
      "riskExplanations": ["..."]
    },
    "trace": {
      "steps": [
        {
          "stepName": "matching",
          "inputSummary": "...",
          "decision": "...",
          "explanation": "..."
        }
      ]
    }
  }
}
```

## Pipeline
Execution in `AnalysisService`:

1. Fetch overseas detail (via `DetailService`).
2. Run deterministic domestic matching.
3. Run deterministic profit computation.
4. Run deterministic risk evaluation.
5. Inject LLM reasoning step after each major stage.
6. Generate markdown report and append final LLM narrative.
7. Return report plus trace for UI rendering.

## Reasoning Contract
`LLMGateway` now supports:

- `generateReasoning(ReasoningRequest request)`

`ReasoningRequest`:
- `stepName`
- `prompt`
- `context` (map)

`ReasoningResult`:
- `decision`
- `explanation`
- `confidenceScore` (0..1)
- fallback/provider/model metadata

HTTP gateway requires strict JSON output:

```json
{
  "decision": "short decision for this step",
  "explanation": "clear explanation based on provided facts",
  "confidence_score": 0.0
}
```

## Current Step Names
- `matching`
- `profit_analysis`
- `risk_analysis`
- `final_report`

These names are stable keys intended for frontend trace panels.

## Fallback Behavior
`RoutingLLMGateway` behavior:

1. If LLM disabled, use simulated reasoning.
2. If forced simulated mode, use simulated reasoning.
3. If HTTP call fails or returns invalid output, fallback to simulated reasoning.

This keeps `/api/analysis/run` stable even when external LLM is unavailable.

## Configuration
From `application.yml`:

- `LLM_GATEWAY_ENABLED` (default `true`)
- `LLM_FORCE_SIMULATED` (default `false`)
- `LLM_CHAT_ENDPOINT`
- `LLM_MODEL`
- `LLM_API_KEY`
- `LLM_TEMPERATURE`

Example local setup:

```bash
export LLM_API_KEY="your_api_key"
export LLM_GATEWAY_ENABLED=true
export LLM_FORCE_SIMULATED=false
```

## Test
Run:

```bash
curl -X POST "http://127.0.0.1:8080/api/analysis/run" \
  -H "Content-Type: application/json" \
  -d '{"externalItemId":"B0XXXXXXX"}'
```

Verify:
- `data.report` exists
- `data.trace.steps` exists
- Step order is deterministic (`matching` -> `profit_analysis` -> `risk_analysis` -> `final_report`)

## Frontend Trace Rendering Notes
Minimal rendering schema:

- Panel title: `stepName`
- Subtitle: `inputSummary`
- Body:
  - decision
  - explanation

Suggested UI policy:
- Keep raw JSON hidden by default.
- Display step panels in backend-provided order.
- Show fallback badge later using provider/fallback metadata if needed.

Schema and example files:
- `backend/docs/schemas/analysis-run-response.schema.json`
- `backend/docs/examples/analysis-run-response.example.json`

## Extension Points (Not Implemented Here)
- Streaming reasoning tokens
- Tool-calling inside reasoning steps
- Multi-agent decomposition
- Persisted trace history
- Prompt versioning and A/B comparison
