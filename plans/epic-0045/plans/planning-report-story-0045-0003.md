# Story Planning Report -- story-0045-0003

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0003 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0003 inserts Step 2.2.8.5 into `x-story-implement/SKILL.md` between PR_CREATED (2.2.8) and APPROVAL GATE (2.2.9, EPIC-0043). Step invokes `x-pr-watch-ci` via Rule 13 Pattern 1 INLINE-SKILL, gates on schema v2 + `--no-ci-watch` flag, persists `task.ciWatchResult` to execution-state, propagates exit code into menu description (RULE-045-07), and forces the menu on exit 20/30 (overriding `--auto-approve-pr`). 29 proposals consolidated into 24 tasks.

## Architecture Assessment

- **Affected files:** `x-story-implement/SKILL.md` (Phase 2.2 edits ~L797–810 per story §3.1; args table; Step 2.3 detail); goldens across targets.
- **Skip matrix (evaluated in order):** (1) schema v1 → skip (Rule 19/RULE-045-02); (2) `--no-ci-watch` → skip; (3) otherwise invoke.
- **State-file extension:** `task.ciWatchResult` object with 5 subfields; legacy shape without field parses (RULE-045-03 schemaVersion "1.0" unchanged).
- **Menu propagation:** Step 2.3 formatter consumes `ciExitCode` + `ciJson`; injects "CI Status: N passed / M failed / K pending" + Copilot status; exit 20/30 prepends "⚠️ CI FAILED — FIX-PR recommended" and forces menu.

## Test Strategy Summary

Story is prose-heavy (SKILL.md edits), so test surface is dominated by:
- Golden diff IT (critical gate — TASK-007/008 RED/GREEN pair)
- Multi-target SHA audit (TASK-009) prevents partial regen drift
- State schema round-trip (TASK-010) with 8-exit-code matrix
- Exit-code propagation IT (TASK-011) asserts 8×expected-phrase matrix
- EPIC-0043 menu regression (TASK-012): 3-option invariant preserved
- v1 backward-compat smoke (TASK-013) asserts zero CI-Watch invocations on v1
- `--no-ci-watch` opt-out golden (TASK-014)

## Security Assessment Summary

Surface is narrow (text-level edit + one new state field). 4 controls:
- **Args template literal** — `args:` must be literal with `{{PR_NUMBER}}` placeholder (no `$VAR`/`$(...)`)
- **Menu text sanitization** — strip ANSI ESC/CR/BEL/C0/C1 from upstream check names/reviewIds; cap 120 chars
- **State-file scrubber** — route `task.ciWatchResult` string fields through `TelemetryScrubber` (Rule 20 patterns); prevents GitHub token leakage via check names
- **Frontmatter least-privilege** — allowed-tools MUST equal EPIC-0043 baseline; no silent expansion

## Implementation Approach

Tech Lead enforces: Rule 13 Pattern 1 INLINE-SKILL audit (grep 0 matches); frontmatter contains both `Skill` and `AskUserQuestion`; Phase 2.2.9 menu byte-identical (audit-interactive-gates.sh exit 0; 2.2.8.5 < 2.2.9 ordering); Rule 14 no worktree creation in new step; phase numbering preserved (2.2.9 not renumbered; Phase-2-2-9-* telemetry markers unchanged); Conventional Commits `feat(task-0045-0003-NNN):`; EPIC-0043 precondition verified (merged to develop) before story merges; `mvn process-resources` before `GoldenFileRegenerator` (memory note).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 24 |
| Architecture tasks | 6 |
| Test tasks | 8 |
| Security tasks | 4 |
| Quality gate tasks | 4 |
| Validation tasks | 2 |
| Merged tasks | 2 (ARCH golden regen merged with QA RED/GREEN pair) |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| EPIC-0043 menu block drift (merge conflict at L797-810) | Architect | High | High | Story 0045-0003 gated on EPIC-0043 merged; TASK-012/020 byte-identical invariant tests |
| Phase numbering regression (2.2.9 renumbered) | TechLead | High | Medium | TASK-022 phase numbering integrity test + Phase-2-2-9-* marker audit |
| State-file shape regression on v1 epics | Security | High | Low | TASK-003 backward-compat clause + TASK-013 v1 smoke |
| ANSI injection via upstream check names | Security | Medium | Low | TASK-016 sanitizer contract + negative fixture |
| Token leak via check name in ciWatchResult | Security | Medium | Low | TASK-017 TelemetryScrubber pre-write |
| Auto-approve bypassed on CI_FAILED (safety-of-release) | PO | High | Medium | TASK-005 force-menu override + Gherkin scenario |
| --no-ci-watch silent removal in future refactor | PO | Low | Medium | TASK-014 golden assertion on literal flag |

## DoR Status

See `dor-story-0045-0003.md`.
