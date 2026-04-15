# Story Planning Report -- story-0039-0009

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0009 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (FALLBACK_MISSING_FIELD) |

## Planning Summary

Pre-flight dashboard aggregates outputs of S01 (auto-version) and S03 (integrity
checks) into a single-screen confirmation panel rendered between Step 1 DETERMINE
and Step 2 VALIDATE-DEEP of `x-release`. Three operator choices (Prosseguir /
Editar versão / Abortar) with a bypass flag `--no-preflight` for CI contexts.
Integrity FAIL short-circuits the prompt and aborts with a distinct exit code.
Plan decomposes into 13 tasks, organized in a 5-wave dependency chain driven by
RED-before-GREEN pairing and PO validation last.

## Architecture Assessment

- **Affected layers:** application (new package `dev.iadev.release.preflight`),
  config (SKILL.md + error-catalog), test (smoke).
- **New components:**
  - `PreflightDashboardRenderer` (application) — pure string composition of 5
    sections from a `DashboardData` record.
  - `PreflightDashboard` (application) — orchestrator: calls VersionDetector
    (S01) + IntegrityChecker (S03), renders, dispatches to AskUserQuestion or
    aborts.
- **Reused components:** `VersionDetector` (S01), `IntegrityChecker` (S03) —
  this story adds zero net domain logic; it's an aggregator.
- **Dependency direction:** application -> domain.port (IntegrityReport,
  DetectedVersion are consumed as DTOs, not mutated). No domain imports needed.

## Test Strategy Summary

- **Acceptance tests (outer loop):** 6 Gherkin scenarios from §7 mapped 1:1 to
  smoke tests in TASK-010/011.
- **Unit tests (inner loop, TPP order):** 6 unit tests on the renderer covering
  nil (null input), constant (happy render), scalar (one section missing),
  collection (multi-line preview), conditional (integrity FAIL branch), iteration
  (truncation boundary at N-1 / N / N+1 lines).
- **Coverage target:** >=95% line, >=90% branch on `release.preflight` package.
- **Framework:** JUnit 5 + AssertJ (project standard).

## Security Assessment Summary

- **OWASP categories applicable:**
  - **A03 Injection** — CHANGELOG body rendered to terminal; strip ANSI escape
    sequences to prevent terminal injection via crafted commit messages.
  - **A05 Security Misconfiguration** — `--preflight-changelog-lines` must be
    bounded to prevent memory exhaustion on pathological input.
- **No new secrets** handled. No network I/O. No deserialization.
- **Risk level:** LOW — adapter of internal trusted data sources.

## Implementation Approach

- Renderer is a pure function: input = `DashboardData` record, output = `String`.
  No side effects, no I/O — trivially unit-testable.
- Orchestrator is a thin coordinator that follows the same constructor-injection
  pattern as S01's `VersionDetector` (Tech Lead wins: consistency over novelty).
- AskUserQuestion wiring lives in SKILL.md bash block, mirroring how S04
  (`x-release-changelog`) wires prompts. Zero Java code touches
  AskUserQuestion directly.
- `--no-preflight` short-circuits in the SKILL.md orchestrator, not in Java —
  keeps the Java renderer testable in isolation.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 13 |
| Architecture tasks | 2 (TASK-008, TASK-009) |
| Test tasks | 3 RED (TASK-001, 003, 005, 010) + 4 GREEN counterparts |
| Security tasks | 1 (TASK-007) |
| Quality gate tasks | 1 (TASK-012) |
| Validation tasks | 1 (TASK-013) |
| Merged tasks | 4 (TASK-002, 004, 006, 011 — ARCH+QA merge) |
| Augmented tasks | 1 (TASK-002 carries SEC DoD criteria inline + TASK-007 verifies) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| CHANGELOG injection via ANSI escapes | SEC | MEDIUM | LOW | TASK-007: strip control chars before render |
| 80-col layout breaks on long version strings | TL | LOW | MEDIUM | TASK-002 DoD: truncate or wrap version section |
| Operator misclicks "Editar versão" losing context | PO | LOW | MEDIUM | TASK-009: exit message includes exact rerun command |
| --no-preflight used in production shells by accident | TL | MEDIUM | LOW | TASK-008: document flag as CI-only in SKILL.md |
| Truncation indicator grammatically wrong in EN contexts | PO | LOW | LOW | Keep PT-BR `(N linhas omitidas)` — matches existing skill copy |

## DoR Status

READY — see `dor-story-0039-0009.md` for the full checklist.
