# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Cross-border arbitrage discovery platform. Identifies price gaps between domestic Chinese markets (1688) and overseas platforms (Amazon), uses AI to match products, and generates trade opportunity reports.

## Repository Structure

```
TradeFlow/
├── backend/              # Spring Boot 3 — core business logic & API
├── tradeflow-web-next/   # Next.js 16 — main product frontend (React/TSX) ← ACTIVE
├── globalvibe-web/       # Nuxt 3 — LEGACY, no new feature work here
├── crawler-service/      # Python FastAPI — scraping, domestic search, LLM proxy, ASR
├── models/               # Shared data model specs
└── docs/                 # Architecture & API docs
```

**Frontend direction:** `tradeflow-web-next/` is the only actively supported UI. `globalvibe-web/` is legacy — validate API changes against the React client, not the Nuxt app.

## Common Commands

### Root (from repo root)
```bash
npm run frontend:dev        # start Next.js dev server (port 3000)
npm run frontend:build      # production build
npm run frontend:lint       # lint
npm run frontend:legacy:dev # legacy Nuxt dev (port 3001) — reference only
```

### Backend
```bash
cd backend && mvn spring-boot:run               # run with local profile
cd backend && mvn clean package -DskipTests     # build jar
cd backend && mvn test                          # run all tests
cd backend && mvn test -Dtest=ClassName         # run a single test class
cd backend && mvn test -Dtest=ClassName#method  # run a single test method
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Crawler Service
```bash
cd crawler-service
pip install -r requirements.txt
uvicorn main:app --reload   # port 8001
```

## End-to-End Data Flow

```
React UI
  → POST /api/analysis/tasks (Phase 1)
  → Spring backend orchestrates Phase1TaskProcessor
      → DiscoveryPhase1Workflow: calls crawler /api/search (Amazon live scrape)
          → fallback: SearchHistoryFallbackService (DB-stored results)
      → stores CandidateProduct list, transitions task to PHASE2
  → Phase2TaskProcessor / SourcingPhase2Workflow
      → QueryRewriteService: calls crawler /api/llm/rewrite (title rewriting)
      → DomesticMatchService: calls crawler /api/domestic/search + /api/domestic/detail
      → PricingEngine: calculates profit margin using app.pricing.* rates
      → ReportAggregateService: persists final ReportAggregate to PostgreSQL
  → React UI polls task status, renders report
```

The crawler service (`port 8001`) acts as a unified proxy layer — it handles Amazon scraping, 1688 domestic search/detail, LLM calls (GLM-5), and ASR transcription. Spring never calls external APIs directly.

## Backend Architecture

### Task Lifecycle
Tasks flow through phases and statuses managed by `TaskStatusTransitionPolicy`:
- **Phases:** `PHASE1` → `PHASE2`
- **Statuses:** `QUEUED` → `RUNNING` → `COMPLETED` / `FAILED`
- **Modes:** `REAL` (live data), `SIMULATED` (generated fixtures), `AUTO_FALLBACK` (try real, fall back to simulated)

The `default-mode` is set via `app.task.default-mode` (default: `AUTO_FALLBACK`; local override: `REAL`).

### Domain Modules (`domain/`)
- **task** — `AnalysisTask` entity, `Phase1/2TaskApplicationService`, `Phase1/2TaskProcessor`, `TaskStatusTransitionPolicy`
- **analysis** — `ProductAnalysisService` (orchestrates pricing + match selection), `PricingEngine`, `MatchSelectionPolicy`
- **search** — `AmazonCrawlerProductSearchService`, `QueryRewriteService`, `SearchHistoryFallbackService`
- **candidate** — `CandidateProduct` model, `CandidateSnapshotService`
- **match** — `DomesticMatchService` (vector + keyword search, scoring via `ScoredCandidate`)
- **report** — `ReportAggregate`, `ReportAggregateService`, `ReportViewAssembler`, `StructuredReportMarkdownRenderer`
- **product / detail** — product data retrieval
- **asr** — audio file transcription flow
- **system** — `RelationalIntegrityAuditService`

### AI Workflows (`ai/workflow/`)
- `DiscoveryPhase1Workflow` — live Amazon crawl → filter by price gap → output `CandidateProduct` list
- `SourcingPhase2Workflow` — rewrite query → search 1688 → score matches → run pricing → build report

### Integration Module (`integration/`)
Each gateway has a `Routing*` implementation that switches between a real HTTP gateway and a simulated/fallback one based on config flags:
- **llm** — `RoutingLLMGateway` → `HttpLLMGateway` / `SimulatedLLMGateway`
- **domestic** — `RoutingDomesticMarketplaceGateway` → `Http1688MarketplaceGateway`
- **overseas** — `RoutingOverseasMarketplaceGateway` → `HttpAmazonMarketplaceGateway`
- **crawler** — crawler HTTP client
- **asr** — ASR HTTP client

### Persistence
- **PostgreSQL** — primary store; schema managed by Flyway (`db/migration/V*.sql`)
- **Redis** — task state caching
- New schema changes must go in a new `V{N}__description.sql` migration file — do not modify existing migrations or `schema.sql`.

### Trace Correlation
All services propagate `X-Trace-Id` header. Spring sets it in MDC (`[%X{traceId:-no-trace}]`); the crawler reads/generates it via `TraceIdMiddleware`.

## Frontend Architecture (Next.js)

App routes under `src/app/`:
- `/` — main workbench (`Workbench` component)
- `/history` — task history
- `/reports` — report list/detail
- `/settings` — configuration

State is managed with **Zustand** stores (`src/stores/`): `task-store`, `products-store`, `agent-store`, `settings-store`, `ui-store`.

`src/lib/` contains domain utilities including log-stage label maps (`LOG_STAGE_LABELS`, `LIVE_STAGE_MESSAGES`) and product category pattern matching used to annotate task progress in Chinese.

## Crawler Service Routes

| Prefix | Purpose |
|---|---|
| `/api/search` | Amazon product search |
| `/api/detail` | Amazon product detail |
| `/api/domestic/search` | 1688 keyword search |
| `/api/domestic/detail` | 1688 product detail |
| `/api/asr/transcribe` | Audio → text transcription |
| `/api/llm/rewrite` | Product title rewriting |
| `/api/llm/report-narrative` | Report narrative generation |
| `/api/llm/reasoning` | Reasoning/analysis |
| `/api/llm/transcript-intent` | ASR intent extraction |

## Configuration

- Active profile: `local` (default), override via `SPRING_PROFILES_ACTIVE`
- All secrets are injected via env vars — see `application.yml` for the full list of `${VAR:default}` bindings
- Key pricing knobs: `app.pricing.usd-to-cny-rate`, `cross-border-logistics-rate`, `platform-fee-rate`, `exchange-loss-rate`
- Vector search: `app.vector.*` — uses local BGE-M3 embeddings; `bootstrap-on-startup: true` loads the model on start

## Code Conventions

- Java: Lombok for boilerplate; inner record-style classes for execution context (e.g. `DomesticSearchExecution`, `ScoredCandidate`, `MatchCandidateAccumulator`)
- Jackson: `non_null` — omit null fields in JSON; dates as ISO strings
- Frontend: TypeScript strict mode, Tailwind CSS, shadcn/ui component patterns
- Routing gateways follow the pattern: interface → `Routing*` dispatcher → `Http*` real impl + simulated/fallback impl
