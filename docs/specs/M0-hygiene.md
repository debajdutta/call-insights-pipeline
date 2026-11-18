# M0: Hygiene

**Branch:** `m0-hygiene`   **Depends on:** nothing   **Baseline:** commit `bd7286d` on `main`

## Goal
Make the repo clean, public-safe and buildable from a clean clone, without changing behaviour.

## Scope

1. **Baseline tag.** The human tags `bd7286d` as `v0.1-pipeline`. The dev agent only prepares the command and does not run it.
2. **Public-docs cleanup.** Remove employer, vendor and target-company names from `docs/README.md`, `docs/SPEC.md` and every other tracked file. Keep the meaning ("entirely synthetic, no real product code or data") in neutral words. Search terms: `[removed]`, `[removed]`, `[removed]`, `[removed]`, `[removed]`, case-insensitive. Git history is not rewritten.
3. **Root README and ROADMAP.** Add the root `README.md` and `ROADMAP.md` provided with this spec. Correct anything in them that contradicts the code. Make `docs/README.md` the detailed guide and keep its links valid.
4. **Root POM.** Add a root `pom.xml` (packaging `pom`) with the six Java modules, shared properties and `dependencyManagement`. Keep Spring Boot at 3.3.4 and Java at 21. No version upgrades in this milestone.
5. **Common module.** Add `callinsights-common` containing the event classes now copied into each service. Replace the copies with the shared classes. Keep package names for events consistent and JSON field names unchanged.
6. **CI.** Add `.github/workflows/ci.yml`: on push and pull request, JDK 21, `mvn -q verify`, and the frontend build. No secrets and no live LLM calls. Leave `.github/modernize/` untouched.
7. **Ignore rules.** Add `.claude/settings.local.json` to `.gitignore`. Confirm `.env`, `logs/` and `media-store/` stay ignored.
8. **Tests.** Add at least one unit test for `summary-service` (none exist), using a stub `SummaryProvider`.

## Out of scope
Docker images (M0b), Spring AI or Boot upgrades (M1), new features, renaming packages.

## Acceptance criteria

| ID | Criterion | How verified |
| --- | --- | --- |
| AC-M0-1 | A case-insensitive search of all tracked files for the listed terms returns nothing | `git grep -i -E "[removed]|[removed]|[removed]|[removed]|[removed]"` |
| AC-M0-2 | `mvn -q verify` passes at the repo root on a fresh clone with no `target/` directories | Fresh clone, run command |
| AC-M0-3 | No event class is duplicated across services; each service depends on `callinsights-common` | Inspect poms and sources |
| AC-M0-4 | JSON field names and topic names of all events are unchanged | Compare event classes with `docs/SPEC.md` and a serialisation test |
| AC-M0-5 | `scripts/run-test.sh` still produces the transcript, summary and evaluation artifacts for one call, with the same file names as before | Run with infra up and the human's key |
| AC-M0-6 | CI workflow runs on a pull request and passes without any secret | Workflow run |
| AC-M0-7 | `summary-service` has at least one passing unit test | `mvn -f summary-service/pom.xml test` |
| AC-M0-8 | `.claude/settings.local.json` is ignored; `git status` is clean after a build | `git status` |
| AC-M0-9 | Root `README.md` quick start works as written | Follow it on a clean clone |

## Human gates
Approve the plan; review the event-class move carefully; run AC-M0-5 with a real key; merge; tag `v0.1-pipeline`.
