# Task Plan — TASK-002 (story-0037-0007)

> See `tasks-story-0037-0007.md` for full DoD, source agent attribution, and risk table.

| Field | Value |
|-------|-------|
| Task ID | TASK-002 | Story ID | story-0037-0007 | Epic ID | 0037 |

## Reference
Full task spec in `tasks-story-0037-0007.md` row TASK-002. Implementer must read tasks file row + corresponding section in story.

## Implementation Notes
- Open atomic commit per task (RULE-007).
- Verify all DoD checklist items before marking complete.
- Critical for TASK-002: `cd` to main repo before `/x-git-worktree remove` to avoid removing cwd.
- Critical for TASK-004: validate `${epicId}` via regex `^[0-9]{4}$` BEFORE shell interpolation.
