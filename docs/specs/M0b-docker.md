# M0b: Docker

**Branch:** `m0b-docker`   **Depends on:** M0 merged   **Baseline:** commit `66b9a06` on `main`

## Goal
Start the whole system (infra, six services, frontend) with one command, built from a clean clone, without changing behaviour or event contracts. Close the M0 leftovers: duplicated helper classes and stale module names in `docs/SPEC.md`.

## Decisions (confirmed by the human)

| # | Decision |
| --- | --- |
| 1 | Media and artifacts live in the named volume `media-data`, mounted at `/data/media-store`. |
| 2 | The root `docker-compose.yml` sets `name: callinsights-docker`. The infra-only stack (started from `infra/`) and the Docker stack cannot run at the same time, and their Mongo volumes are separate (see Scope 4). |
| 3 | `scripts/docker-up.sh` is the key-warning wrapper (Scope 5). |
| 4 | One parameterised root `Dockerfile` (`ARG SERVICE`) builds all six services. The frontend keeps its own `frontend/Dockerfile`. |
| 5 | The Mongo document classes stay duplicated in `catalog-service` and `gateway-service`, pinned by the mapping tests. A shared document module would need its own spec and ADR later. |
| 6 | The frontend calls the gateway directly at `localhost:8086` (CORS). The nginx `/api` proxy stays under "Found, not done". |

## Scope

1. **Service Dockerfile.** Add one root `Dockerfile` with `ARG SERVICE`. All six services (`call-generator`, `transcription-service`, `summary-service`, `evaluation-service`, `catalog-service`, `gateway-service`) build from it through `build: { context: ., dockerfile: Dockerfile, args: { SERVICE: <module> } }` in compose. There are no per-directory service Dockerfiles. Layout:
   - Build stage `maven:3.9-eclipse-temurin-21`. Build context is the **repo root**, because a module cannot build without the root `pom.xml` and `callinsights-common`. Copy the root `pom.xml` and **all seven module `pom.xml` files** first (the reactor needs every declared module's pom to resolve), then the sources of `callinsights-common` and of `${SERVICE}` only, then run `mvn -B -q -DskipTests -pl ${SERVICE} -am package`. This is the same reactor command as `scripts/services.sh`. A BuildKit cache mount on `/root/.m2` is allowed. A plain `dependency:go-offline` layer is not required, because it fails on the unpublished `callinsights-common` snapshot.
   - Runtime stage `eclipse-temurin:21-jre`, workdir `/app`. `ARG SERVICE` is declared again in this stage (build args do not cross stages). Copy `${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar` from the build stage to `/app/app.jar`, run as a non-root user, `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
   - **No `EXPOSE`.** It is metadata only, compose publishes each port explicitly, and an `ARG PORT` would be a second place to keep in sync with each `application.yml`.
   - A build without `SERVICE` fails fast (for example `RUN test -n "$SERVICE"`), rather than building the whole reactor.
   - Unit tests are not run in the image build. `mvn -q verify` and CI stay the test gate.
   - No `ENV` or `ARG` carries a secret, and no `application.properties` is copied.
2. **Frontend Dockerfile.** Add `frontend/Dockerfile` (context `frontend/`): build stage `node:24-alpine` with `npm ci && npm run build`, runtime stage `nginx` (stable alpine tag, pinned) serving `dist/frontend/browser`. Add `frontend/nginx.conf` with an SPA fallback (`try_files $uri $uri/ /index.html`). Add `frontend/.dockerignore` (`node_modules`, `dist`, `.angular`).
   - **How the frontend reaches the gateway (decided).** `angular.json` has no `fileReplacements`, so the production build uses `src/environments/environment.ts`, which sets `apiBaseUrl` to `http://localhost:8086/api`. The browser therefore calls the gateway directly on host port 8086. The gateway's CORS default (`GATEWAY_CORS_ALLOWED_ORIGIN`, default `http://localhost:4200`) already allows that origin if the frontend is published on host port 4200. No frontend source change and no nginx proxy in M0b. Switching to a relative `/api` with an nginx proxy is recorded under "Found, not done".
3. **Root `.dockerignore`.** Exclude `.git`, `**/target`, `frontend/node_modules`, `frontend/dist`, `logs`, `media-store`, `.env`, `**/application.properties`, `.claude`, `.idea`, `.vscode`. This keeps local secrets and data out of every build context.
4. **One compose file.** Add a root `docker-compose.yml` with `name: callinsights-docker` that uses `include: - infra/docker-compose.yml` and defines the six services and the frontend. `infra/docker-compose.yml` is **not edited**.
   - **Why include and not extend.** `scripts/start-all.sh`, `scripts/stop-all.sh` and `scripts/status.sh` run `docker compose` inside `infra/` and expect infra-only behaviour. Adding the services to that file would make `start-all.sh` start containerised services that then collide on ports 8081 to 8086 with the jar processes it launches. A root file that includes the infra file keeps the script workflow unchanged and gives one command from the root. `include` needs Compose v2.20 or later. The README states this.
   - **Project name and coexistence.** The infra-only stack runs as Compose project `infra` (the directory name; the infra file sets no `name:`). The Docker stack runs as project `callinsights-docker`. `infra/docker-compose.yml` sets fixed `container_name`s (`call-insights-kafka`, `call-insights-kafka-ui`, `call-insights-mongo`, `call-insights-mongo-express`), and the included services keep them, so **the two stacks cannot run at the same time**: the second `up` fails on a container-name conflict (and on host ports 9092, 27017, 8090, 8091). Their Mongo volumes are **separate**: the infra-only stack uses `infra_mongo-data`, the Docker stack uses `callinsights-docker_mongo-data` (both from the `mongo-data` volume key in `infra/docker-compose.yml`). The Docker stack therefore starts with an empty catalog. This is intended and documented.
   - **Connection overrides** use Spring relaxed binding through environment variables only, with no new profile files:

     | Setting | Variable | Value | Services |
     | --- | --- | --- | --- |
     | Kafka | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` (the existing internal `PLAINTEXT` listener) | all six |
     | MongoDB | `SPRING_DATA_MONGODB_URI` | `mongodb://mongo:27017/call-insights` | catalog-service, gateway-service |
     | Media and artifacts | `CALLGENERATOR_MEDIA_BASEDIRECTORY`, `TRANSCRIPTIONSERVICE_MEDIA_BASEDIRECTORY`, `SUMMARYSERVICE_MEDIA_BASEDIRECTORY`, `EVALUATIONSERVICE_MEDIA_BASEDIRECTORY` | `/data/media-store` | the four pipeline services |

   - **Shared volume (decided).** Events and the Mongo catalog carry **absolute** file paths (`mediaPath`, `transcriptPath`, version `path`). The gateway reads artifact files from those paths and deletes them. So `call-generator`, `transcription-service`, `summary-service`, `evaluation-service` and `gateway-service` all mount the named volume `media-data` at the same path, `/data/media-store`, read-write. `catalog-service` only stores paths and does not mount it. A named volume means the containers never write into the repo or the host home directory.
   - **Logs.** Each service keeps writing `logs/<service>.log` relative to `/app`, inside the container. That file is not persisted. `docker compose logs` is the log view for the Docker path.
   - **Ports published:** 8081 to 8086 (services), 4200 to container port 80 (frontend). Infra ports stay as in `infra/docker-compose.yml`.
   - **Ordering and health.** Every service declares `depends_on` on `kafka` with `condition: service_healthy`. `catalog-service` and `gateway-service` also depend on `mongo` with `condition: service_healthy`. Each service has a healthcheck on `http://localhost:<port>/actuator/health` (actuator is already a dependency and `health` is already exposed). The dev agent confirms that the healthcheck tool (for example `curl`) is present in `eclipse-temurin:21-jre`. If it is not, the agent uses a check that needs no extra package rather than installing one. The frontend depends on `gateway-service: service_healthy` and has its own healthcheck with a tool already in `nginx:alpine`, for example `wget -q --spider http://localhost/`. No package installs. Use `restart: unless-stopped` for the services.
   - **Other environment.** `JWT_SECRET`, `SEED_USER_USERNAME`, `SEED_USER_PASSWORD` and `GATEWAY_CORS_ALLOWED_ORIGIN` are listed value-less (pass-through), like the key. If they are unset in the shell they stay unset, and the existing `application.yml` defaults apply.
5. **API key handling.** `transcription-service` and `summary-service` list `ANTHROPIC_API_KEY` under `environment:` **without a value** (Compose pass-through from the shell that runs `docker compose`). No `.env` file, no `env_file:`, no `${ANTHROPIC_API_KEY:?}` hard failure, no key in any Dockerfile, image layer or tracked file. This matches `scripts/start-all.sh`: a missing key prints a warning but the stack still starts, and the LLM calls fail at use. (Both providers build the client with `AnthropicOkHttpClient.fromEnv()`. The dev agent confirms, with a container started without the key, that the service starts and does not restart-loop.) Compose cannot print a warning itself, so add `scripts/docker-up.sh` (decided). It reuses the exact `key_warning` text from `start-all.sh` (with the restart hint changed to `docker compose up -d transcription-service summary-service`), then runs `docker compose up -d --build` from the repo root and prints the warning again at the end. No existing script is modified.
6. **README quick start.** Update the root `README.md` quick start so the primary path is:
   ```bash
   export ANTHROPIC_API_KEY="..."   # per terminal session; never commit it
   scripts/docker-up.sh             # or: docker compose up -d --build
   ```
   Then: UI at `http://localhost:4200`, gateway at `http://localhost:8086`, stop with `docker compose down` (add `-v` to wipe Mongo and media volumes). Link to `docs/DOCKER.md` for details. Keep the `scripts/start-all.sh` flow as the "run services from source" alternative. Change the Status table row "Run everything with one command (Docker)" to Built only after AC-M0b-5 passes. Add Compose v2.20+ to Prerequisites.
7. **`docs/DOCKER.md`.** Add a Docker guide, linked from the root README quick start. Contents:
   - The two ways to run: scripts (`scripts/start-all.sh`, services from source as host jars, infra-only stack from `infra/`) and Docker (`scripts/docker-up.sh` or `docker compose up -d --build` from the root), and when to use each (scripts for day-to-day development and debugging in an IDE; Docker for a clean, one-command run from a fresh clone). The two cannot run at the same time; stop one before starting the other.
   - The Compose network and service names, with the env-override table from Scope 4.
   - The volumes (`media-data` at `/data/media-store`, `mongo-data`), and why the media path must be identical in every container: events and the catalog carry absolute paths, and the gateway reads and deletes files by those paths.
   - The healthchecks and start order (Scope 4, "Ordering and health").
   - The API key pass-through (Scope 5), and the warning that `docker compose config` prints the resolved key if it is exported.
   - Resetting with `docker compose down -v` (wipes `callinsights-docker_mongo-data` and `callinsights-docker_media-data`; the infra-only stack's `infra_mongo-data` is untouched).
   - This diagram:

     ```mermaid
     flowchart TB
       browser["Browser on the Mac"]
       shell["Your shell: export ANTHROPIC_API_KEY"]
       subgraph net["Compose network (project callinsights-docker)"]
         fe["frontend: nginx :80, host 4200"]
         gw["gateway-service :8086"]
         cat["catalog-service :8085"]
         pipe["call-generator, transcription-service, summary-service, evaluation-service (8081 to 8084)"]
         kafka["kafka:29092"]
         mongo["mongo:27017"]
         media[("volume media-data at /data/media-store")]
         mdata[("volume mongo-data")]
       end
       browser -->|":4200"| fe
       browser -->|":8086, CORS"| gw
       shell -.->|"key, pass-through to transcription-service and summary-service only"| pipe
       pipe --> kafka
       cat --> kafka
       gw --> kafka
       cat --> mongo
       gw --> mongo
       pipe --> media
       gw --> media
       mongo --> mdata
     ```

     kafka-ui (8090) and mongo-express (8091) also start, from the infra file.
8. **M0 leftovers: duplicated classes.** Inventory at baseline `66b9a06` (paths under `<module>/src/main/java/com/callinsights/`):

   | Class | Copies | Identical? | Decision |
   | --- | --- | --- | --- |
   | `TranscriptArtifact` (record) | `transcriptionservice/model/`, `summaryservice/model/`, `evaluationservice/model/` | Yes. Same seven components in the same order (`callId`, `agentId`, `templateId`, `version`, `modelUsed`, `generatedAt`, `transcriptText`). The summary copy only adds a Javadoc. | **Dedupe.** Single source `callinsights-common` as `com.callinsights.common.artifact.TranscriptArtifact`. Delete the three copies. It is a file format (`transcript_v{n}.json`), not a Kafka event, so no event contract changes. JSON field names stay the same. |
   | `ArtifactVersionResolver` | `transcriptionservice/service/`, `summaryservice/service/`, `evaluationservice/service/` | Summary and evaluation: identical code (they differ only in package and Javadoc). Transcription: a subset. It has only `nextVersion` and no `latestArtifactPath`, with the same result for every input (max `_v<n>` + 1, or 1 when the directory or files are missing). | **Dedupe.** Single source `callinsights-common` as `com.callinsights.common.artifact.ArtifactVersionResolver`, using the summary/evaluation superset. It becomes a plain class without `@Component`, because `callinsights-common` has no Spring dependency and must not gain one. Each of the three services registers it with a `@Bean` in its existing `config` package. Delete the three copies. |
   | `CallDocument` (`calls`) | `catalogservice/document/`, `gatewayservice/document/` | Same collection, same fields. The catalog copy adds the no-arg and all-args constructors. | **Keep (decided).** Reason: a shared copy needs `@Document`/`@Id` from Spring Data MongoDB in `callinsights-common` (a new dependency for a module that four non-Mongo services use, with a risk of triggering Mongo auto-configuration in them), or a new module. Neither is justified in M0b; a shared document module would need its own spec and ADR. Mapping is identical today; a test pins it (see Tests). |
   | `AuditLogDocument` (`audit_log`) | same two modules | Same collection and fields. The gateway copy is read-only (no setters, no constructors). | **Keep**, same reason. |
   | `ArtifactCatalogDocument` (`artifacts`) | same two modules | Same collection, fields and `buildId`. The catalog copy adds an explicit no-arg constructor and Javadoc. | **Keep**, same reason. |
   | `ArtifactVersionEntry` (embedded) | same two modules | Same fields. The catalog copy adds constructors. | **Keep**, same reason. |
   | `ArtifactCatalogRepository`, `CallRepository`, `AuditLogRepository` | same two modules | No. The gateway adds `findByCallId` and `findByCallIdOrderByTimestampDesc`. | **Keep.** These are service-specific query interfaces. |
   | `UserDocument` | `gatewayservice/document/` only | n/a | Not duplicated. |

   The `callinsights-common` pom `<description>` is updated to mention the artifact classes. No dependency is added to any pom.
9. **SPEC.md section 9.** Make the repository layout match the root `pom.xml` modules and the tree. Edit section 9 only:

   | Stale in section 9 | Correct |
   | --- | --- |
   | `gateway/` | `gateway-service/` |
   | `metadata-consumer/` | `catalog-service/` |
   | `model-registry/  # shared config module` | Remove. No such module exists; the model registry is config only (section 4.5). |
   | (missing) | `callinsights-common/  # shared event records and artifact helpers` |
   | (missing) | `pom.xml  # root Maven reactor` |
   | (missing) | `Dockerfile  # all six services, ARG SERVICE` |
   | (missing) | `docker-compose.yml  # whole system; includes infra/docker-compose.yml` |
   | `infra/docker-compose.yml  # Kafka, MongoDB (+ own services in milestone 2)` | `# Kafka, MongoDB, Kafka UI, Mongo Express` |
   | `docs/` lists only `SPEC.md`, `TASKS.md`, `README.md` | Add `API_TESTING.md`, `DOCKER.md` and `specs/` |

   The service `Dockerfile` appears once, at the root, not in each service directory. `frontend/` notes `Dockerfile, nginx.conf`.

## Files to add or change

| File | Change |
| --- | --- |
| `Dockerfile` (root, `ARG SERVICE`, used by all six services) | Add |
| `frontend/Dockerfile`, `frontend/nginx.conf`, `frontend/.dockerignore` | Add |
| `.dockerignore` (root) | Add |
| `docker-compose.yml` (root) | Add |
| `scripts/docker-up.sh` | Add (executable) |
| `docs/DOCKER.md` | Add |
| `callinsights-common/src/main/java/com/callinsights/common/artifact/TranscriptArtifact.java`, `.../ArtifactVersionResolver.java` | Add |
| `callinsights-common/pom.xml` | Description text only |
| The three `TranscriptArtifact.java` and three `ArtifactVersionResolver.java` copies | Delete |
| `TranscriptionOrchestrator`, `SummaryOrchestrator`, `EvaluationOrchestrator`, `SummaryOrchestratorTest` | Imports only |
| `transcription-service`, `summary-service`, `evaluation-service` `config/` | Add a `@Bean ArtifactVersionResolver` (in an existing or new `@Configuration` class) |
| `transcription-service/src/test/.../ArtifactVersionResolverTest.java` | Move to `callinsights-common` and extend |
| `README.md` | Quick start (with a link to `docs/DOCKER.md`), prerequisites, status row |
| `docs/SPEC.md` | Section 9 only |
| `ROADMAP.md` | M0b status, only after the human gates pass |

## Tests

Write these tests first where practical.
- `callinsights-common`: `ArtifactVersionResolverTest`, moved from transcription-service and kept unchanged. Add cases for `latestArtifactPath`: empty directory, missing directory, highest of `_v1`, `_v2`, `_v10`, and other base names ignored.
- `callinsights-common`: `TranscriptArtifactJsonTest`. It serialises a synthetic `TranscriptArtifact` and asserts the exact seven JSON field names. It also deserialises a synthetic `transcript_v1.json` fixture written in today's format.
- Existing tests (`SummaryOrchestratorTest`, `EventJsonContractTest`, catalog and gateway tests) pass unchanged except for imports.
- `catalog-service` and `gateway-service`: one small mapping test each that pins the collection name and field names of the four kept document classes (for example by reading the `@Document` annotation and declared fields). If the two copies drift, one of the tests fails.
- No automated container test is added to CI. The compose checks are the manual ACs below.

## Out of scope / will not do
Spring Boot or any framework upgrade, Spring AI (M1). New Maven dependencies or modules. Event contract changes (topic names, event fields), and changes to the `callinsights-common` event package. Merging the Mongo document classes or adding a shared document module. Changing `infra/docker-compose.yml` or existing scripts. Frontend source changes. An nginx `/api` proxy. Kubernetes, registries, image publishing, CI image builds. Neo4j or Postgres containers (the UC-05 store, decision D2, arrives with the knowledge-service, not in M0b). Any `.env` or `env_file`. Edits to `.github/modernize/`, `logs/`, `media-store/`, `.env`, `application.properties`, `.claude/settings.local.json`. No push, merge or tag.

## Risks

| Risk | Mitigation |
| --- | --- |
| Port clash with `scripts/start-all.sh` (jar processes on 8081 to 8086, `ng serve` on 4200), and container-name clash with an infra-only stack started from `infra/`: project `infra` and project `callinsights-docker` both use the fixed `container_name`s from `infra/docker-compose.yml`, so the second stack fails to start | The two stacks cannot run at the same time, by design. README and `docs/DOCKER.md` state: run `scripts/stop-all.sh` before the Docker path and `docker compose down` before the script path. |
| Mongo data differs between the two paths (`infra_mongo-data` vs `callinsights-docker_mongo-data`), so the Docker path starts with an empty catalog | Decided: separate volumes, set by `name: callinsights-docker`. Documented in `docs/DOCKER.md`. `docker compose down -v` from the root never touches `infra_mongo-data`. |
| Absolute paths recorded by a host run (`/Users/...`) are unreadable inside containers and the reverse | Separate volumes per path (`media-data` is not the host media directory). Do not mix data between the paths. Documented. |
| Non-root runtime user cannot write a fresh named volume | Create `/data/media-store` owned by the runtime user in the image, so the named volume `media-data` inherits the ownership. Checked by AC-M0b-6. |
| The single root `Dockerfile` copies too much, so a change in one service invalidates every image's cache, or a module pom is missing and the reactor fails | Copy all module poms (the reactor needs them) but only `callinsights-common` and `${SERVICE}` sources. Fail fast when `SERVICE` is empty. Checked by AC-M0b-4. |
| Healthcheck tool missing in the JRE image | The dev agent checks this and uses a check that needs no extra package. No package installs. The frontend uses `wget`, already in `nginx:alpine`. |
| Memory: six JVMs plus Kafka and Mongo | README notes the Docker Desktop memory needed. Optional `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75` in compose. |
| `docker compose config` prints the resolved key if it is exported | ACs that run `config` do so with the key unset. `docs/DOCKER.md` warns about it. Never paste `config` output into issues or commits. |
| `include` not supported on older Compose | Prerequisite v2.20+ in README. |

## Acceptance criteria

| ID | Criterion | How verified |
| --- | --- | --- |
| AC-M0b-1 | `mvn -q verify` passes at the repo root on a fresh clone | Fresh clone, run command |
| AC-M0b-2 | Frontend still builds | `(cd frontend && npm ci && npm run build)` |
| AC-M0b-3 | The root compose file is valid and includes infra | With `ANTHROPIC_API_KEY` unset: `docker compose config -q` exits 0; `docker compose config --services` lists `kafka`, `mongo`, `kafka-ui`, `mongo-express`, the six services and `frontend`; the project name is `callinsights-docker` |
| AC-M0b-4 | All seven images build from a clean clone, from two Dockerfiles | `docker compose build` exits 0; `git ls-files '*Dockerfile'` lists exactly `Dockerfile` and `frontend/Dockerfile` |
| AC-M0b-5 | One command brings the system up healthy | `docker compose up -d --build` (or `scripts/docker-up.sh`); within 5 minutes `docker compose ps --format '{{.Service}} {{.Health}}'` shows `healthy` for `kafka`, `mongo`, the six services and `frontend` |
| AC-M0b-6 | Services and UI are reachable from the host | `curl -s localhost:<port>/actuator/health` returns `"status":"UP"` for 8081 to 8086; `curl -s -o /dev/null -w '%{http_code}' localhost:4200/` and `localhost:4200/calls` return `200`; `POST localhost:8086/api/auth/login` with the default seed user returns a token; `POST localhost:8081/api/calls/generate` returns a `callId` and its media file exists in the shared volume (`docker compose exec call-generator ls /data/media-store/<callId>`) |
| AC-M0b-7 | A missing key behaves like `start-all.sh` | With the key unset: `scripts/docker-up.sh` prints the key warning, the stack still starts, `transcription-service` and `summary-service` are `healthy` with restart count 0 after 2 minutes (`docker inspect -f '{{.RestartCount}}'`) |
| AC-M0b-8 | The key is never stored | `test ! -e .env`; `git grep -n -I -E 'sk-ant-[A-Za-z0-9]'` returns nothing; `git grep -n ANTHROPIC_API_KEY -- docker-compose.yml Dockerfile frontend/Dockerfile` shows only value-less entries in the compose file; `docker history --no-trunc` and `docker image inspect` of every built image contain no `ANTHROPIC_API_KEY` |
| AC-M0b-9 | The full pipeline works in containers with a real key | Human run: with the key exported, generate a call; the shared volume holds `transcript_v1.json`, `summary_v1.json` and `evaluation_v1.json` for that call, and the UI shows the three artifacts after login |
| AC-M0b-10 | The script workflow is unaffected | `git diff --stat 66b9a06 -- infra/ scripts/` lists only the new `scripts/docker-up.sh`; with the Docker stack down, `scripts/start-all.sh` then `scripts/status.sh` behave as before |
| AC-M0b-11 | Each deduped class has a single definition | `git grep -n -E 'record TranscriptArtifact\b'` and `git grep -n -E 'class ArtifactVersionResolver\b'` each return exactly one line, under `callinsights-common/` |
| AC-M0b-12 | The transcript JSON format is unchanged | `TranscriptArtifactJsonTest` passes (`mvn -pl callinsights-common test`) |
| AC-M0b-13 | Kept Mongo documents are unchanged and pinned | `git diff 66b9a06 -- '*/document/*.java'` is empty; the two mapping tests pass |
| AC-M0b-14 | Event contracts are unchanged | `git diff 66b9a06 -- callinsights-common/src/main/java/com/callinsights/common/event/` is empty; `EventJsonContractTest` passes; topic names in every `application.yml` are unchanged (`git diff 66b9a06 -- '*/application.yml'` is empty) |
| AC-M0b-15 | SPEC.md section 9 matches the pom modules | Every `<module>` in the root `pom.xml` appears in section 9, and section 9 names no Java module directory that is missing from the pom (`gateway/`, `metadata-consumer/` and `model-registry/` are absent) |
| AC-M0b-16 | Root `README.md` quick start works as written and links to `docs/DOCKER.md` | Follow it on a clean clone; the link resolves |
| AC-M0b-17 | No banned terms | `git grep -i -f ~/banned-terms.txt` returns nothing |
| AC-M0b-18 | Running the stack leaves the repo clean | After AC-M0b-5 and `docker compose down`: `git status --porcelain` is empty |
| AC-M0b-19 | The `docs/DOCKER.md` diagram renders and covers every service | The Mermaid diagram renders on GitHub (branch view of `docs/DOCKER.md`); every name from `docker compose config --services` (run with the key unset) appears in the diagram or in the kafka-ui/mongo-express line below it |

## Found, not done
- `frontend/src/environments/environment.production.ts` (`apiBaseUrl: '/api'`) is never used, because `angular.json` has no `fileReplacements`. A follow-up could wire it up and add an nginx `/api` proxy to the gateway, which removes the cross-origin call. That changes the `npm run build` output, so it is left out of M0b (decision 6).
- `scripts/run-test.sh` and `scripts/status.sh` assume host jars, `logs/` and `$HOME/call-insights-pipeline/media-store`. They do not report correctly against the Docker stack. A Docker-aware variant is a follow-up.
- `docs/SPEC.md` still says "Metadata Consumer" in sections 4.1 to 4.3 and 6, and section 8 step 2 describes this milestone as future. Only section 9 is in scope.
- `docs/README.md` (detailed guide) and `docs/TASKS.md` describe the pre-Docker flow. The README update is limited to the root `README.md` quick start and the link to `docs/DOCKER.md`.
- `infra/docker-compose.yml` uses `provectuslabs/kafka-ui:latest` and `mongo-express:latest` (unpinned).
- `ArtifactVersionResolver` still reuses version numbers after a delete (its own Javadoc notes this against SPEC.md section 4.4). Moving the class does not change that.
- A shared Mongo document module (decision 5) would need its own spec and ADR.

## Human gates
Approve the plan. Review the class moves, the root `Dockerfile` and the compose file. Run AC-M0b-9 with a real key. Check AC-M0b-19 on GitHub. Merge. Tag at your discretion (SPEC.md section 8 suggests a git tag for this milestone).
