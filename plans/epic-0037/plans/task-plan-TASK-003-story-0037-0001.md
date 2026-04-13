# Task Plan — TASK-003

| Field | Value |
|-------|-------|
| Task ID | TASK-003 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | merged(Architect, QA, PO) |
| Type | documentation |
| TDD Phase | GREEN |
| Layer | cross-cutting |
| Estimated Effort | XS |
| Date | 2026-04-13 |

## Objective

Delete the drift section "Integration with Epic Execution" (lines 355-379) from `x-git-worktree/SKILL.md`. Section documents an integration that does not exist; rewrite is scoped to story-0037-0003.

## Implementation Guide

1. Open `targets/claude/skills/core/x-git-worktree/SKILL.md`.
2. Locate H2 heading "Integration with Epic Execution" (~line 355).
3. Delete from the H2 line through the last line of the section (~line 379).
4. Insert a single HTML comment placeholder at the deletion site:
   `<!-- This section will be rewritten by story-0037-0003 after x-dev-epic-implement migration. -->`
5. Grep verifications:
   - `grep "Integration with Epic Execution" SKILL.md` → 0 hits
   - `grep "Agent(isolation" SKILL.md` → 0 hits inside this section (other mentions OK)
6. Verify surrounding markdown structure (next H2 still renders cleanly).

## Definition of Done

- [ ] Section header + body deleted
- [ ] HTML comment placeholder in place referencing story-0037-0003
- [ ] `grep "Integration with Epic Execution" SKILL.md` → 0
- [ ] No orphaned anchor links elsewhere in the file pointing to deleted section
- [ ] Surrounding sections render valid markdown
- [ ] No commented-out remnants of the deleted prose left behind (PO-003)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-001 | Sequencing — rule file exists before SKILL.md edits |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Other docs link to the deleted section anchor | Low | Low | grep for `#integration-with-epic-execution` across `targets/` |
