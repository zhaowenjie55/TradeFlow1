# TradeFlow React Frontend

This is the canonical TradeFlow frontend. It is the only UI that should receive new feature work.

## Scope

The app is responsible for:

- creating Phase 1 analysis tasks
- polling Phase 1 and Phase 2 task status
- selecting overseas candidates for Phase 2
- handling `WAITING_1688_VERIFICATION` resume flow
- rendering structured reports and provenance

It talks to the Spring backend through `/api/analysis/tasks`.

## Development

Run the app locally:

```bash
npm run dev
```

Build and start:

```bash
npm run build
npm run start
```

Lint:

```bash
npm run lint
```

## Project Position

`tradeflow-web-next/` replaces `globalvibe-web/`.

If you find UI behavior that still exists only in the Nuxt app, port it here and remove the duplication instead of maintaining both implementations long term.
