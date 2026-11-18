# CallInsights

An event-driven call analytics pipeline built as a portfolio project. A synthetic call generator produces calls; services transcribe, summarise and evaluate them over Kafka; a catalog and gateway expose the results to an Angular supervisor UI.

**All data is synthetic.** The project is not affiliated with any employer, vendor or product.

## Status

| Area | State |
| --- | --- |
| Pipeline: generate, transcribe, summarise, evaluate | Working end to end |
| Catalog (MongoDB), audit log, JWT login gateway, Angular UI | Working |
| Run everything with one command (Docker) | Planned (M0b) |
| Spring AI port, tests and CI | Planned (M0, M1) |
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

Java 21, Spring Boot 3.3.4, Kafka (KRaft), MongoDB, Angular. Local infrastructure runs from `infra/docker-compose.yml`.

## Quick start

```bash
cd infra && docker-compose up -d        # Kafka, MongoDB, Kafka UI, Mongo Express
export ANTHROPIC_API_KEY="..."          # per terminal session; never commit it
scripts/start-all.sh                    # builds and starts the six services and the UI
scripts/run-test.sh                     # one call through the whole pipeline
scripts/stop-all.sh
```

Details, ports and debugging URLs are in [docs/README.md](docs/README.md).

## Documentation

- [docs/SPEC.md](docs/SPEC.md): problem statement, service boundaries, event contracts
- [docs/specs/](docs/specs/): one spec per milestone or use case, with acceptance criteria
- [ROADMAP.md](ROADMAP.md): what is built and what is planned
- [CLAUDE.md](CLAUDE.md): rules for the AI coding assistant used with human review
