# UC-01: LLM compliance evaluation

**Branch:** `uc-01-llm-evaluation`   **Depends on:** M0, M0b and M1 merged (Spring AI in place)

## Goal
Add an LLM-based evaluator beside the rule-based `KeywordScorer`, so each call gets a compliance score with cited evidence, and the two methods can be compared on a labelled set.

## Requirements

| ID | Requirement |
| --- | --- |
| FR-01 | Extract a scorer interface from `KeywordScorer`. Add `LlmScorer` implementing it through Spring AI `ChatClient`. |
| FR-02 | Mode is configured by `evaluation-service.scoring.mode`: `keyword`, `llm` or `both`. Default is `keyword`. |
| FR-03 | `LlmScorer` returns typed output (Spring AI `entity()`): `passed`, `score` (0.0 to 1.0), per-criterion results, and for each criterion the transcript quote that supports it. |
| FR-04 | The prompt is a versioned resource (`prompts/evaluation-v1`). The artifact records scorer id, prompt version and model name. Temperature is 0. |
| FR-05 | In `both` mode the keyword result stays in the artifact as the baseline next to the LLM result. Existing artifact fields stay backward compatible. |
| FR-06 | If the LLM call fails or returns invalid output, the service falls back to the keyword result and marks the artifact `degraded: true` with a reason. It never blocks the pipeline. |
| FR-07 | A quote returned by the model must appear in the transcript. A criterion whose quote is not found is marked unsupported. |
| FR-08 | Only synthetic transcripts are sent to the model. |

## Evaluation set
A golden set of at least 20 synthetic transcripts in `evaluation-service/src/test/resources/golden/`, each with an expected pass or fail and the expected failed criteria. Includes clear passes, clear failures and borderline cases. The human approves the labels.

## Acceptance criteria

| ID | Criterion | How verified |
| --- | --- | --- |
| AC-01-1 | In `keyword` mode behaviour and artifacts are identical to before | Existing tests plus a regression test |
| AC-01-2 | In `llm` and `both` modes the artifact contains score, criteria, supporting quotes, scorer id, prompt version, model | Unit test with a stubbed provider (WireMock) |
| AC-01-3 | Every supported criterion's quote is a substring of the transcript | Test on recorded responses |
| AC-01-4 | Provider failure produces a keyword result with `degraded: true`; the pipeline still completes | Failure-injection test |
| AC-01-5 | Golden-set run reports pass or fail agreement and mean absolute score error for both scorers. Initial target: LLM agreement at or above the keyword baseline and at least 85 percent. Tune the target after the first live run, with the human's approval | Eval test tagged `eval`, run on recorded responses in CI |
| AC-01-6 | A live run with a real key on the golden set produces a saved report (`docs/evals/UC-01-<date>.md`) | Manual, `-Pllm-live` profile, by the human |
| AC-01-7 | No key, employer name or real data appears in the diff | `git grep`, diff review |

## Test plan
JUnit with WireMock for provider responses; Testcontainers for Kafka and Mongo in one end-to-end test; eval harness as a JUnit test class tagged `eval`; recorded responses committed under `src/test/resources/recorded/`. Live calls only under the `llm-live` Maven profile, never in CI.

## Out of scope
Redaction (UC-02), streaming (UC-04), RAG (UC-05), model routing (UC-08), UI changes beyond showing the new fields if trivial.

## Human gates
Approve the plan and the prompt text; approve the golden-set labels; review the diff (prompt, data sent to the model, fallback path); run AC-01-6 with a real key exported in the shell; merge.
