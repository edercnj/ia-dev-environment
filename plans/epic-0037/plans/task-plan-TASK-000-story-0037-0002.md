# Task Plan — TASK-000 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | merged(PO, TechLead) | Type | validation | TDD Phase | VERIFY | Effort | XS |

## Objective
DoR gate: confirm story-0037-0001 merged to develop, branch cut, baseline green.

## Implementation Guide
1. Verify Rule 14 file present at `targets/claude/rules/14-worktree-lifecycle.md` (story-0037-0001 merged).
2. `git checkout develop && git pull && mvn clean verify` — green baseline.
3. `git checkout -b feature/story-0037-0002-detect-context`.
4. Read `targets/claude/skills/core/x-git-worktree/SKILL.md` in full; identify insertion point after Operation 4 (cleanup).
5. Verify directory depth (4 levels currently; 5 levels post-EPIC-0036 rename).

## DoD
- [ ] Rule 14 exists in develop
- [ ] Branch `feature/story-0037-0002-detect-context` created from updated develop
- [ ] Baseline `mvn clean verify` green
- [ ] x-git-worktree SKILL.md fully read
- [ ] Directory depth verified

## Dependencies
TASK-000 depends on story-0037-0001 (Phase 0 of epic).
