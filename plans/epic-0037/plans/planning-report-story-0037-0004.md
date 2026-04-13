# Story Planning Report — story-0037-0004

| Field | Value |
|-------|-------|
| Story ID | story-0037-0004 |
| Epic ID | 0037 |
| Date | 2026-04-13 |

## Planning Summary

Opt-in `--worktree` flag for `x-git-push`. Pure additive change; backward-compat default preserved (RULE-004). Docs-only.

## Architecture Assessment

Single file modified: `x-git-push/SKILL.md`. Frontmatter, parameters table, Step 1.3 (worktree-aware branch creation), Backward Compatibility section, When-to-Use bullet. Inlines `detect_worktree_context()` snippet from story-0002's Operation 5. Caller-owns-cleanup (does not auto-remove after push).

## Test Strategy Summary

5 Gherkin scenarios TPP-ordered: backward compat → happy main → happy nested (detection reuses) → error (create fails) → boundary (non-standard slug prefix). Regression smoke verifies byte-identical default behavior.

## Security Assessment Summary

- Slug sanitization regex strips type prefix; documented in Section 5.1.
- No new input surfaces; flag-controlled branch path stays within `.claude/worktrees/`.
- OWASP: N/A for docs; A01 considerations are documented via sanitization guidance.
- Risk level: **LOW**.

## Implementation Approach

Sequential: frontmatter → params → Step 1.3 → regression smoke (blocker) → flag smoke → golden regen + PR.

## Task Breakdown Summary

6 tasks: 3 doc + 2 smoke + 1 quality-gate.

## Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Default behavior changes inadvertently | HIGH | Low | Regression smoke mandatory as TASK-004 |
| Slug collision on `.claude/worktrees/` | Medium | Low | Slug derivation deterministic; caller responsible for unique branch |
| Nested worktree accidentally created | High | Low | Detection check per Section 3.3 |

## DoR Status

**READY** — see `dor-story-0037-0004.md`.
