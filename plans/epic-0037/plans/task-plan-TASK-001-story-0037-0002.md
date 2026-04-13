# Task Plan — TASK-001 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | merged(Architect, QA, PO) | Type | documentation | TDD Phase | GREEN | Effort | S |

## Objective
Add "Operation 5: detect-context" section to `targets/claude/skills/core/x-git-worktree/SKILL.md` between Operation 4 and "Git Flow Integration". Includes workflow, canonical bash snippet, 3 sample outputs, JSON schema, exit-code contract, Mermaid flowchart, RULE-018 cross-reference.

## Implementation Guide
1. Open SKILL.md; locate end of "Operation 4: cleanup" (~L329) and "Git Flow Integration" header (~L331).
2. Insert new H3 section `### Operation 5: detect-context`.
3. Top of section: blockquote `> **See:** [RULE-018 — Worktree Lifecycle](../../../../rules/14-worktree-lifecycle.md), Section 3 (Non-Nesting Invariant)`. Verify path depth (4-level current; 5-level post-EPIC-0036).
4. Subsections in order: Parameters (none), Output (JSON), Workflow (4-step list), Mermaid flowchart (from story Section 6.1), Bash Snippet (canonical `detect_worktree_context()` from story Section 3.1), Sample Outputs (3 cases), Error Handling table.
5. Snippet must be byte-identical to story 3.1 source initially; TASK-004 will harden JSON escaping.
6. Embed JSON schema from story Section 5.1.
7. Embed exit-code table from story Section 5.2.

## DoD
- [ ] Section placed between Operation 4 and Git Flow Integration
- [ ] H3 heading `### Operation 5: detect-context`
- [ ] RULE-018 cross-reference resolves (verify with click-test)
- [ ] Function `detect_worktree_context()` defined; POSIX (no jq dep in canonical block)
- [ ] 3 sample JSON outputs valid, parseable, match schema
- [ ] JSON schema (draft-07) embedded; required = [inWorktree, worktreePath, mainRepoPath]
- [ ] Mermaid flowchart renders cleanly; nodes match snippet steps
- [ ] Exit codes 0/1/2 documented per story 5.2
- [ ] Surrounding sections unchanged

## Dependencies
TASK-000.

## Risks
| Risk | Mitigation |
|------|------------|
| Wrong relative path depth | Click-test in GitHub preview |
| Mermaid syntax errors | Local preview before commit |
