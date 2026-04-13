# Task Plan — TASK-006 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | ProductOwner | Type | validation | TDD Phase | GREEN | Effort | XS |

## Objective
Apply PO amendments to story file: add detached-HEAD and symlinked-path Gherkin scenarios; standardize all path examples to `/repo`; verify literal `null` semantics for worktreePath.

## Implementation Guide
1. Open `plans/epic-0037/story-0037-0002.md`.
2. Section 7 — add Gherkin scenarios:
   - "Detached HEAD inside main repo" (matches Sample Output #3)
   - "Worktree path with symlink in middle" (covers macOS `/private/var` and CI runner symlink resolution)
3. Section 3.1 (Sample Outputs) — replace all `/Users/dev/repo` with `/repo` for consistency.
4. Section 7 (Gherkin) — same standardization.
5. Update Section 7.2 mandatory categories list to include the new scenarios.
6. Verify snippet emits literal `null` (not string `"null"`) for `worktreePath` when not in worktree (printf format check).
7. Adjust ordering note in Section 7.1 if new scenarios fit a TPP slot.

## DoD
- [ ] Section 7 has new "Detached HEAD" scenario
- [ ] Section 7 has new "Worktree path with symlink" scenario
- [ ] All path examples in Sections 3.1 + 7 use `/repo` (zero `/Users/dev/repo` remaining)
- [ ] Section 7.2 mandatory categories list updated
- [ ] worktreePath emits literal `null` (printf verified)
- [ ] PO-004 callout added to story-0037-0003..0008 dependencies sections (separate sub-task, may defer to those stories)

## Dependencies
TASK-001 (parallel with TASK-002, TASK-003, TASK-004).

## Notes
PO-004 (cross-story callout) is best handled by author of stories 0003-0008 during their own implementation rather than within this PR — captured here as escalation, not blocking task.
