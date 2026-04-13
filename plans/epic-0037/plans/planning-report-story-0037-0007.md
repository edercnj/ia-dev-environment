# Story Planning Report — story-0037-0007

| Field | Value |
|-------|-------|
| Story ID | story-0037-0007 |
| Epic ID | 0037 |
| Date | 2026-04-13 |

## Planning Summary

Makes `x-pr-fix-epic` worktree-aware automatically (not opt-in). Orchestrator-like skill where isolation always makes sense. Idempotency (RULE-010 of the skill) is preserved: resumed runs reuse `.claude/worktrees/fix-epic-{id}/` rather than re-creating.

## Architecture Assessment

Modifies branch-creation block in `x-pr-fix-epic/SKILL.md` (~line 795). Adds Step N.1 (detect), N.2 (decision table), N.3 (idempotency check). Cleanup runs after correction PR merged. `IN_WT=true` edge case (unusual context) logs warning and proceeds without create.

## Test Strategy Summary

3 smoke scenarios: first-run create, resumed-run reuse, post-merge cleanup. Manual smoke with fixture epic + simulated PR.

## Security Assessment Summary

- No new input surfaces. Worktree ID derived from epic ID (numeric).
- Skill typically runs from main repo (orchestrator context), so nesting is unusual.
- Risk level: **LOW**.

## Implementation Approach

Sequential: branch-creation rewrite → idempotency check → cleanup → 3-scenario smoke → golden regen + PR.

## Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Resumed run creates second worktree | Medium | Medium | Idempotency check (TASK-002) is mandatory |
| Correction PR never merges → worktree leaks | Low | Medium | Operator uses `/x-git-worktree cleanup` periodically |
| `IN_WT=true` edge case breaks flow | Low | Low | Warning + fallback to current cwd documented |

## DoR Status

**READY** — see `dor-story-0037-0007.md`.
