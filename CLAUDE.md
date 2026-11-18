# CLAUDE.md: rules for Claude Code in this repo

CallInsights is a **portfolio project**: an event-driven call transcription, summary and evaluation pipeline built on **synthetic data only**. A human reviews every change. Read this file at the start of every session.

## Project facts (verify against the code before relying on them)

- Six Spring Boot services, Java 21: `call-generator` (8081), `transcription-service`, `summary-service`, `evaluation-service` (8084), `catalog-service`, `gateway-service` (8086). Ports 8081 to 8086, see each `application.yml`.
- Frontend: Angular in `frontend/`.
- Infra: Kafka (KRaft) and MongoDB via `infra/docker-compose.yml`. Media and versioned JSON artifacts live on the local filesystem; MongoDB holds the catalog and audit log.
- Packages: `com.callinsights.<service>`. Keep this namespace.
- LLM access today: the Anthropic Java SDK behind `SummaryProvider` and `TranscriptionProvider`. Evaluation is the rule-based `KeywordScorer`. Spring AI is introduced in milestone M1.
- Docs: `docs/SPEC.md`, `docs/TASKS.md`, `docs/API_TESTING.md`, `docs/specs/` (one spec per milestone or use case), `ROADMAP.md`.

## Hard rules

1. **Synthetic data only.** Never add real call content, real names, real customer or company data.
2. **No employer, client or target-company names** in code, tests, docs, commit messages or prompts. Use neutral wording ("a commercial call-recording product").
3. **No secrets.** Never write an API key anywhere. Never read, create or edit `.env`, `application.properties`, or `.claude/settings.local.json`. The key comes from `ANTHROPIC_API_KEY` exported in the human's shell for that session.
4. **Never push, never merge, never tag** unless the human asks. Commit on the working branch only. Do not touch `main`.
5. **Do not change event contracts** (topic names, event fields) without an entry in `docs/adr/` and the human's approval.
6. **Prompts and tool definitions need human approval.** Show them in the plan and again in the diff summary.
7. Do not add dependencies or upgrade framework versions outside the spec in front of you.
8. Do not edit `.github/modernize/` (tooling from another assistant) or anything under `logs/` and `media-store/`.

## Working method

1. **Plan first.** Start in plan mode: files to change, tests to add, risks, what you will not do. Wait for approval.
2. **Tests first** where practical. One spec, one branch, small commits (one logical change each, imperative subject line).
3. Run the build and tests before saying anything is done. Report the commands and the result. Never claim a pass you did not run.
4. Stay inside the spec's scope. Anything else goes in the status note as "found, not done".
5. Finish with a **status note** (template below).

## Build and test

- Per module until the root POM exists (M0): `mvn -f <service>/pom.xml test`
- After M0: `mvn -q verify` at the root.
- Whole system locally: `scripts/start-all.sh`, `scripts/status.sh`, `scripts/run-test.sh`, `scripts/stop-all.sh`.
- Frontend: see `frontend/package.json` for the test and build scripts.
- Live LLM calls are manual only and never run in CI.

## Status note template

```
Spec: <file>   Branch: <name>   Commits: <n>
Done: <what changed, in 3 to 6 bullets>
Verified: <commands run and results>
Not done / found: <list>
Needs human: <approvals, a real-key run, decisions>
```

## Decisions in force (change here and in an ADR if they change)

- D1 Framework: stay on Spring Boot 3.3.4 until M1, then Spring Boot 4.x with Spring AI 2.0.
- D2 Vector store (UC-05): Postgres with pgvector.
- D3 Providers: Anthropic first, then a local model through Ollama. A second cloud provider is optional.
- D4 Order: M0 hygiene, M0b Docker, M1 Spring AI port, then use cases.
