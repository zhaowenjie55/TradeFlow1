# Legacy Nuxt Frontend

This frontend is no longer the primary TradeFlow UI.

## Status

`globalvibe-web/` is kept only as a legacy reference while `tradeflow-web-next/` becomes the sole supported frontend.

Rules for this app:

- do not add new product features here
- do not treat this app as the source of truth for backend compatibility
- only use it for migration comparison or emergency regression checks

## Active Frontend

The canonical frontend now lives in:

```text
../tradeflow-web-next
```

## Local Use

If you explicitly need to run the legacy UI:

```bash
npm run dev
```

But normal development should happen in the React app instead.
