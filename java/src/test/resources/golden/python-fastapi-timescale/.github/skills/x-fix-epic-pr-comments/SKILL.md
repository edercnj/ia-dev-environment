---
name: x-fix-epic-pr-comments
description: >
  Discovers all PRs from an epic via execution-state.json, fetches and classifies
  review comments in batch, generates a consolidated findings report, applies fixes,
  and creates a single correction PR. Supports dry-run, explicit PR list fallback,
  and idempotent re-execution.
  Reference: `.github/skills/x-fix-epic-pr-comments/SKILL.md`
---

# Skill: Fix Epic PR Comments

## Purpose

Automates the complete cycle of addressing PR review comments across an entire epic for {{PROJECT_NAME}}. Instead of processing PRs one by one, this skill discovers all PRs from an epic's `execution-state.json`, fetches all review comments in batch, classifies them, generates a consolidated report, applies fixes, and creates a single correction PR.

## Triggers

- `/x-fix-epic-pr-comments 0024` -- fix comments on all PRs from epic 0024
- `/x-fix-epic-pr-comments 0024 --dry-run` -- generate report only, no fixes
- `/x-fix-epic-pr-comments 0024 --prs 143,144,145` -- fix comments on specific PRs only
- `/x-fix-epic-pr-comments 0024 --skip-replies` -- fix without replying to original comments
- `/x-fix-epic-pr-comments 0024 --include-suggestions` -- also fix suggestion-type comments

## Workflow

```
1. PARSE       -> Parse epic ID and flags
2. VALIDATE    -> Check prerequisites (epic dir, checkpoint, or --prs)
3. DISCOVER    -> Extract PR list from execution-state.json (or --prs)
4. IDEMPOTENCY -> Check for existing correction branch
5. FETCH       -> Batch-fetch review comments from all discovered PRs
6. CLASSIFY    -> Classify each comment (actionable/suggestion/question/praise/resolved)
7. REPORT      -> Generate consolidated findings report
8. FIX         -> Apply corrections for actionable comments
9. VERIFY      -> Compile + test after fixes
10. REPLY      -> Reply to original PR comments
11. PR         -> Create single correction PR
```

### Step 1 -- Input Parsing

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier |

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--dry-run` | boolean | `false` | Generate report only, no fixes |
| `--prs` | `List<Integer>` | (none) | Explicit PR list |
| `--skip-replies` | boolean | `false` | Skip replying to comments |
| `--include-suggestions` | boolean | `false` | Include suggestion-type comments |

### Step 3 -- PR Discovery

1. Read `execution-state.json` from the epic directory
2. Extract PR numbers from stories with `prNumber != null`
3. Sort by PR number ascending
4. If `--prs` provided, use explicit list instead

### Step 6 -- Classification

| Type | Description | Action |
|------|-------------|--------|
| **Actionable** | Clear request to change code | Implement fix |
| **Suggestion** | Optional improvement | Fix if `--include-suggestions` |
| **Question** | Clarification request | Skip (needs human) |
| **Praise** | Positive feedback | Skip |
| **Resolved** | Already addressed | Skip |

### Step 7 -- Consolidated Report

Generates markdown report at `plans/epic-{epicId}/reports/pr-comments-report.md` with:
- Summary by classification category
- Actionable and suggestion finding tables
- Questions requiring human response
- Recurring themes analysis

### Step 8 -- Fix Application

For each actionable finding:
1. Locate file and line from finding
2. Apply suggestion block or infer correction from comment
3. Verify compilation after each fix
4. Revert if compilation fails; continue to next finding

### Step 9 -- Verification

After all fixes, run full test suite. If tests fail, bisect to identify offending fix(es) and revert them.

### Step 11 -- PR Creation

Create single correction PR referencing all source PRs:
```bash
gh pr create --base main \
  --title "fix(epic-{epicId}): address PR review comments" \
  --body "{pr_body}"
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Epic directory not found | Abort with `EPIC_DIR_NOT_FOUND` |
| Checkpoint missing and no `--prs` | Abort with `CHECKPOINT_NOT_FOUND` |
| No valid PRs | Abort with `NO_VALID_PRS` |
| Fix causes compilation failure | Revert fix, mark as failed, continue |
| Fix causes test regression | Bisect, revert offending fix |
