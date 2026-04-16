# Story Planning Report -- story-0039-0014

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0014 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (FALLBACK_MISSING_FIELD) |

## Planning Summary

Parity story: `/x-release --hotfix` inherits the full interactive UX shipped by
phases 1-3 of epic-0039 (auto-version, prompts, smart resume, pre-flight, SUMMARY
diagram, telemetry) with adaptations — PATCH-only bumps, base branch = `main`,
separate state files, hotfix-specific SUMMARY diagram, and a `releaseType="hotfix"`
telemetry derivation. Implementation reuses ~95% of the existing release pipeline
via a new immutable `ReleaseContext` record injected through the dependency chain.
Plan decomposes into 14 tasks: 5 RED/GREEN pairs (context, version rejection,
version override, state-file isolation), 1 context-injection fan-out (preflight +
summary + telemetry), 1 SEC verification, 1 SKILL.md doc task, 1 smoke, and 2
quality/validation gates.

## Architecture Assessment

- **Affected layers:** domain (new `ReleaseContext` + `BumpRestriction` enum),
  application (VersionDetector param), adapter.outbound (StateFileDetector,
  TelemetryWriter, PreflightDashboardRenderer, SummaryRenderer — all accept ctx),
  config (SKILL.md hotfix section + error catalog), test (smoke).
- **New components:**
  - `ReleaseContext` (domain.model, record) — `(BumpRestriction restrictBumpTo,
    String baseBranch, boolean hotfix)`, factories `release()` / `hotfix()`.
  - `BumpRestriction` (domain.model, enum) — `ANY` / `PATCH_ONLY`.
  - `HotfixInvalidCommitsException` / `HotfixVersionNotPatchException` — carry
    error codes per story §5.4.
- **Adapted components (parametrized):**
  - `VersionDetector` — accepts `ReleaseContext`, enforces `restrictBumpTo`.
  - `StateFileDetector` — resolves `release-state-hotfix-<X.Y.Z>.json` when hotfix.
  - `PreflightDashboardRenderer` — renders hotfix banner.
  - `SummaryRenderer` — emits hotfix mermaid variant.
  - `TelemetryWriter` — derives `releaseType` from `ctx.hotfix`.
- **Dependency direction:** config/SKILL -> application/VersionDetector ->
  domain/ReleaseContext <- adapter.outbound (state, preflight, summary, telemetry).
  Domain `ReleaseContext` has ZERO framework imports — verified by Rule 04.
- **Reuse strategy:** 95% code reuse via dependency injection; only bump validation
  and branching decisions are parametric. Existing release pipeline is NOT
  duplicated.

## Test Strategy Summary

- **Acceptance tests (outer loop):** 6 Gherkin scenarios from §7 mapped to:
  - 1 smoke test (TASK-012) covering happy path (PATCH auto-detect), degenerate
    (feat commit rejected), boundary (state file separation), SUMMARY variant, and
    telemetry releaseType.
  - 1 unit test (TASK-005) for `--version` override non-PATCH error.
- **Unit tests (inner loop, TPP order):**
  - ReleaseContext: nil (degenerate factory probes) -> constant (record equality).
  - VersionDetector: scalar (feat rejected) -> conditional (override validator).
  - StateFileDetector: scalar (hotfix path) -> conditional (dispatch by flag).
  - Renderers: collection (3 components share context injection).
- **Coverage target:** >=95% line, >=90% branch on `dev.iadev.release` packages
  affected by hotfix changes.
- **Framework:** JUnit 5 + AssertJ; `@TempDir` for state-file ITs; parametrized
  tests for commit-type matrix.

## Security Assessment Summary

- **OWASP categories applicable:**
  - **A03 Injection** — version string inserted into state-file path; MUST validate
    against `^\d+\.\d+\.\d+$` to prevent path traversal via crafted `--version`.
  - **A05 Security Misconfiguration** — state-file prefix hardcoded to `plans/` (no
    user-controlled directory); SKILL.md documents PATCH-only restriction.
  - **A09 Logging & Monitoring Failures** — hotfix error codes (`HOTFIX_INVALID_COMMITS`,
    `HOTFIX_VERSION_NOT_PATCH`) emit WARN-level structured messages only; never
    leak internal stack traces or class names to operator stdout.
- **No new secrets** handled. No network I/O. State-file write reuses existing
  atomic write path from S02 (no new I/O code introduced). Immutable
  `ReleaseContext` record prevents tamper after construction.
- **Risk level:** LOW — purely additive parametrization of trusted release pipeline.

## Implementation Approach

- `ReleaseContext` is a Java record (immutable, zero-args factories). Ships with
  `release()` and `hotfix()` static factories — no builder required.
- `BumpRestriction` enum declares only `ANY` and `PATCH_ONLY`; future MINOR-only
  flows can extend without touching callers.
- `VersionDetector.detectVersion(commits, ctx)` is a pure function — no file I/O.
  Rejection happens at commit classification stage via `ctx.restrictBumpTo` check.
- `StateFileDetector.resolveStatePath(version, ctx)` uses ternary dispatch on
  `ctx.hotfix`; no regex or string parsing.
- Three renderers (preflight, summary, telemetry) are refactored in ONE task
  (TASK-009) to keep the context-injection pattern consistent and avoid mid-flight
  interface inconsistency between them.
- SKILL.md edit touches ONLY `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`
  (RULE-001 — source of truth). `mvn process-resources` regenerates the `.claude/`
  mirror before any smoke test run.
- Tech Lead consistency: all 5 adapted components accept `ReleaseContext` uniformly
  — no Boolean flag parameters (coding-standards forbids boolean flag arguments).
- No new exception hierarchy — reuses existing `ReleaseException` base with new
  error codes.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 14 |
| Architecture tasks | 1 (TASK-011 SKILL.md + error catalog) |
| Test tasks (RED) | 4 (TASK-001, 003, 005, 007) |
| Test tasks (GREEN impl) | 5 (TASK-002, 004, 006, 008, 009) |
| Smoke tasks | 1 (TASK-012) |
| Security tasks | 1 (TASK-010) |
| Quality gate + validation tasks | 2 (TASK-013, TASK-014) |
| Merged tasks | 5 (ReleaseContext, VersionDetector x2, StateFileDetector, renderer fan-out) |
| Augmented tasks | 2 (TASK-004 and TASK-008 augmented with SEC-010 version-string validation criteria via injection rule) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Hotfix state file collides with release-normal state on simultaneous runs | ARCH | HIGH | LOW | Separate filename prefix `release-state-hotfix-` (TASK-008) |
| `--version` override bypasses PATCH restriction | SEC | HIGH | LOW | Explicit `HOTFIX_VERSION_NOT_PATCH` guard at detector (TASK-006) |
| Version string traversal via crafted `--version ../../etc` | SEC | MEDIUM | LOW | Regex `^\d+\.\d+\.\d+$` validation before any filesystem use (TASK-010) |
| Boolean flag parameter creeps into renderer signatures | TL | MEDIUM | MEDIUM | Uniform `ReleaseContext` injection across all 5 adapters; TASK-013 cross-file consistency gate |
| SKILL.md mirror drifts from source-of-truth | TL | MEDIUM | LOW | Edit only `targets/claude/`; `mvn process-resources` regenerates; verified in TASK-011 DoD |
| `releaseType` field collides with existing telemetry schema | SEC | LOW | LOW | Derive from existing `hotfix: true` state-file field; no new schema field (TASK-009) |
| Smoke test over-couples to exact SUMMARY rendering | QA | LOW | MEDIUM | Assert on substring presence (`hotfix/3.1.1`) not full diagram equality (TASK-012) |
| 95% reuse assumption invalid (hidden release-only code paths) | ARCH | MEDIUM | LOW | TASK-013 coverage run exposes untested branches on hotfix path |

## DoR Status

READY — see `dor-story-0039-0014.md`. All 10 mandatory checks PASS; conditional
checks N/A (compliance=none, contract_tests=false).
