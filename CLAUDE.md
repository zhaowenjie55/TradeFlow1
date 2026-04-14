# TradeFlow — CLAUDE.md

Cross-border arbitrage discovery platform. Identifies price gaps between domestic Chinese markets and overseas platforms, uses AI to match products, and generates trade opportunity reports.

## Repository Structure

```
TradeFlow/
├── backend/              # Spring Boot 3 — core business logic & API
├── tradeflow-web-next/   # Next.js 16 — main product frontend (React/TSX)
├── globalvibe-web/       # Nuxt 3 — alternative/admin frontend (Vue)
├── crawler-service/      # Python FastAPI — product data crawler
├── models/               # Shared data models / specs
└── docs/                 # Architecture & API docs
```

## Services

| Service | Tech | Port | Purpose |
|---|---|---|---|
| backend | Spring Boot 3, Java 21 | 8080 | REST API, AI workflow, matching engine |
| tradeflow-web-next | Next.js 16 | 3000 | Main user-facing frontend |
| globalvibe-web | Nuxt 3 | 3001 | Secondary/admin frontend |
| crawler-service | Python FastAPI | 8000 | Product data scraping |

## Common Commands

### Backend
```bash
# Run (local profile, connects to PostgreSQL + Redis)
cd backend && mvn spring-boot:run

# Build jar
cd backend && mvn clean package -DskipTests

# API docs (when running)
open http://localhost:8080/swagger-ui.html
```

### Next.js Frontend
```bash
cd tradeflow-web-next
npm run dev      # dev server
npm run build    # production build
npm run lint     # lint
```

### Nuxt Frontend
```bash
cd globalvibe-web
npm run dev      # dev server
npm run build    # production build
```

### Crawler Service
```bash
cd crawler-service
pip install -r requirements.txt
uvicorn main:app --reload
```

## Backend Architecture

### Domain Modules (`domain/`)
- **analysis** — product analysis orchestration
- **match** — domestic/overseas product matching (`DomesticMatchService`)
- **report** — trade opportunity report generation (`DomesticProductMatch`, `DomesticProductMatchVO`)
- **search** — product search
- **product / candidate / detail** — product data models and retrieval
- **marketplace** — marketplace integration
- **task** — async task management
- **workflow** — workflow state tracking
- **asr** — ASR (Automatic Sourcing & Routing) flow

### AI Module (`ai/workflow/`)
- `DiscoveryPhase1Workflow` — Phase 1: find overseas products with price opportunity
- `SourcingPhase2Workflow` — Phase 2: source domestic suppliers for matched products

### Integration Module (`integration/`)
- **llm** — LangChain4j LLM client wrappers
- **domestic** — domestic platform API clients
- **overseas** — overseas platform API clients
- **crawler** — crawler service client
- **asr** — ASR integration

## Key Dependencies
- **LangChain4j** — LLM integration (embeddings + chat)
- **Spring Data Redis** — caching and task state
- **PostgreSQL** — primary database (hosted at `47.103.117.192:5432/tradeflow`)
- **SpringDoc OpenAPI** — API documentation
- **Virtual threads** (`spring.threads.virtual.enabled: true`) — Java 21 virtual threads enabled

## Configuration
- Active profile: `local` (default), override via `SPRING_PROFILES_ACTIVE`
- Config files: `application.yml`, `application-local.yml`
- CORS: configured to allow localhost dev servers (any common port)

## Code Conventions
- Java: Lombok for boilerplate, record-style inner classes for DTOs (e.g. `DomesticSearchExecution`, `ScoredCandidate`)
- Null handling: Jackson configured with `non_null` — omit null fields in JSON responses
- Dates: serialized as ISO strings, not timestamps
- Frontend: TypeScript strict mode, Tailwind CSS for styling
