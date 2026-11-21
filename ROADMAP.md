# CallInsights Roadmap

What is built and what is planned. All data is synthetic. A row is marked **Built** only after it has run and its acceptance criteria are checked.

## Foundation

| Milestone | What | Status |
| --- | --- | --- |
| v0.1 | Kafka pipeline, catalog, gateway with JWT, Angular UI | Built (tag `v0.1-pipeline`) |
| M0 | Hygiene: root README, root POM, shared module, CI build, public-docs cleanup | Built |
| M0b | Dockerfiles and one-command startup (`scripts/docker-up.sh`), see `docs/DOCKER.md` | Built |
| M1 | Spring Boot 4 and Spring AI 2.0 port behind the existing provider interfaces | Planned |

## Use cases

| # | Feature | Status |
| --- | --- | --- |
| UC-01 | LLM compliance evaluation with evidence, compared with the rule baseline | Planned |
| UC-02 | PII redaction advisor with a labelled test set | Planned |
| UC-03 | Summary and action items with prompt versioning | Partly built (summary exists, on the Anthropic SDK) |
| UC-04 | Streaming summary over SSE | Planned |
| UC-05 | Policy RAG with citations (Neo4j, LangChain4j) | Planned |
| UC-06 | Assistant with tool calling and approval | Planned |
| UC-07 | MCP server verified with a real client | Planned |
| UC-08 | Model routing, fallback and a local-model policy gateway | Planned |

Each item ships with acceptance criteria in `docs/specs/`, a test or evaluation result, and a short README section.
