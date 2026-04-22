# Story Planning Report -- story-0045-0004

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0004 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0004 inserts Step 4.5 into `x-task-implement/SKILL.md` between Step 4 (atomic TDD commits) and Step 5 (worktree cleanup). Step 4.5 conditionally invokes `x-pr-watch-ci` with a skip matrix (schema v1 / `--no-ci-watch` / orchestrator parent detected). Key concern: `x-task-implement --worktree` already creates a worktree, so `x-pr-watch-ci` must remain sequential/API-only (no nested worktree). 26 proposals consolidated into 19 tasks.

## Architecture Assessment

- **Affected files:** `x-task-implement/SKILL.md` (Step 4.5 insertion, detect-context block, `--no-ci-watch` flag, frontmatter `Skill`); goldens across targets.
- **Skip matrix (evaluated in order):** (1) schema v1 → skip; (2) `--no-ci-watch` → skip; (3) orchestrator=parent (detect-context) → skip with log "delegated to parent"; (4) otherwise invoke `x-pr-watch-ci`.
- **Detect-context mechanism:** Rule 14 §3 canonical `git rev-parse --show-toplevel` + fixed-string `/.claude/worktrees/` substring check. Marker authority is the trusted detect-context routine — NO env-var/flag override (security-critical).
- **State-file:** `.claude/state/task-watch-<TASK-ID>.json` per RULE-045-03 schema v1.0 (atomic `.tmp` + rename).

## Test Strategy Summary

- **Outer loop:** Standalone CI-failed → menu highlights FIX-PR (end-to-end IT with stubbed gh CLI).
- **Inner loop (TPP on detect-context):** nil (no parent → invoke) → constant (parent=x-story-implement → skip) → scalar (parent=x-epic-implement → skip) → conditional (malformed marker → fail-open to standalone, WARN log).
- **Regression tests:** v1 fallback (zero invocations + SCHEMA_VERSION_FALLBACK_* log); `--no-ci-watch` (zero invocations + opt-out log); EPIC-0043 menu still fires on both paths.
- **Double-watch prevention:** orchestrated invocation (subagent of x-story-implement) emits zero CI-Watch markers.

## Security Assessment Summary

Surface narrow (SKILL.md text edit). 4 controls:
- **Detect-context authority (TASK-014)** — marker MUST come from `/x-git-worktree detect-context`, forbidding plan-supplied flags / env-var overrides. Prevents forged skip.
- **No nested worktree (TASK-015)** — verify `x-pr-watch-ci` is API-polling only; smoke asserts `git worktree list` count unchanged after Step 4.5.
- **Frontmatter least-privilege (TASK-016)** — allowed-tools adds only `Skill`.
- **State-file whitelist + scrubber (TASK-017)** — `task-watch-<TASK-ID>.json` whitelists 5 fields; `failureReason` routed through scrubber; no raw PR body/job env/log excerpts.

## Implementation Approach

Tech Lead enforces: verbatim Rule 13 Pattern 1 call (no bare-slash); frontmatter `Skill` present idempotent; telemetry markers `Phase-4-5-CI-Watch start/end` balanced; state-file atomic write; Conventional Commits `feat(task-0045-0004-NNN):`; Step numbering (4.5 between 4 and 5) preserved; standalone vs orchestrated parity with x-story-implement retrofit (same skip conditions, mirrored logic).

PO refinements:
- Parent-orchestrator marker must be explicit (env var or state-file key named and documented)
- `--no-ci-watch` precedence wins over schema version (v2 + flag → skip)
- Rule 14 `detect-context` envelope extension OR derived classifier needed (returns `orchestrator=parent\|none`)

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 19 |
| Architecture tasks | 5 |
| Test tasks | 8 |
| Security tasks | 4 |
| Quality gate tasks | 2 |
| Validation tasks | 0 (PO observations integrated into ARCH/QA) |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Forged orchestrator marker bypasses CI-Watch | Security | High | Low | TASK-014 pins marker to trusted `/x-git-worktree detect-context` only |
| Nested worktree regression (Rule 14 violation) | Security | High | Low | TASK-015 smoke asserts `git worktree list` count unchanged |
| Silent skip of CI-Watch via broken detect-context | QA | Medium | Medium | TASK-010 fail-open to standalone on malformed marker |
| --no-ci-watch precedence ambiguity | PO | Medium | Medium | TASK-003 documents flag-over-schema precedence |
| State-file leaks upstream log excerpts | Security | Medium | Low | TASK-017 whitelist + scrubber |
| Missing `orchestrator` classifier in detect-context envelope | TechLead | Medium | High | TASK-002 extends envelope OR derives classifier in SKILL.md |

## DoR Status

See `dor-story-0045-0004.md`.
