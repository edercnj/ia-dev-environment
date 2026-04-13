# Story Planning Report — story-0037-0007

| Field | Value |
|-------|-------|
| Story ID | story-0037-0007 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary

Doc-only story making `x-pr-fix-epic-comments` automatically worktree-aware (NOT opt-in — orchestrator-pattern). Idempotent re-runs reuse existing worktree (RULE-010 of skill). Cleanup gated on PR merged. 5 Gherkin scenarios + 2 PO additions = 7 total. Consolidated to 9 atomic tasks.

## Architecture Assessment

3 substeps near L795 of SKILL.md: (1) detect via inline `detect_worktree_context()`; (2) decide based on `IN_WT` and existence of fix-epic-${epicId} dir; (3) idempotency check `[ -d ".claude/worktrees/fix-epic-${epicId}" ]`. Cleanup step at end of workflow: must `cd` to main repo BEFORE `/x-git-worktree remove` (avoid removing cwd). Canonical worktree id = `fix-epic-${epicId}` (not `fix-epic-{id}` placeholder form).

## Test Strategy Summary

7 ATs (5 Gherkin + 2 PO additions: WT_REUSE_DIVERGED, merge-conflict defer-cleanup). Smoke fixture: ephemeral epic with mock PR comments; 3 invocations covering create / reuse / cleanup; edge invocation from inside another worktree. Idempotency proof: identical worktree path across runs, no duplicate creation, captured with timestamps.

## Security Assessment Summary

- **CWE-78 command injection**: `${epicId}` interpolated into shell — must validate `^[0-9]{4}$` before use
- **Error masking**: `git checkout fix/... || true` swallows real errors — replace with explicit handling
- **CWE-209 information disclosure**: scrub absolute worktree paths from user-facing error messages
- **WT_REUSE_DIVERGED**: explicit error code documented (existing worktree on different branch than expected)

## Implementation Approach

TechLead enforces RULE-003 (creator-owns-removal): pr-fix-epic creates and removes its own worktree. Cleanup gated on PR merged state — preserves worktree if merge blocked or aborted (operator manual cleanup). Atomic commits per concern. Smoke fixture is mandatory deliverable in PR body.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 9 |
| Architecture | TASK-001, TASK-002, TASK-003 |
| Test/Smoke | TASK-006, TASK-007 |
| Security | TASK-004 |
| Quality gate | TASK-008, TASK-009 |
| Validation | TASK-005 |
| Merged | 4 |
| Augmented | 1 (TASK-001 ← Sec) |

## Risk Matrix

| Risk | Source | Sev | Likely | Mitigation |
|------|--------|-----|--------|-----------|
| Cleanup removes cwd accidentally | Architect | High | Low | TASK-002 explicit `cd` to main repo first; example provided |
| `${epicId}` injection | Security | High | Low | TASK-004 regex validation `^[0-9]{4}$` |
| Reused worktree on wrong branch (silent corruption) | Security | Medium | Low | TASK-003 WT_REUSE_DIVERGED error code + remediation |
| Smoke fixture requires PR mocking | QA | Medium | Medium | Document fixture creation in plans dir; consider gh CLI mock |
| Edge case (invoked from inside worktree) | PO | Low | Low | TASK-001 warning + proceed (no abort) |

## DoR Status

**READY** — 10/10 mandatory pass. See `dor-story-0037-0007.md`.
