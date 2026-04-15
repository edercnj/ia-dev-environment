# Story Planning Report -- story-0039-0004

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0004 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story delivers parallelization of VALIDATE-DEEP phase in `x-release` skill, targeting ≥40% wall-clock reduction. Scope covers a new `ParallelCheckExecutor` in application layer plus shell-level `&`/`wait` orchestration in SKILL.md. All existing `VALIDATE_*` error codes are preserved (RULE-005). Story has 4 pre-scoped tasks in §8; consolidation expanded to 9 tasks to include RED/GREEN TDD pairs, security validation for `--max-parallel`, quality gate for preserved codes, and PO acceptance-criteria verification.

## Architecture Assessment

- **Affected layers:** application (new `ParallelCheckExecutor`, `ResultAggregator`), cross-cutting (SKILL.md bash).
- **New components:** `dev.iadev.release.validate.ParallelCheckExecutor`, `dev.iadev.release.validate.ResultAggregator`.
- **Modified components:** `x-release/SKILL.md` (VALIDATE-DEEP phase block).
- **Dependency direction validated:** application depends only on JDK stdlib (`CompletableFuture`, `ExecutorService`); no domain intrusion.
- **Implementation order:** tests (RED) → Executor (GREEN) → Aggregator (GREEN) → SKILL.md (GREEN) → benchmark/smoke (VERIFY).
- **Integration points:** build step (`mvn verify`) runs first and gates artifact availability for 7 parallel checks.

## Test Strategy Summary

- **Acceptance Tests (AT):** 5 scenarios from story §7 — build-fail, happy path, multi-failure, `--max-parallel=1`, benchmark reduction.
- **Unit Tests (UT) in TPP order:**
  - Level 1 (nil): empty check list → returns empty result set.
  - Level 2 (constant): single check PASS → single PASS result.
  - Level 3 (scalar): one check FAIL + one PASS → aggregator sorts FAIL first.
  - Level 4 (collection): 7 checks in pool of 4 → all execute, all captured.
  - Level 5 (conditional): `--max-parallel=1` serializes execution.
  - Level 6 (iteration): multiple concurrent failures → first-alphabetic code returned.
- **Integration/Smoke:** `ValidateDeepParallelSmokeTest` forces golden check failure end-to-end.
- **Performance:** `ValidateDeepBenchmarkTest` asserts ≥ 40% wall-clock reduction.
- **Estimated coverage:** ≥ 95% line / ≥ 90% branch (per epic DoD).

## Security Assessment Summary

- **OWASP mapping:** A03 Injection (via `--max-parallel` argument — integer validation required), A05 Security Misconfiguration (bounded pool size 1..16 prevents DoS via thread explosion).
- **Input validation:** `--max-parallel N` parsed as `Integer`, rejected if outside `[1, 16]`, rejected if non-numeric. Error code: `ABORT_INVALID_PARALLELISM`.
- **Auth/Secrets/PII:** N/A — release tooling runs locally in developer / CI context.
- **Dependency security:** no new libraries; reuses JDK `java.util.concurrent`.
- **Error handling:** exit codes `VALIDATE_*` preserved; no internal paths or stack traces leaked in output.
- **Risk level:** LOW.

## Implementation Approach

- **Chosen approach:** JDK `ExecutorService` with `CompletableFuture.allOf` for the Java-level executor; bash `&` + `wait $PID` for the SKILL.md shell-level orchestration (matches existing `x-release` SKILL.md style).
- **Quality gates:** method length ≤ 25 lines, class length ≤ 250 lines, no wildcard imports, coverage thresholds enforced by `jacoco:check`, cross-file consistency verified against existing `x-release` validators.
- **Coding standards compliance:** Rule 03 (hard limits), Rule 05 (coverage), Rule 06 (security baseline — input validation), Rule 07 (operations baseline — structured logging `[VALIDATE-DEEP] check-name: Xs`).
- **Alternative considered:** Java parallel streams (rejected — harder to capture per-task exit codes and durations).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 9 |
| Architecture tasks | 2 (TASK-002, TASK-003) |
| Test tasks | 4 (TASK-001, TASK-005, TASK-006, TASK-009) |
| Security tasks | 1 (TASK-007) |
| Quality gate tasks | 1 (TASK-008) |
| Validation tasks | 1 (TASK-009) |
| Merged tasks | 2 (TASK-002 merged Architect+QA; TASK-004 merged Architect+TL) |
| Augmented tasks | 1 (TASK-002 augmented with SEC input validation) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|--------------|----------|------------|------------|
| Benchmark flakiness under CI jitter | QA | MEDIUM | MEDIUM | Median of 5 runs; skip on constrained CPU env |
| Shell `&`/`wait` Windows portability | Architect | LOW | LOW | Document POSIX shell requirement (already assumed by `x-release`) |
| Thread explosion via unbounded `--max-parallel` | Security | MEDIUM | LOW | Hard bound `[1,16]`; reject non-integer with `ABORT_INVALID_PARALLELISM` |
| Silent rename of `VALIDATE_*` codes | Tech Lead | HIGH | LOW | Quality gate TASK-008: grep for all 7 original codes; CI blocks on absence |
| Race condition between build artifact readiness and parallel checks | Architect | HIGH | LOW | Build runs sequentially BEFORE parallel dispatch (enforced in aggregator) |

## DoR Status

**READY** — see `dor-story-0039-0004.md` for full checklist.
