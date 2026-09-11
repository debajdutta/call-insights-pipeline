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

## Monitoring

**Infra (Docker containers):**

```bash
cd infra
docker compose ps                    # status of Kafka, MongoDB, kafka-ui, mongo-express
docker compose logs -f <service>     # e.g. kafka, mongo
```

- Kafka UI: http://localhost:8090
- Mongo Express: http://localhost:8091

**Spring Boot services** (run individually on Day 1, not containerized — see SPEC.md §7/§8):

Each service logs to stdout; redirect to a file if running in the background, e.g.:

```bash
nohup java -jar target/<service>-*.jar > /tmp/<service>.log 2>&1 &
tail -f /tmp/<service>.log
```

Each also exposes Spring Boot Actuator for health/metrics without needing the logs:

| Service | Port | Health endpoint | Manual trigger |
|---|---|---|---|
| call-generator | 8081 | `GET /actuator/health` | `POST /api/calls/generate` |

(Add a row here as each new service comes online.)

## How to use this with Claude Code

Open this repo in Claude Code and start with:

> "Read docs/SPEC.md and docs/TASKS.md. Start with Task 0 (infra bootstrap) and Task 1 (Call Generator, FR1). Explain your approach before writing code, then show me the diff before applying it."

Work through `TASKS.md` in order — each task maps to an FR ID in `SPEC.md`.

## Status

- **Task 0 (infra bootstrap)** — done, verified: all four containers (Kafka, MongoDB, kafka-ui, mongo-express) start healthy via `docker-compose up -d`; both UIs reachable.
- **Task 1 (Call Generator, FR1)** — done, verified: `call-completed` event confirmed directly on the Kafka topic (not just app logs), correct key/payload.
- Tasks 2-8 not yet started.
