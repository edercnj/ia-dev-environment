# Task Plan — TASK-006

| Field | Value |
|-------|-------|
| Task ID | TASK-006 |
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Source Agent | TechLead |
| Type | quality-gate |
| TDD Phase | VERIFY |
| Layer | cross-cutting |
| Estimated Effort | XS |
| Date | 2026-04-13 |

## Objective

Enforce commit hygiene (Conventional Commits, atomic boundaries, scope) and open the PR against `develop` with proper labels and body.

## Implementation Guide

1. Verify each commit on the branch matches `^(docs|chore)\(story-0037-0001\): ` pattern (no `feat`/`fix` since no code).
2. Confirm minimum 5 atomic commits in this order:
   - `docs(story-0037-0001): create rule 14 worktree lifecycle`
   - `docs(story-0037-0001): replace inline naming block with RULE-018 pointer`
   - `docs(story-0037-0001): delete drift section in x-git-worktree SKILL.md`
   - `docs(story-0037-0001): update RULE-018 cross-references`
   - `chore(story-0037-0001): regenerate golden files`
3. Confirm regen commit isolates generated output from SoT edits.
4. Push branch.
5. Open PR via `gh pr create`:
   - base: `develop`
   - title: `docs(story-0037-0001): promote RULE-018 to rule file 14-worktree-lifecycle.md`
   - label: `epic-0037`
   - body: links to `plans/epic-0037/story-0037-0001.md`; declares RULE-001/005/006/007 compliance; lists each `RULE-018` redirect classification.

## Definition of Done

- [ ] All commits match Conventional Commits pattern with `(story-0037-0001)` scope
- [ ] Minimum 5 atomic commits in correct order
- [ ] Regen commit separated from SoT-edit commits
- [ ] Branch name follows `feature/story-0037-0001-*` (Rule 09)
- [ ] PR base = `develop` (never `main`)
- [ ] PR labeled `epic-0037`
- [ ] PR body links story file
- [ ] PR body declares RULE-001/005/006/007 compliance
- [ ] No amended commits on pushed branch
- [ ] CI green on PR

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-005 | Build must be green before opening PR |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Commit history not atomic (mixed SoT + regen) | Medium | Low | Stage selectively; review `git log -p` before push |
