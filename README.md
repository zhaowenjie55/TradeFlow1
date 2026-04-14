# TradeFlow

TradeFlow is a split-stack product sourcing workflow:

- `backend/`: Spring Boot orchestration, persistence, pricing, and reporting
- `crawler-service/`: FastAPI integrations for Amazon, 1688, ASR, and external LLM calls
- `tradeflow-web-next/`: canonical frontend, built with Next.js and React
- `globalvibe-web/`: legacy Nuxt frontend kept only for reference while the React client replaces it

## Frontend Direction

The React frontend in `tradeflow-web-next/` is the only actively supported UI.

The Nuxt app in `globalvibe-web/` is now considered legacy:

- no new feature work should land there
- backend/API changes should be validated against the React client first
- use it only as a temporary comparison surface during migration

## Development

Run the React frontend:

```bash
npm run frontend:dev
```

Build or start the React frontend:

```bash
npm run frontend:build
npm run frontend:start
```

The legacy Nuxt frontend is still available explicitly:

```bash
npm run frontend:legacy:dev
```

## Active Workflow

The main user flow is:

1. React UI submits Phase 1 task requests to `/api/analysis/tasks`
2. Spring backend orchestrates Phase 1 and Phase 2 workflows
3. FastAPI crawler performs live scraping, 1688 detail loading, and external LLM calls
4. Spring persists candidate snapshots, match records, and final reports

## Known Architectural Rule

If frontend behavior differs between `tradeflow-web-next/` and `globalvibe-web/`, treat the React implementation as the source of truth and port or remove the Nuxt behavior rather than maintaining both.
