# TASKS.md — Day 1 Checklist

Mapped to FR IDs in SPEC.md. Suggested order follows the data flow (generator → transcription → summary/evaluation → metadata → frontend → gateway).

## Task 0 — Infra bootstrap
- [ ] `infra/docker-compose.yml`: Kafka (+ Zookeeper or KRaft mode), MongoDB
- [ ] Confirm `docker-compose up` brings up Kafka + Mongo, reachable from host
- **Claude Code prompt pattern:** *"Set up docker-compose.yml with Kafka (KRaft mode, no Zookeeper) and MongoDB, with sensible default ports and a named volume for Mongo data."*

## Task 1 — Call Generator (FR1)
- [ ] Synthetic call-metadata generator (random callId, agentId, templateId, timestamp)
- [ ] Dummy media file generator (silence/tone .wav or reused sample files) written to local FS (Blob later)
- [ ] Publish `call-completed` event to Kafka with callId/mediaPath/templateId/timestamp
- **Claude Code prompt pattern:** *"Implement a CallGenerator service that creates synthetic call metadata and a dummy .wav file, writes the file to a configurable local directory, and publishes a call-completed event to Kafka. Explain your approach before writing code."*

## Task 2 — Transcription Service (FR2, part of FR5)
- [ ] Kafka consumer for `call-completed`
- [ ] Model-registry lookup for selected/default provider
- [ ] Call provider (start with one real provider — Azure AI Speech or an LLM used as a mock transcriber)
- [ ] Write `transcript_v{n}.json` next to media
- [ ] Publish `call-transcript-generated`
- [ ] Handle `artifact-regeneration-requested` (artifactType=transcript) the same way, incrementing version

## Task 3 — Summary/Insights Service (FR2, part of FR5)
- [ ] Kafka consumer for `call-transcript-generated` (not `call-completed`)
- [ ] Read transcript file, call selected model provider for summary/insights
- [ ] Write `summary_v{n}.json`, publish `call-summary-generated`
- [ ] Handle regeneration-requested (artifactType=summary)

## Task 4 — Evaluation Service (FR2, part of FR5)
- [ ] Kafka consumer for `call-transcript-generated`
- [ ] Rule-based template/keyword scoring (NOT ML) against transcript
- [ ] Write `evaluation_v{n}.json`, publish `call-evaluation-generated`
- [ ] Handle regeneration-requested (artifactType=evaluation)

## Task 5 — Metadata Consumer (FR3, FR7)
- [ ] Consume `call-completed`, all `*-generated`, `artifact-deleted`, `artifact-regeneration-requested`
- [ ] Upsert MongoDB catalog: call record + per-artifact-type current version + file path
- [ ] Append-only audit log collection: action, artifactType, version, actor, model used, timestamp

## Task 6 — Gateway/BFF (FR8)
- [ ] REST endpoints: list calls, get call detail (with artifact status/versions), trigger generate/regenerate (with model param), trigger delete
- [ ] Publishes `artifact-regeneration-requested` / `artifact-deleted` directly to Kafka
- [ ] Reads from MongoDB for list/detail views

## Task 7 — Frontend (FR4, FR5, FR6)
- [ ] Hardcoded-user login screen
- [ ] Call record list view
- [ ] Call detail view: show transcript/summary/evaluation if present (current version), with version history accessible
- [ ] Generate/Regenerate action with model-provider dropdown (from model registry, exposed via Gateway)
- [ ] Delete action (confirm before hard delete)

## Task 8 — End-to-end verification
- [ ] Full flow: generate a call → auto-pipeline produces all 3 artifacts → visible in UI
- [ ] Manual delete → artifact gone, audit log shows it
- [ ] Manual regenerate with a different model → new version appears, audit log shows model used
- [ ] Confirm Summary/Evaluation correctly wait on Transcript (don't fire prematurely)

## Not in Day 1 scope (see SPEC.md §7)
- Real-time/streaming ASR, live keyword spotting/tagging
- OAuth
- Containerizing own services / cloud deployment
- Kafka Streams state store / Interactive Queries for aggregates
