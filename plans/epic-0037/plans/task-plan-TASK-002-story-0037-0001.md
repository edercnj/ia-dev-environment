# Task Plan — TASK-002

| Field | Value |
|-------|-------|
| Task ID | TASK-002 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | merged(Architect, QA, PO) |
| Type | documentation |
| TDD Phase | GREEN |
| Layer | cross-cutting |
| Estimated Effort | XS |
| Date | 2026-04-13 |

## Objective

Replace inline naming convention block (lines ~49-57) in `targets/claude/skills/core/x-git-worktree/SKILL.md` with a single-line pointer to RULE-018 / `14-worktree-lifecycle.md`.

## Implementation Guide

1. Open `java/src/main/resources/targets/claude/skills/core/x-git-worktree/SKILL.md`.
2. Locate the section "Naming Convention (RULE-018)" around lines 49-57.
3. Compute correct relative path from SKILL.md to the new rule file: `../../../../rules/14-worktree-lifecycle.md` (verify depth from `skills/core/x-git-worktree/`).
4. Replace the inline table with a single line:
   `> **See:** [RULE-018 — Worktree Lifecycle](../../../../rules/14-worktree-lifecycle.md) for naming convention, protected branches, non-nesting invariant, lifecycle, and creator-owns-removal matrix.`
5. Verify markdown frontmatter and surrounding sections remain intact.
6. `grep -n "RULE-018" SKILL.md` — should return >=1 line containing the pointer.
7. `grep -n "task-XXXX-YYYY-NNN" SKILL.md` — should return 0 (table removed).

## Definition of Done

- [ ] Lines 49-57 contain only the pointer line
- [ ] Inline naming convention table removed (no duplicate elsewhere in file)
- [ ] Relative link path resolves to `targets/claude/rules/14-worktree-lifecycle.md`
- [ ] SKILL.md frontmatter intact
- [ ] Surrounding sections unchanged
- [ ] `grep "Naming Convention" SKILL.md` returns at most 1 hit (the pointer header)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-001 | Pointer target must exist |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Wrong relative path depth | Medium | Low | Verify by clicking link in GitHub preview |
