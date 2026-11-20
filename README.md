# CallInsights

An event-driven call analytics pipeline built as a portfolio project. A synthetic call generator produces calls; services transcribe, summarise and evaluate them over Kafka; a catalog and gateway expose the results to an Angular supervisor UI.

**All data is synthetic.** The project is not affiliated with any employer, vendor or product.

## Status

| Area | State |
| --- | --- |
| Pipeline: generate, transcribe, summarise, evaluate | Working end to end |
| Catalog (MongoDB), audit log, JWT login gateway, Angular UI | Working |
| Run everything with one command (Docker) | Planned (M0b) |
| Root Maven build, shared event module, unit tests, CI | Built (M0) |
| Spring AI port | Planned (M1) |
| LLM evaluation, redaction, RAG, tool calling, MCP, model routing | Planned, see [ROADMAP.md](ROADMAP.md) |

Today the summary and transcription services call the Anthropic Java SDK directly, behind small provider interfaces; evaluation is rule-based.

## Architecture

```
call-generator --call-completed--> transcription-service --call-transcript-generated--> summary-service
                                                                  |                          |
                                                                  +----> evaluation-service  |
                                                                                             v
                          catalog-service (MongoDB: calls, artifacts, audit_log) <-- events
                                   ^
                                   |
                  gateway-service (JWT) <--> Angular UI (frontend/)
```

Kafka carries events only. Media and versioned JSON artifacts live on the filesystem, which is the source of truth for content.

## Stack

Java 21, Spring Boot 3.3.4, Kafka (KRaft), MongoDB, Angular 22. Local infrastructure runs from `infra/docker-compose.yml`.

The backend is one Maven reactor: the root `pom.xml` builds `callinsights-common` (the shared Kafka event records) and the six services.

## Prerequisites

- JDK 21 and Maven 3.9+
- Docker with Compose v2.20 or later (`docker compose`; the root compose file uses `include`), with about 6 GB of memory for the Docker path
- Node.js 22.22+ or 24 with npm (for the Angular frontend)
- An Anthropic API key, only for real transcription and summary generation

## Build and test

```bash
mvn -q verify                          # all modules, unit tests only: no Kafka, MongoDB or API key needed
mvn -pl summary-service -am test       # one service plus the modules it depends on
(cd frontend && npm ci && npm run build)
```

## Quick start

Run from the repository root:

```bash
export ANTHROPIC_API_KEY="..."   # per terminal session; never commit it
scripts/docker-up.sh             # or: docker compose up -d --build
```

Then open the UI at `http://localhost:4200` (gateway at `http://localhost:8086`). Stop with `docker compose down`; add `-v` to also wipe the Mongo and media volumes. Details, volumes and the API key handling are in [docs/DOCKER.md](docs/DOCKER.md).

### Run the services from source

For development and debugging, run the services as host processes instead. Stop the Docker stack first (`docker compose down`); the two cannot run at the same time.

```bash
export ANTHROPIC_API_KEY="..."          # per terminal session; never commit it
scripts/start-all.sh                    # starts Kafka, MongoDB, Kafka UI, Mongo Express, then builds and starts the six services and the UI
scripts/status.sh                       # health of infra, services and UI
scripts/run-test.sh                     # one call through the whole pipeline
scripts/stop-all.sh                     # stops everything, including infra
```

Details, ports and debugging URLs are in [docs/README.md](docs/README.md).

## Documentation

- [docs/SPEC.md](docs/SPEC.md): problem statement, service boundaries, event contracts
- [docs/DOCKER.md](docs/DOCKER.md): running the whole system with Docker Compose
- [docs/specs/](docs/specs/): one spec per milestone or use case, with acceptance criteria
- [ROADMAP.md](ROADMAP.md): what is built and what is planned
- [CLAUDE.md](CLAUDE.md): rules for the AI coding assistant used with human review
