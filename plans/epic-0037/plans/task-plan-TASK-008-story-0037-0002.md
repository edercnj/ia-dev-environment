# Task Plan — TASK-008 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | TechLead | Type | quality-gate | TDD Phase | VERIFY | Effort | XS |

## Objective
Enforce commit hygiene and open PR against develop with proper labels and body.

## Implementation Guide
1. Verify each commit matches `^(docs|chore)\(story-0037-0002\): ` (no `feat`/`fix` since no Java code).
2. Recommended atomic commit sequence:
   - `docs(story-0037-0002): add Operation 5 detect-context section`
   - `docs(story-0037-0002): add inline-use pattern with jq prereq`
   - `docs(story-0037-0002): add security considerations subsection`
   - `docs(story-0037-0002): harden JSON escaping in detect-context snippet`
   - `docs(story-0037-0002): apply PO amendments — gherkin + path standardize`
   - `docs(story-0037-0002): add manual smoke evidence`
   - `chore(story-0037-0002): regenerate golden files`
3. Push branch.
4. Open PR via `gh pr create`:
   - base: `develop`
   - title: `docs(story-0037-0002): add x-git-worktree detect-context operation`
   - label: `epic-0037`
   - body: links story file, smoke evidence file; declares RULE-001/002/007 compliance.

## DoD
- [ ] All commits match Conventional Commits pattern
- [ ] Atomic commit sequence followed (regen isolated)
- [ ] Branch name `feature/story-0037-0002-detect-context` (Rule 09)
- [ ] PR base = develop
- [ ] PR labeled `epic-0037`
- [ ] PR body links story + smoke evidence
- [ ] PR body declares RULE-001/002/007 compliance
- [ ] CI green

## Dependencies
TASK-007.
