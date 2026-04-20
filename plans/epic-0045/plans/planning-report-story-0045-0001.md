# Story Planning Report -- story-0045-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0001 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0001 delivers the `x-pr-watch-ci` primitive skill (taxonomy `core/pr/`) that encapsulates CI polling + Copilot review detection, with a pure Java classifier (`PrWatchStatusClassifier`) backing the 8-row public exit-code contract (RULE-045-05). Planning consolidates 40 TASK_PROPOSAL entries from 5 agents into 36 final tasks organised by TDD phase (6 RED / 6 GREEN / 3 config-skill / 2 golden-IT / 6 security / 8 quality-gate / 5 validation).

## Architecture Assessment

- **Affected layers:** `adapter.pr` (Java helper + enum), `resources/targets/claude/skills/core/pr/x-pr-watch-ci/` (new skill), `test/resources/golden/**` (regenerated), `test/java/dev/iadev/adapter/pr/` (parameterized test).
- **New classes:** `PrWatchExitCode` (enum, 8 values matching RULE-045-05), `PrWatchStatusClassifier` (pure classifier with nested input records `Check`, `ChecksSnapshot`, `CopilotReview`, `PrState`, `ClassifierConfig`).
- **Dependency direction:** adapter.pr uses only `java.time`/`java.util`; zero framework imports (Rule 04). Classifier is IO-free. Skill body in bash performs all IO (gh CLI, state-file).
- **Integration:** Caller invokes skill via Rule 13 Pattern 1 `Skill(skill: "x-pr-watch-ci", args: "...")`. Exit-code/JSON surface is the public contract.
- **Implementation order:** TDD test-first (TASK-001 nil → TASK-006 iteration paired with GREEN impl), SKILL.md base+polling parallel with Java work, README+goldens last.

## Test Strategy Summary

- **Outer loop:** Gherkin scenarios (8) in §7 map 1:1 to parametrized classifier rows + 2 golden-diff ITs (SKILL.md, README).
- **Inner loop:** Strict TPP ordering nil → constant → scalar → collection → conditional → iteration. Each RED (`QA-NNN`) paired with GREEN (`QA-NNN+1`). Final Level 6 `@ParameterizedTest` consolidates 8-row exit-code contract.
- **Coverage target:** ≥ 95% Line / ≥ 90% Branch on classifier (Rule 05). Strong assertions only — no `isNotNull()`-only.
- **Test file:** ≤ 250 lines via `@Nested` organization by TPP level.
- **No live `gh` integration** (owned by story-0045-0006 smoke).

## Security Assessment Summary

- **OWASP mapping:** A01 (state-file ACL), A03 HIGH (`--pr-number`/`--state-file` → `gh` CLI injection surface), A05 (file perms + atomic temp), A08 (state-file JSON deserialization on resume), A09 (secret leakage in `gh` error output).
- **6 controls:** args validation (bounds), path traversal protection, state-file 0600 + atomic move, secret redaction (reuse Rule 20 `TelemetryScrubber` patterns), strict Jackson deserialization (no polymorphic typing, `FAIL_ON_UNKNOWN_PROPERTIES=true`), `ProcessBuilder` argv-list (never `sh -c`).
- **Risk level:** Medium. Attack surface narrow but non-trivial; defenses are standard and well-tested patterns.

## Implementation Approach

Tech Lead enforces: adapter-purity (no framework imports on classifier); Rule 03 hard limits (method ≤ 25 lines, class ≤ 250, ≤ 4 params); JaCoCo gate via CI; TDD test-first via git log audit; Rule 13 audit (allowed-tools `[Bash]` only — primitive, not orchestrator); Rule 14 (no worktrees — sequential skill); Rule 18 atomic commits per task with Conventional Commits scope `task(task-0045-0001-NNN)`; cross-file consistency with EPIC-0035 `release-state` pattern and existing `core/pr/` skills (x-pr-create, x-pr-fix).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 36 |
| Architecture tasks (ARCH) | 3 (SKILL.md base/polling/README) |
| Test tasks (QA) | 14 (12 unit RED/GREEN pairs + 2 golden ITs) |
| Security tasks (SEC) | 6 (args, path, perms, redaction, JSON, ProcessBuilder) |
| Quality gate tasks (TL) | 8 (arch, code-quality, coverage, TDD order, Rule 13, Conv Commits, consistency, Rule 14) |
| Validation tasks (PO) | 5 (AC mapping, exit-code completeness, state-file, JSON contract, scenario amendments) |
| Merged tasks | 2 (QA GREEN-nil merged with ARCH-001 enum; QA GREEN-iteration merged with ARCH-003 classifier) |
| Augmented tasks | 6 (GREEN impl tasks enriched with SEC DoD criteria on input validation + path handling) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| `gh` CLI shell injection via malformed `--pr-number` | Security | High | Low | TASK-018 bounds validation + TASK-023 ProcessBuilder argv-list |
| State-file path traversal | Security | High | Low | TASK-019 canonicalize + prefix-check |
| State-file polymorphic deserialization RCE | Security | High | Very Low | TASK-022 strict Jackson config |
| Secret leakage in error output | Security | Medium | Medium | TASK-021 reuse `TelemetryScrubber` |
| Classifier public contract drift (exit codes) | Tech Lead | High | Low | TASK-026 JaCoCo gate + TASK-033 PO audit |
| SKILL.md golden drift across targets | QA | Medium | Medium | TASK-016/017 golden diff ITs |
| Scope creep from PO scenario gaps (NO_CI_CONFIGURED/PR_CLOSED) | PO | Low | High | TASK-036 amend §7 before implementation |

## DoR Status

See `dor-story-0045-0001.md`. Verdict filled in Phase 5.
