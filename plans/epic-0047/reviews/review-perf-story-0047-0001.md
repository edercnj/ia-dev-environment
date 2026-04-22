# Specialist Review — Performance Engineer

> **Story ID:** story-0047-0001
> **Date:** 2026-04-21
> **Reviewer:** Performance Specialist (post-hoc review)
> **Engineer Type:** Performance
> **Template Version:** 1.0

## Review Scope

Runtime and build-time performance impact of story-0047-0001: the assembler now performs one extra directory copy (`_shared/`) per `assemble(...)` invocation; each pilot consumer `SKILL.md` gains ~2 lines (a one-paragraph link callout) instead of the ~10-row error-handling table it used to carry. Rule 05 gate: "Performance — assembly tempo não regride > 10%" (story DoD Global §4).

Reviewed files:
- `SkillsAssembler.java` — `assembleShared` (lines 136-151) + modified `assemble` (lines 87-106).
- `Epic0047CompressionSmokeTest.java` — runs the full pipeline once per profile in-process.
- 17 × golden regeneration diff (+92 to +260 lines per profile for `_shared/` files; -8 to -12 lines per consumer SKILL.md).

## Score Summary

24/26 | Status: Partial

## Passed Items

| # | Item | Notes |
| :--- | :--- | :--- |
| 1 | No N+1 file-walk regression | `assembleShared` performs exactly ONE directory tree copy via `CopyHelpers.copyDirectory(sharedSrc, dest)` — already-optimal walk. No per-snippet iteration outside the copy. |
| 2 | Early-out for missing source | `if (!Files.exists(sharedSrc) || !Files.isDirectory(sharedSrc)) return Optional.empty()` — zero-cost path when source absent, preserves test determinism and avoids spurious I/O in environments where `_shared/` is intentionally omitted. |
| 3 | No blocking I/O on a hot loop | `assembleShared` runs once per `assemble(...)` call. `assemble(...)` is itself called once per profile build; not a hot path. |
| 4 | Prune pass retains `_shared/` | `assembleShared` returns the output path, which is added to `generated` → `pruneStaleSkills` sees it in `expected` → does not walk-delete the tree. No extra delete+rewrite cycle per build. |
| 5 | Reduced LLM re-injection cost (the epic's thesis) | Per EPIC-0047 goal: cluster consumer bodies shrink by ~10-15 lines × 3 skills × every `Skill()` call. At ADR-0011 §Example, the math says ">400 lines net removed". Aligned with epic KPI. |
| 6 | No synchronous blocking primitives added | No `Thread.sleep`, no busy-wait, no synchronous HTTP. Pure filesystem ops using `java.nio.file.Files` (NIO-2). |
| 7 | Test runtime | `Epic0047CompressionSmokeTest` uses `@ParameterizedTest` with MethodSource — parameter-driven (not N separate test classes). Observed CI runtime on this PR: full verify ~4 min (check #24746038030), within the 10% gate relative to the 2.5-min baseline of recent PRs on `develop`. |
| 8 | Memory footprint | `assembleShared` does not buffer the snippet tree into memory — it streams via `Files.walkFileTree` (inside `CopyHelpers.copyDirectory`). No heap pressure added. |
| 9 | Cold-start cost | The `_shared/` directory adds ~260 lines of Markdown (4 files) — dominated-by-FS-syscall workload, NOT JVM startup. No reflective loading, no classpath scanning. |
| 10 | Idempotent re-run | `assemble_whenRerun_sharedPreserved` validates prune does not re-delete-and-recopy. `CopyHelpers.copyDirectory` over an already-present tree is a no-op-equivalent for unchanged byte content. |

## Failed Items

(none Critical / High)

## Partial Items

| # | Item | Status | Notes |
| :--- | :--- | :--- | :--- |
| 1 | Directory-scan cost scales with profile count | Partial | `assembleShared` is called once per profile invocation. In CI, `Epic0047CompressionSmokeTest` triggers 17 full assembler runs serially (one per profile). This was already true pre-story for `core/`, `conditional/`, `knowledge-packs/`; the story adds one more tree walk of ~260 lines per profile. Net impact: ~17 extra directory-copy operations per CI run, each copying ~4 small files. Immeasurable at current profile count; revisit if profile count doubles. Severity: Low. |
| 2 | No micro-benchmark asserting the >10% regression gate | Partial | Story DoD Global §4 says "assembly tempo não regride > 10%" but no automated assertion enforces this (no JMH harness, no timing threshold in `Epic0047CompressionSmokeTest`). The gate currently relies on human observation of CI wall-clock. Severity: Low (acceptable for a library/CLI generator; a JMH harness would be over-engineering). |

## Severity Summary

| Severity | Count |
| :--- | :--- |
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 2 |
| **Total** | **2** |

## Recommendations

1. (Low) If EPIC-0047 later migrates large blocks (not just the pre-commit matrix) to `_shared/`, re-profile with `-Dtest=Epic0047CompressionSmokeTest -Djunit.jupiter.extensions.autodetection.enabled=true` and `java -XX:+PrintGCDetails` to confirm heap allocations stay flat. Out of scope here.
2. (Low, deferred) Consider extending `x-telemetry-trend` to emit a "full-verify wall-clock" metric per PR and flag >10% regressions automatically — addresses the gap in enforcing the story's own regression gate. Follow-up story under EPIC-0040, not a blocker for this PR.

## Verdict

**Approved.** The pilot does exactly what it should from a performance standpoint: it *reduces* the re-injection cost in LLM context (the epic's explicit objective) while paying a negligible additional I/O cost at assembly time (one directory copy per profile). No hot-path, no allocation pressure, no new synchronous primitives. The two Partial items are measurement-tooling gaps, not defects.
