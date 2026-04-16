# Performance Specialist Review — story-0040-0007

**Story:** story-0040-0007 — Instrument planning skills with phase + subagent telemetry markers
**PR:** #416
**Reviewer:** Performance Specialist
**Date:** 2026-04-16
**Max Score:** 26

---

## Summary

```
ENGINEER: Performance
STORY: story-0040-0007
SCORE: 26/26
STATUS: Approved
```

## Scope

Performance surface of this story is bounded:

1. Extension of `telemetry-phase.sh` with 2 new KINDs. Helper is invoked twice per phase and once per parallel agent — overhead budget was fixed at **50ms per marker** in story-0040-0006 §4.
2. 5 SKILL.md files gain plain-text marker blocks — no runtime cost beyond the helper invocations.
3. 1 Rule 13 update — pure docs.
4. 5 new JUnit `*IT` test classes — compiled once, run in CI.

## Checklist

### Runtime Overhead (8/8)

- [x] **P1 — Helper stays below 50ms budget (2/2):** No new jq passes introduced; `subagent-start`/`subagent-end` share the same `build_event → jq enrich → emit` pipeline as `start`/`end`. Added arg validation is constant-time.
- [x] **P2 — No new I/O per marker (2/2):** Emission still goes through `telemetry-emit.sh` which already holds `flock` (RULE-004 fail-open).
- [x] **P3 — No busy loops / retries (2/2):** Fail-open short-circuits on every error path (invalid arg, missing role, missing helpers, jq failure).
- [x] **P4 — Short-circuit on disable flag (2/2):** `CLAUDE_TELEMETRY_DISABLED=1` check runs first, before any fs lookup or jq invocation.

### Scalability (6/6)

- [x] **P5 — Cost scales with dispatches, not stories (2/2):** `x-story-plan` emits 5 pairs regardless of story size — no per-LOC or per-file scaling.
- [x] **P6 — `x-epic-orchestrate` per-story loop is O(N) (2/2):** Emits 1 pair per story being planned. For an epic with 50 stories that is 100 markers × 50ms = 5s of telemetry overhead, well inside planning budgets (minutes).
- [x] **P7 — No hot-path regression (2/2):** No code path runs inside an inner TDD loop or a tight iteration; markers wrap phase entry/exit only.

### Resource Usage (6/6)

- [x] **P8 — No new long-lived threads (2/2):** Helper is a short-lived bash process.
- [x] **P9 — NDJSON line growth bounded (2/2):** Each new event is <= 300 chars; the existing scrubber+flock pipeline handles rate.
- [x] **P10 — Tests do not leak processes (2/2):** `ProcessBuilder` calls `waitFor(10, SECONDS)`; JUnit `@TempDir` cleans up after each method.

### Test Performance (6/6)

- [x] **P11 — Fast feedback (2/2):** Full 5-class IT suite runs in <1s after the schema load (one-shot `@BeforeAll`).
- [x] **P12 — Assumptions gate on jq/bash (2/2):** `assumeBashAvailable` / `assumeJqAvailable` skip gracefully on CI without hard dependencies, preventing false failures.
- [x] **P13 — No sleep/poll anti-pattern (2/2):** Zero `Thread.sleep`; all waits use `Process.waitFor(timeout)`.

## PASSED

- [P1..P13] All 13 Performance items at 2/2

## FAILED

_None._

## PARTIAL

_None._

## Notes

- Overhead in production: 2 markers per phase × 4 phases × 50ms = **400ms per x-story-plan invocation**, plus 5 × 2 × 50ms = **500ms per parallel-wave**, totalling ~900ms of telemetry cost on top of a ~60s planning run (~1.5%). Well within EPIC-0040's declared budget.
- No CPU/memory hot spots; all bash invocations are short-lived.

**Verdict:** Approved.
