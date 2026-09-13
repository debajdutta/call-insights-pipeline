# Call Insights Pipeline (Synthetic)

A skill-building microservices project: an event-driven call-transcription/summary/evaluation pipeline with a supervisor UI, built to gain current, demonstrable hands-on depth with Kafka, microservice architecture, and multi-provider LLM integration.

**This is an entirely synthetic rebuild** — own fake call generator, dummy media, own scoring rules. No real AudioCodes/SmartTAP code, data, or proprietary logic is used anywhere in this project.

See [`docs/SPEC.md`](./SPEC.md) for the full spec (problem statement, service boundaries, event contracts, data model, acceptance criteria), [`docs/TASKS.md`](./TASKS.md) for the Day-1 implementation checklist, and [`docs/API_TESTING.md`](./API_TESTING.md) for curl examples against the Gateway/BFF API.

## Stack

- **Frontend:** Angular + TypeScript (Node.js is a build-time toolchain only, not a runtime service)
- **Backend services:** Java 21 + Spring Boot (Gateway/BFF, Call Generator, Transcription, Summary/Insights, Evaluation, Metadata Consumer)
- **Middleware:** Kafka (event orchestration only — media never flows through Kafka)
- **Storage:** MongoDB (metadata + artifact catalog + audit log), local filesystem/Blob (media + versioned JSON artifacts — source of truth for content)
- **Local dev:** Docker Compose for Kafka + MongoDB; services run individually via IDE/CLI

## Running locally (Day 1)

Repo name: `call-insights-pipeline`


```bash
cd infra
docker-compose up -d       # starts Kafka + MongoDB
```

Then run each Spring Boot service individually (`mvn spring-boot:run` or via IDE), and the frontend via:

```bash
cd frontend
npm install
ng serve
```

## Scripts

`scripts/` automates the local dev loop — see each file's header comment for details:

| Script | Does |
|---|---|
| `scripts/start-all.sh` | Brings up Docker infra if needed, rebuilds and (re)launches all 4 backend services. Idempotent. Requires `ANTHROPIC_API_KEY` set in the shell (e.g. via `~/.zshrc`) for transcription-service/summary-service to do real work. |
| `scripts/stop-all.sh` | Stops the 4 backend services; leaves Docker infra running. |
| `scripts/status.sh` | Read-only readiness check: infra container health, all 4 actuator endpoints, API key presence (name only, never the value). |
| `scripts/run-test.sh` | Triggers one call generation, waits for the full pipeline to complete, prints the log trace and all three generated artifacts. |

## Monitoring

**Infra (Docker containers):**

```bash
cd infra
docker compose ps                    # status of Kafka, MongoDB, kafka-ui, mongo-express
docker compose logs -f <service>     # e.g. kafka, mongo
```

- Kafka UI: http://localhost:8090
- Mongo Express: http://localhost:8091

**Spring Boot services** — each writes its own log via Spring Boot's native file logging (`logging.file.name` in `application.yml`, includes automatic rotation), to a dedicated path inside the repo:

```bash
tail -f logs/<service>.log
grep <callId> logs/*.log   # trace one call across every service
```

(`logs/` is gitignored — runtime output, not source. `scripts/start-all.sh` always writes here regardless of which directory it's run from.)

Each service also exposes Spring Boot Actuator for health/metrics without needing the logs:

| Service | Port | Health endpoint | Manual trigger |
|---|---|---|---|
| call-generator | 8081 | `GET /actuator/health` | `POST /api/calls/generate` |
| transcription-service | 8082 | `GET /actuator/health` | (event-driven; consumes `call-completed`) |
| summary-service | 8083 | `GET /actuator/health` | (event-driven; consumes `call-transcript-generated`) |
| evaluation-service | 8084 | `GET /actuator/health` | (event-driven; consumes `call-transcript-generated`) |
| catalog-service | 8085 | `GET /actuator/health` | (event-driven; consumes `call-completed`/`*-generated`/`artifact-deleted`) |
| gateway-service | 8086 | `GET /actuator/health` | `POST /api/auth/login`, `GET /api/calls[/{callId}]`, `POST .../regenerate`, `DELETE .../artifacts/{type}` |

## How to use this with Claude Code

Open this repo in Claude Code and start with:

> "Read docs/SPEC.md and docs/TASKS.md. Start with Task 0 (infra bootstrap) and Task 1 (Call Generator, FR1). Explain your approach before writing code, then show me the diff before applying it."

Work through `TASKS.md` in order — each task maps to an FR ID in `SPEC.md`.

## Status

- **Task 0 (infra bootstrap)** — done, verified: all four containers (Kafka, MongoDB, kafka-ui, mongo-express) start healthy via `docker-compose up -d`; both UIs reachable.
- **Task 1 (Call Generator, FR1)** — done, verified: `call-completed` event confirmed directly on the Kafka topic (not just app logs), correct key/payload.
- **Task 2 (Transcription Service)** — done, verified: consumes `call-completed`, calls Anthropic for a synthetic transcript (scenario-seeded per template for verifiable ground truth), writes `transcript_v1.json`, publishes `call-transcript-generated`.
- **Task 3 (Summary/Insights Service)** — done, verified: consumes `call-transcript-generated`, calls Anthropic for a summary, writes `summary_v1.json`, publishes `call-summary-generated`.
- **Task 4 (Evaluation Service)** — done, verified: consumes `call-transcript-generated`, rule-based keyword scoring (no LLM), writes `evaluation_v1.json`, publishes `call-evaluation-generated`.
- **Task 5 (Metadata Consumer, FR3)** — done, verified: renamed `catalog-service` during implementation (see SPEC.md). Consumes `call-completed`/`call-transcript-generated`/`call-summary-generated`/`call-evaluation-generated` (plus `artifact-deleted`, not yet published by anything), writes `calls`, `artifacts` (current version + version history), and `audit_log` collections to MongoDB. Confirmed directly in MongoDB, not just app logs.
- Full pipeline (Tasks 1-5) verified end-to-end live via `scripts/run-test.sh` — real Kafka, real Anthropic calls, real MongoDB catalog writes, correct artifacts at every stage.
- **Task 6 (Gateway/BFF, FR4/FR5/FR6/FR7/FR8)** — done, verified: DB-backed login (MongoDB `users` collection, bcrypt, JWT via `POST /api/auth/login`), `GET /api/calls` and `GET /api/calls/{callId}` (catalog + artifact content read from MongoDB + local FS), `GET /api/calls/{callId}/audit`, `POST /api/calls/{callId}/artifacts/{artifactType}/regenerate` and `DELETE /api/calls/{callId}/artifacts/{artifactType}` (both publish Kafka events consumed by `catalog-service`). Verified live: login success/failure, unauthenticated 401, regenerate correctly bumped a version (1→2), delete removed the file from disk and marked the catalog entry `deleted: true` while preserving version history.
- **Task 7 (Frontend, FR4/FR5/FR6)** — done, verified in-browser (manual + user testing): Angular 22 app (`frontend/`, standalone components, signals, lazy-loaded routes), login page against `gateway-service`, call list, call detail with transcript/summary/evaluation content, regenerate (with model input) and delete actions that poll the backend until the change actually lands rather than guessing a fixed delay, with a visible notice if that poll times out (~60s). Required adding CORS support to `gateway-service` (`WebConfig`, origin configurable via `GATEWAY_CORS_ALLOWED_ORIGIN`) since the frontend (`localhost:4200`) and Gateway (`localhost:8086`) are different origins. Known rough edges (UI polish, error messaging) are being addressed incrementally rather than blocking this milestone.
- Task 8 (full E2E across all 7 services via `docker-compose up`) not yet started.
