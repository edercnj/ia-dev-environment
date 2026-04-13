# Story Planning Report — story-0037-0006

| Field | Value |
|-------|-------|
| Story ID | story-0037-0006 |
| Epic ID | 0037 |
| Date | 2026-04-13 |

## Planning Summary

Extends the story-0005 pattern to the task layer. Adds `--worktree` opt-in to `x-task-implement`; detection ensures task execution inside an existing worktree (story or epic) reuses that worktree (no nesting). Closes the last direct branch-creation point.

## Architecture Assessment

Modifies `x-task-implement/SKILL.md` frontmatter, parameters table, and Branch Creation block (~line 150). Inlines `detect_worktree_context()`. 3-context decision table (standalone without flag, standalone with flag, orchestrated-inside-worktree). Uses `TASK_OWNS_WORKTREE` flag parallel to story-0005's `STORY_OWNS_WORKTREE`.

## Test Strategy Summary

4 smoke scenarios: standalone-legacy, standalone-worktree, orchestrated-inside-story-worktree, orchestrated-inside-epic-worktree. Each verifies branch creation, detection behavior, and cleanup ownership.

## Security Assessment Summary

Same risk profile as story-0005: nesting prevention is the critical invariant. Task worktree paths use `.claude/worktrees/task-XXXX-YYYY-NNN/` naming from RULE-018 Section 1.

## Implementation Approach

Sequential: frontmatter → branch-creation block → cleanup block → 4-scenario smoke → golden regen → PR.

## Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Task creates worktree inside story worktree (nesting) | CRITICAL | Low | Detection mandatory; scenario 3 covers |
| Task cleanup removes story worktree | HIGH | Low | `TASK_OWNS_WORKTREE` flag gates cleanup |
| Backward-compat for direct invocations | HIGH | Low | Legacy scenario is TASK-004 first case |

## DoR Status

**READY** — see `dor-story-0037-0006.md`.
