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

## How to use this with Claude Code

Open this repo in Claude Code and start with:

> "Read docs/SPEC.md and docs/TASKS.md. Start with Task 0 (infra bootstrap) and Task 1 (Call Generator, FR1). Explain your approach before writing code, then show me the diff before applying it."

Work through `TASKS.md` in order — each task maps to an FR ID in `SPEC.md`.

## Status

Design/documentation phase complete (SPEC.md, TASKS.md, README.md). Implementation not yet started.
