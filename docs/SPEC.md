# Call Insights Pipeline — SPEC.md

## 0. Purpose & Guardrails

**Purpose:** A skill-building project to gain current, demonstrable hands-on depth with Kafka Streams event orchestration, microservice architecture, multi-provider LLM integration, and a full-stack (Angular + Spring Boot) supervisor tool — closing specific gaps between the resume and target job postings (Kafka Streams named as a preferred skill at JPMorgan Chase and others).

**Guardrail (non-negotiable):** This is an entirely **synthetic** rebuild.
- Own fake call-metadata generator.
- Own dummy media files.
- Own template/keyword scoring rules.
- **Never** AudioCodes/SmartTAP's real code, data, proprietary scoring logic, or credentials.
- The real SmartTAP system is referenced only at the architecture-pattern level (event-driven, pluggable storage) — never its implementation.

**Resume ground rule (from the broader backlog):** a skill only goes on the resume once this is actually built and run — this SPEC is the design; it is not itself evidence of completion.

---

## 1. Problem Statement

Build a synthetic, event-driven agent-evaluation platform: when a call recording completes, the system automatically transcribes it, generates an insights/summary, and evaluates the agent's performance against a template — all pluggable across multiple LLM providers. A supervisor can log in, browse call records, view generated artifacts, and manually delete or regenerate any artifact (optionally choosing a different model provider), producing a new version each time. All activity is audit-logged.

---

## 2. Goals (Functional Requirements)

| ID | Requirement |
|----|-------------|
| FR1 | Synthetic call-record generator produces call metadata + dummy media, streamed to Blob/local filesystem, with a `call-completed` event published to Kafka |
| FR2 | Transcription, Summary/Insights, and Evaluation services consume `call-completed` (or regeneration-request) events in parallel and each produce a versioned JSON artifact file, written next to the media |
| FR3 | A Metadata Consumer writes/updates call + artifact catalog entries in MongoDB (source-of-truth for *location and version*, not content) |
| FR4 | Frontend (Angular) supports DB-backed login (credentials in MongoDB, bcrypt-hashed; Gateway/BFF validates and issues a short-lived JWT), lists call records, and lets a supervisor view existing transcripts/summaries/evaluations |
| FR5 | Supervisor can trigger generation/regeneration of any artifact, selecting a model provider from a model registry |
| FR6 | Supervisor can delete an artifact (hard delete); regeneration without prior delete creates a new, incremented version |
| FR7 | Every generate/delete/regenerate action is recorded in an audit log (who, what, when, which version, which model) |
| FR8 | A Gateway/BFF (Spring Boot) is the single entry point for the frontend, fronting all backend services |

## 3. Non-Goals (Day 1 / this phase)

- No true real-time/streaming ASR or live keyword spotting, call tagging, or in-call analytics (explicitly deferred — see §7).
- No OAuth/real identity provider — login is app-managed (MongoDB-backed users, bcrypt + JWT), not a third-party IdP; OAuth/SSO is a documented future increment.
- No MySQL (superseded by MongoDB for all structured metadata).
- No Kubernetes/cloud deployment yet (local-first via Docker Compose; see §8).
- No RAG/vector DB (separate backlog item, not part of this project).
- No production-grade security hardening (secrets management, rate limiting, etc.) — noted as future work, not blocking.

---

## 4. Architecture Overview

### 4.1 Service Boundaries

| Service | Responsibility |
|---|---|
| **Frontend** (Angular + TypeScript) | Login (DB-backed credentials via Gateway/BFF, JWT held client-side), call-record list/detail, artifact viewing, generate/regenerate/delete actions, model-provider selection |
| **Gateway / BFF** (Spring Boot) | Single entry point for frontend; validates login credentials against MongoDB (bcrypt) and issues/validates JWTs; routes to backend services; aggregates responses |
| **Call Generator** (Spring Boot) | Synthetic call + media generator; writes media to Blob/local FS; publishes `call-completed` |
| **Transcription Service** (Spring Boot) | Consumes `call-completed`/regeneration events; calls selected model provider; writes versioned transcript JSON; publishes `call-transcript-generated` |
| **Summary/Insights Service** (Spring Boot) | Same pattern, produces summary/insights JSON; depends on transcript existing |
| **Evaluation Service** (Spring Boot) | Same pattern, produces evaluation JSON (rule-based template scoring, not ML); depends on transcript existing |
| **Metadata Consumer** (Spring Boot) | Consumes all `call-completed` / `*-generated` / delete events; upserts MongoDB catalog + audit log |
| **Model Registry** (config, not a running service initially) | Maps logical model name → provider, endpoint, credentials-env-var, request shape |

### 4.2 Data Flow (Day 1: post-processing only)

```
Call Generator
   │
   ├──► media file written directly to Blob/local FS (NOT via Kafka)
   │
   └──► "call-completed" event → Kafka
                                     │
              ┌──────────────────────┼──────────────────────┐
              ▼                      ▼                      ▼
      Transcription Svc       Summary/Insights Svc    Evaluation Svc
      (needs media)           (needs transcript)      (needs transcript)
              │                      │                      │
              └── writes {artifact}_v{n}.json next to media (Blob/local FS) ──┘
              │                      │                      │
              └──────────────────────┴──────────────────────┘
                                     │
                      "call-{artifact}-generated" event → Kafka
                                     │
                                     ▼
                          Metadata Consumer
                                     │
                                     ▼
                    MongoDB (catalog + audit log)
                                     │
                                     ▼
                          Gateway/BFF ◄──── Frontend (supervisor)
                                     │
                    supervisor: delete / regenerate (+ model choice)
                                     │
                    "regeneration-requested" event → Kafka (same consumers)
```

**Note on Summary/Evaluation depending on Transcript:** since both consume events but need transcript content, they either (a) also consume `call-transcript-generated` rather than `call-completed` directly, or (b) consume `call-completed` but poll/fetch the transcript file, retrying if not yet present. **Decision: (a)** — cleaner, avoids polling. Only Transcription Service consumes `call-completed` directly; Summary and Evaluation both consume `call-transcript-generated`.

### 4.3 Kafka Topics (draft)

| Topic | Producer | Consumers | Payload (key fields) |
|---|---|---|---|
| `call-completed` | Call Generator | Transcription Service, Metadata Consumer | callId, agentId, mediaPath, templateId, timestamp |
| `call-transcript-generated` | Transcription Service | Summary Service, Evaluation Service, Metadata Consumer | callId, agentId, templateId, transcriptPath, version, modelUsed |
| `call-summary-generated` | Summary Service | Metadata Consumer | callId, summaryPath, version, modelUsed |
| `call-evaluation-generated` | Evaluation Service | Metadata Consumer | callId, evaluationPath, version, modelUsed |
| `artifact-regeneration-requested` | Gateway/BFF | Transcription/Summary/Evaluation Service (whichever matches artifactType) | callId, artifactType, requestedModel, requestedBy |
| `artifact-deleted` | Gateway/BFF | Metadata Consumer | callId, artifactType, version, deletedBy |

*(Topic/payload names are a Day-1 draft — refine once TASKS.md implementation starts.)*

### 4.4 Storage

- **MongoDB** — call metadata, artifact catalog (current version + full version history pointers), audit log. Source of truth for *what exists and where*, not artifact *content*.
- **Files (Blob or local filesystem, pluggable)** — media, and versioned JSON artifacts (`transcript_v1.json`, `transcript_v2.json`, ...), stored alongside the media. **Source of truth for content.**
- **Versioning:** monotonically increasing per artifact type per call; never reused, even after delete. Delete = hard delete of the file + catalog entry (marked deleted in audit log, not silently removed from history).

### 4.5 Model Registry

A configuration (not a running service on Day 1) mapping a logical model name to: provider (OpenAI/Azure OpenAI/Anthropic/etc.), endpoint, credential env-var reference, request-shape adapter. Transcription/Summary/Evaluation services read this config to know how to call whichever provider the user selected at generation time.

---

## 5. Non-Functional Requirements / Constraints

- Local-first (see §8) — must run entirely on one developer machine via Docker Compose for Kafka + MongoDB, with services run directly (IDE/CLI) during active development.
- All artifacts and metadata are synthetic; no real customer or employer data at any point.
- Services should be provider-agnostic and trigger-agnostic (auto vs. manual regeneration hit the same consumer logic).
- Audit log is append-only.

---

## 6. Acceptance Criteria (Day 1)

- [ ] Call Generator produces a synthetic call + dummy media file, writes media to local FS/Blob, publishes `call-completed`.
- [ ] Transcription Service consumes `call-completed`, calls a real model provider (at least one, e.g. Azure AI Speech or an LLM-based mock transcription), writes `transcript_v1.json`, publishes `call-transcript-generated`.
- [ ] Summary and Evaluation Services each consume `call-transcript-generated`, produce their own versioned JSON, publish their `*-generated` events.
- [ ] Metadata Consumer writes catalog + audit entries to MongoDB for all of the above.
- [ ] Frontend: DB-backed login (MongoDB users, bcrypt-hashed passwords, JWT issued by Gateway/BFF), list of call records, detail view showing transcript/summary/evaluation (if present).
- [ ] Frontend: supervisor can trigger delete and regenerate (with model selection) for any artifact; both flows work end-to-end and produce correct versioning + audit entries.
- [ ] Gateway/BFF is the only service the frontend talks to.
- [ ] Entire system runs locally via `docker-compose up` (Kafka + MongoDB containerized) + services run individually.

---

## 7. Future Increments (explicitly deferred)

- **Real-time processing:** true streaming ASR + live keyword spotting / call tagging / in-call analytics. Deferred because there is currently no consumer that needs mid-call action — building streaming plumbing without a real use case isn't worth the complexity yet. Revisit once a live-analytics consumer is actually needed.
- **OAuth/SSO** in place of app-managed DB login.
- **Containerize own services** (each gets a Dockerfile; full system runs via one `docker-compose up`) — natural next milestone after Day 1.
- **Cloud deployment** (AKS) — after containerization; SMB-style alternate `ReportStore` backend proven by running in two environments.
- **Kafka Streams state-store / Interactive Queries** for windowed aggregates (e.g. pass/fail rate over time) — plain consumers writing to Mongo are sufficient for Day 1; revisit if aggregate analytics become a real requirement.
- **Model Registry as its own service** (currently just config).
- Tie-in with the separate Prompt Evaluation Harness backlog item, for scoring the scoring itself.
- **Real ASR via a self-hosted Whisper model, paired with a voice-recorder-as-call-generator** (real mic input instead of synthetic metadata) — gives genuine ground-truth transcription testing instead of LLM-mock transcripts. Deliberately deferred until after the synthetic pipeline is proven end-to-end through the frontend, since it's a materially larger, separate piece of work (new native/Python dependency for Whisper, no ffmpeg on this dev machine so `faster-whisper` over the original `whisper` package, plus a voice-recorder component not yet described anywhere in this SPEC). **Revisit once Tasks 5-8 (Metadata Consumer, Gateway, Frontend) are built and tested — flag this back to the user at that point.**

---

## 8. Deployment Staging Plan

1. **Now (local dev):** Kafka + MongoDB via Docker Compose; all Spring Boot services run via IDE/`mvn spring-boot:run`; Angular via `ng serve`. Node.js/npm is a **build-time toolchain only** for Angular — not a separate runtime service; no Node backend layer between Angular and the Gateway/BFF.
2. **Next milestone:** Dockerfile per own service; single `docker-compose.yml` brings up the *entire* system (git-tag this milestone).
3. **Future:** Move to AKS (Azure, consistent with existing Blob/AI Speech usage); compose → Kubernetes manifests or Helm chart.

---

## 9. Repository Layout (monorepo)

```
call-insights-pipeline/
├── docs/
│   ├── SPEC.md
│   ├── TASKS.md
│   └── README.md
├── infra/
│   └── docker-compose.yml        # Kafka, MongoDB (+ own services in milestone 2)
├── frontend/                      # Angular + TypeScript
├── gateway/                       # Spring Boot BFF
├── call-generator/                # Spring Boot
├── transcription-service/         # Spring Boot
├── summary-service/                # Spring Boot
├── evaluation-service/             # Spring Boot
├── metadata-consumer/              # Spring Boot
└── model-registry/                 # shared config module
```

Chosen over multi-repo: easier to demo end-to-end, no cross-repo versioning overhead for a single-developer project at this scale.

---

## 10. Open Items / To Refine During Implementation

- Exact Kafka payload schemas (Avro/JSON Schema vs. plain JSON) — plain JSON recommended for Day 1 simplicity.
- Exact model-registry config format (YAML vs. properties vs. DB-backed later).

### Resolved during implementation

**Task 0 (infra):**
- Kafka image: `apache/kafka:3.8.0` (official image, native KRaft support, no Confluent-specific env vars needed).
- No auth on Kafka or MongoDB — local dev only, revisit if this ever runs anywhere shared.
- kafka-ui on `localhost:8090`, mongo-express on `localhost:8091` (ports not specified in SPEC/TASKS; chosen during implementation).

**Task 1 (Call Generator, FR1):**
- `call-completed` payload now includes `agentId` in addition to the four fields originally drafted above — the Metadata Consumer (Task 5) will need it on the call record. The table above has been updated to reflect this.
- `agentId`: fixed pool of 8 (`agent-001`..`agent-008`), configurable via `call-generator.agent-pool`.
- `templateId`: fixed pool of 5 (`TEMPLATE_SALES_CALL`, `TEMPLATE_SUPPORT_CALL`, `TEMPLATE_ONBOARDING_CALL`, `TEMPLATE_COLLECTIONS_CALL`, `TEMPLATE_RETENTION_CALL`), configurable via `call-generator.template-pool`. **Evaluation Service (Task 4) should key its rule-based scoring off these IDs.**
- `timestamp`: ISO-8601 string (`Instant.now().toString()`), not epoch millis/nanos — chosen for cross-service/human readability; Spring Kafka's default `JsonSerializer` does not auto-format `java.time.Instant` as ISO-8601, so the field is typed as `String` at the source rather than relying on serializer config.
- Trigger mechanism: REST endpoint (`POST /api/calls/generate`) over `CommandLineRunner`, so generation can be triggered repeatedly against a running instance instead of only once at JVM startup.
- Dummy media: 2-second silent WAV (8kHz, mono, 16-bit) via `javax.sound.sampled` — no external audio-codec dependency needed.

**Decided ahead of Task 5/6 (Metadata Consumer, Gateway/BFF):**
- Login mechanism upgraded from hardcoded in-memory users (original FR4) to DB-backed: a `users` collection in MongoDB (username + bcrypt-hashed password), validated by the Gateway/BFF, which issues a short-lived JWT for the frontend to hold. Chosen to keep a minimal-but-real security practice in scope for this skill-building project, without taking on full OAuth/IdP integration (still deferred, see §7).

**Task 6 (Gateway/BFF, FR4/FR5/FR6/FR7/FR8):**
- Gateway/BFF publishes regeneration/delete events directly to Kafka (resolves the open item above) — keeps Transcription/Summary/Evaluation Services purely event-driven and symmetric between auto and manual triggers; Gateway needs no synchronous endpoint on any of them.
- Gateway reads MongoDB directly for catalog data (`calls`, `artifacts`, `audit_log` collections) rather than calling a read API on the Metadata Consumer (`catalog-service`) — both are just Spring Data Mongo repositories against the same database, simplest option for Day 1.
- JWT: `io.jsonwebtoken:jjwt` (0.12.x), HS256, secret via `JWT_SECRET` env var (local-dev fallback in `application.yml`, must be ≥32 bytes), 8-hour expiration. Hand-rolled `OncePerRequestFilter` bearer-token check rather than full Spring Security — only two auth states exist (anonymous on `/api/auth/login` + `/actuator/**`, authenticated everywhere else), so a filter framework added configuration surface without buying anything.
- Password hashing: `spring-security-crypto`'s `BCryptPasswordEncoder` only (not the full `spring-boot-starter-security` — avoids its auto-configured filter chain/default login page, which nothing here needs).
- First supervisor user seeded via a `CommandLineRunner` (`UserSeeder`) on startup if the `users` collection is empty, using `SEED_USER_USERNAME`/`SEED_USER_PASSWORD` env vars (dev fallback: `supervisor`/`ChangeMe123!`). No user-management UI/API yet — additional users are inserted into MongoDB directly.
- Delete semantics (FR6): Gateway looks up the artifact's current version+path from the catalog, deletes the file from local FS itself, then publishes `artifact-deleted` — `catalog-service` (Task 5) marks the catalog entry `deleted: true` and appends the audit log entry. Version history is preserved in the catalog doc even after delete, matching §4.4's "hard delete of file + catalog entry, not silently removed from history."
- Regenerate (FR5) is fire-and-forget from the Gateway's perspective: it validates the call/artifact type exist, then publishes `artifact-regeneration-requested`; the version bump itself already happens inside Transcription/Summary/Evaluation Service (built in Tasks 2-4), so Gateway does no version bookkeeping of its own.
- Gateway/BFF port: `8086` (next after `catalog-service`'s `8085`).
