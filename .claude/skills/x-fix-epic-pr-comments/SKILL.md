---
name: x-fix-epic-pr-comments
description: "Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--dry-run] [--prs N,M,...] [--skip-replies] [--include-suggestions]"
user-invocable: true
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Fix Epic PR Comments

## Purpose

Automates the complete cycle of addressing PR review comments across an entire epic for {{PROJECT_NAME}}. Instead of processing PRs one by one, this skill discovers all PRs from an epic's `execution-state.json`, fetches all review comments in batch, classifies them, generates a consolidated report, applies fixes, and creates a single correction PR.

## When to Use

- After completing an epic with `/x-dev-epic-implement` and receiving review comments on multiple PRs
- When an epic has 5+ PRs with accumulated review comments that need batch correction
- When you want a consolidated view of all review findings across an epic before fixing
- When you need to fix review comments from automated reviewers (Copilot, CodeRabbit) across multiple PRs

## Triggers

- `/x-fix-epic-pr-comments 0024` -- fix comments on all PRs from epic 0024
- `/x-fix-epic-pr-comments 0024 --dry-run` -- generate report only, no fixes
- `/x-fix-epic-pr-comments 0024 --prs 143,144,145` -- fix comments on specific PRs only
- `/x-fix-epic-pr-comments 0024 --skip-replies` -- fix without replying to original comments
- `/x-fix-epic-pr-comments 0024 --include-suggestions` -- also fix suggestion-type comments

## Workflow Overview

```
1. PARSE       -> Parse epic ID and flags
2. VALIDATE    -> Check prerequisites (epic dir, checkpoint, or --prs)
3. DISCOVER    -> Extract PR list from execution-state.json (or --prs)
4. IDEMPOTENCY -> Check for existing correction branch (RULE-010)
5. FETCH       -> Batch-fetch review comments from all discovered PRs (story-0026-0002)
6. CLASSIFY    -> Classify each comment using x-fix-pr-comments heuristics (story-0026-0002)
7. REPORT      -> Generate consolidated findings report (story-0026-0003)
8. FIX         -> Apply corrections for actionable comments (story-0026-0004)
9. VERIFY      -> Compile + test after fixes (story-0026-0004)
10. REPLY      -> Reply to original PR comments (story-0026-0005)
11. PR         -> Create single correction PR (story-0026-0004)
```

> **This story (story-0026-0001) implements steps 1-4.** Steps 5-11 are delivered by subsequent stories.

---

## Step 1 -- Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier, e.g., `0024` |

The epic ID is a required positional argument. If missing, abort immediately:

```
ERROR: Epic ID is required. Usage: /x-fix-epic-pr-comments [EPIC-ID] [flags]
```

If the epic ID does not match `^\d{4}$`, abort:

```
ERROR: Invalid epic ID format. Expected 4 digits (e.g., 0024). Got: {input}
```

### Optional Flags

| Flag | Type | Default | Description | Rule |
|------|------|---------|-------------|------|
| `--dry-run` | boolean | `false` | Generate consolidated report only, no fixes applied | RULE-007 |
| `--prs` | `List<Integer>` | (none) | Explicit PR list, overrides checkpoint discovery | RULE-006 |
| `--skip-replies` | boolean | `false` | Apply fixes without replying to original PR comments | -- |
| `--include-suggestions` | boolean | `false` | Include suggestion-type comments in fix scope (default: actionable only) | -- |

### Flag Precedence

| Condition | Behavior |
|-----------|----------|
| `--prs` provided + checkpoint exists | `--prs` takes precedence (override) |
| `--prs` provided + no checkpoint | Use explicit PR list |
| No `--prs` + checkpoint exists | Discover PRs from checkpoint |
| No `--prs` + no checkpoint | Abort with `CHECKPOINT_NOT_FOUND` error |

---

## Step 2 -- Prerequisite Checks

Validate prerequisites in order. Abort on first failure.

### Path A: Without `--prs` flag (checkpoint-based discovery)

**2A.1. Epic Directory**

Check that `plans/epic-XXXX/` exists (or variant with suffix, e.g., `plans/epic-XXXX-*`):

```bash
# Check for exact match or suffix variant
ls -d plans/epic-{epicId}/ plans/epic-{epicId}-*/ 2>/dev/null | head -1
```

If not found:

```
ERROR: Directory plans/epic-{epicId}/ not found. Run /x-story-epic-full first.
```

Error code: `EPIC_DIR_NOT_FOUND`

**2A.2. Execution State (Checkpoint)**

Check that `execution-state.json` exists in the epic directory (RULE-001):

```bash
ls plans/epic-{epicId}*/execution-state.json 2>/dev/null | head -1
```

If not found:

```
ERROR: execution-state.json not found in plans/epic-{epicId}*/. Use --prs flag to provide PR list explicitly.
```

Error code: `CHECKPOINT_NOT_FOUND`

### Path B: With `--prs` flag (explicit PR list)

**2B.1. Validate PR numbers**

All values must be positive integers:

```
ERROR: Invalid PR number in --prs list: {value}. Expected positive integers.
```

**2B.2. Validate at least one PR exists**

For each PR number, verify it exists on GitHub:

```bash
gh pr view {prNumber} --json state --jq '.state' 2>/dev/null
```

If no PR in the list is valid:

```
ERROR: No valid PRs found in the provided list.
```

Error code: `NO_VALID_PRS`

---

## Step 3 -- PR Discovery (RULE-001)

### From Checkpoint (default)

1. Read `execution-state.json` from the epic directory
2. Parse the `stories` object and extract each story entry
3. Filter stories where `prNumber != null`
4. For each qualifying story, build a PR record:

```json
{
  "prNumber": 146,
  "storyId": "story-0024-0001",
  "prMergeStatus": "MERGED",
  "prUrl": "https://github.com/{owner}/{repo}/pull/146"
}
```

5. Sort PR records by `prNumber` ascending
6. Log discovery result:

```
Discovered {N} PRs from epic {epicId}: #{pr1}, #{pr2}, ..., #{prN}
```

### From Explicit List (`--prs` flag, RULE-006)

1. Parse comma-separated PR numbers from `--prs` flag
2. For each PR number, fetch state via GitHub CLI:

```bash
gh pr view {prNumber} --json number,state,title --jq '{number: .number, state: .state, title: .title}'
```

3. Build PR records with available information (storyId will be `unknown` for explicit PRs)
4. Filter out invalid/non-existent PRs with a warning:

```
WARNING: PR #{prNumber} not found, skipping.
```

5. Sort and log as above

### Discovery Output

The PR discovery step produces a list used by all subsequent steps:

```
PR Discovery Summary
====================
Epic: {epicId}
Source: checkpoint | explicit (--prs)
Total stories: {totalStories}
Stories with PR: {storiesWithPr}
Stories without PR: {storiesWithoutPr} (skipped)

PRs to process:
  #{pr1} (story-XXXX-YYYY) - MERGED
  #{pr2} (story-XXXX-YYYY) - MERGED
  ...
```

---

## Step 4 -- Idempotency Check (RULE-010)

Before proceeding to comment fetching, check if a correction branch already exists.

### Branch Detection

```bash
git branch -a | grep "fix/epic-{epicId}-pr-comments"
```

### Decision Matrix

| Condition | `--dry-run` | Action |
|-----------|-------------|--------|
| Branch does NOT exist | any | Proceed normally |
| Branch exists | `true` | Skip check, proceed with report generation only |
| Branch exists | `false` | Prompt user: `Branch fix/epic-{epicId}-pr-comments already exists. Update existing (u) or create new (n)?` |

### User Response Handling

| Response | Action |
|----------|--------|
| `u` (update) | Checkout existing branch, continue with incremental fixes |
| `n` (new) | Delete old branch, create fresh `fix/epic-{epicId}-pr-comments` |

---

## Error Handling

| Error Code | Condition | Message | Recovery |
|------------|-----------|---------|----------|
| `EPIC_DIR_NOT_FOUND` | Epic directory does not exist | `Directory plans/epic-{epicId}/ not found.` | Run `/x-story-epic-full` first |
| `CHECKPOINT_NOT_FOUND` | execution-state.json missing and no `--prs` | `execution-state.json not found. Use --prs flag.` | Provide `--prs` flag |
| `NO_VALID_PRS` | No PR in explicit list exists | `No valid PRs found in the provided list.` | Verify PR numbers |
| `INVALID_EPIC_ID` | Epic ID not 4 digits | `Invalid epic ID format. Expected 4 digits.` | Provide valid 4-digit ID |
| `INVALID_PR_NUMBER` | Non-integer in `--prs` list | `Invalid PR number: {value}.` | Fix the PR list |

---

## Integration Notes

### Dependency on x-fix-pr-comments

This skill reuses the classification heuristics from `/x-fix-pr-comments` (RULE-002). The classification categories are:

| Type | Heuristic Keywords | Action |
|------|-------------------|--------|
| **Actionable** | "please change", "should be", "must", "fix", "bug", "wrong" | Implement fix |
| **Suggestion** | "consider", "maybe", "could", "suggestion", "nit" | Fix if `--include-suggestions` |
| **Question** | "?", "why", "what", "how does" | Skip (human response needed) |
| **Praise** | "LGTM", "nice", "good", "great" | Skip |
| **Resolved** | Thread marked as resolved | Skip |

### execution-state.json Schema (Relevant Fields)

The checkpoint file follows this structure for PR discovery:

```json
{
  "epicId": "0024",
  "stories": {
    "story-XXXX-YYYY": {
      "id": "story-XXXX-YYYY",
      "status": "SUCCESS",
      "prNumber": 146,
      "prUrl": "https://github.com/{owner}/{repo}/pull/146",
      "prMergeStatus": "MERGED"
    }
  }
}
```

Only entries with `prNumber != null` are included in PR discovery. Stories with `status: FAILED` or `status: BLOCKED` that never created a PR are silently skipped.

### Cross-Rule Reference

| Rule | Title | How This Skill Implements It |
|------|-------|------------------------------|
| RULE-001 | Checkpoint as PR source | Reads `execution-state.json` to discover PRs |
| RULE-006 | Fallback without checkpoint | Accepts `--prs` flag for explicit PR list |
| RULE-007 | Mandatory dry-run support | `--dry-run` generates report without applying fixes |
| RULE-010 | Idempotency | Detects existing `fix/epic-{epicId}-pr-comments` branch |

### Future Steps (Subsequent Stories)

| Story | Step | Delivers |
|-------|------|----------|
| story-0026-0002 | Steps 5-6 | Batch comment fetching and classification |
| story-0026-0003 | Step 7 | Consolidated findings report |
| story-0026-0004 | Steps 8-9, 11 | Fix orchestration, verification, and PR creation |
| story-0026-0005 | Step 10 | Reply engine and status tracking |
