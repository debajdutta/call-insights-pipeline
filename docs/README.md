# Call Insights Pipeline (Synthetic)

A skill-building microservices project: an event-driven call-transcription/summary/evaluation pipeline with a supervisor UI, built to gain current, demonstrable hands-on depth with Kafka, microservice architecture, and multi-provider LLM integration.

**This is an entirely synthetic rebuild** — own fake call generator, dummy media, own scoring rules. No real AudioCodes/SmartTAP code, data, or proprietary logic is used anywhere in this project.

See [`docs/SPEC.md`](./SPEC.md) for the full spec (problem statement, service boundaries, event contracts, data model, acceptance criteria) and [`docs/TASKS.md`](./TASKS.md) for the Day-1 implementation checklist.

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
- Full pipeline (Tasks 1-4) verified end-to-end live via `scripts/run-test.sh` — real Kafka, real Anthropic calls, correct artifacts at every stage.
- Tasks 5-8 (Metadata Consumer, Gateway/BFF, Frontend, full E2E) not yet started.
