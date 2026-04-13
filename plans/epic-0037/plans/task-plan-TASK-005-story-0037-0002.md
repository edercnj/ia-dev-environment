# Task Plan — TASK-005 (story-0037-0002)

| Field | Value |
|-------|-------|
| Source Agent | merged(QA, Architect, TechLead) | Type | verification | TDD Phase | VERIFY | Effort | XS |

## Objective
Manual smoke test of `detect_worktree_context()` snippet across all Gherkin scenarios using a self-contained fixture. Capture evidence to a committed file and link from PR body.

## Implementation Guide
1. Create `plans/epic-0037/plans/smoke-evidence-story-0037-0002.md`.
2. Embed fixture script:
   ```bash
   set -e
   cd /tmp && rm -rf wt-smoke && mkdir wt-smoke && cd wt-smoke && git init -q
   git commit --allow-empty -q -m init
   git worktree add .claude/worktrees/test-wt -b feat/test
   # Run snippet from each location; capture stdout/stderr/exit code
   git worktree remove --force .claude/worktrees/test-wt
   ```
3. Execute snippet in 5 contexts: main repo root; inside `.claude/worktrees/test-wt`; deep subdir of worktree; `/tmp/not-a-repo`; empty `.claude/worktrees/`.
4. After TASK-006 amendments merged: also test detached HEAD and symlinked path.
5. After TASK-004 hardening: include double-quote-path scenario.
6. Capture stdout, stderr, exit code per scenario; paste into evidence file.
7. Validate every JSON output with `jq empty` (must succeed for all success cases).
8. Link evidence file from PR body.

## DoD
- [ ] `smoke-evidence-story-0037-0002.md` created in plans dir
- [ ] Fixture script self-contained and idempotent (re-runnable)
- [ ] All 5 Gherkin scenarios + 2 PO amendments + 1 SEC scenario covered (8 total minimum)
- [ ] Each output includes stdout, stderr, exit code
- [ ] All success-case JSON validates with `jq empty`
- [ ] Evidence file linked from PR body

## Dependencies
TASK-002, TASK-004, TASK-006.
