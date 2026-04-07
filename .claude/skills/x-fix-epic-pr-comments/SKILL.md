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

> **story-0026-0001 implements steps 1-4.** **story-0026-0002 implements steps 5-6 (including 6B).** Steps 7-11 are delivered by subsequent stories.

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

## Step 5 -- Batch Comment Fetching (RULE-002)

For each PR discovered in Step 3, fetch all review comments in batch. Two API calls per PR are required: inline (line-level) comments and review-level comments.

### 5.1 Fetch Inline Comments

```bash
gh api repos/{owner}/{repo}/pulls/{prNumber}/comments \
  --jq '.[] | {id: .id, path: .path, line: .line, body: .body, createdAt: .created_at, user: .user.login}'
```

Extract per comment:

| Field | Source | Description |
|-------|--------|-------------|
| `id` | `.id` | Unique comment ID |
| `path` | `.path` | File path relative to repo root |
| `line` | `.line` | Line number in the diff |
| `body` | `.body` | Full comment text (may contain markdown, suggestion blocks) |
| `createdAt` | `.created_at` | ISO-8601 timestamp |
| `user` | `.user.login` | Reviewer GitHub username |

### 5.2 Fetch Review-Level Comments

```bash
gh api repos/{owner}/{repo}/pulls/{prNumber}/reviews \
  --jq '.[] | select(.body != "" and .body != null) | {id: .id, body: .body, state: .state, user: .user.login}'
```

Extract per review (filter out reviews with empty body):

| Field | Source | Description |
|-------|--------|-------------|
| `id` | `.id` | Unique review ID |
| `body` | `.body` | Review-level comment text |
| `state` | `.state` | Review state (`APPROVED`, `CHANGES_REQUESTED`, `COMMENTED`, `DISMISSED`) |
| `user` | `.user.login` | Reviewer GitHub username |

### 5.3 Rate Limiting

GitHub API allows 5,000 requests per hour for authenticated users. With 2 calls per PR, a 16-PR epic consumes 32 requests (well within limits). For larger epics:

1. Before each API call, check remaining rate limit:
   ```bash
   gh api rate_limit --jq '.resources.core.remaining'
   ```

2. If remaining < 100, check reset time:
   ```bash
   gh api rate_limit --jq '.resources.core.reset'
   ```

3. If HTTP 429 (Too Many Requests) is returned:
   - Read `Retry-After` header value (seconds)
   - Log: `Rate limit hit. Waiting {seconds}s before retry.`
   - Pause execution for the indicated duration
   - Retry the failed request

4. Maximum 3 retry attempts per API call. After 3 failures, skip the PR with warning.

### 5.4 Timeout Handling

Each PR has a 30-second timeout for the complete fetch cycle (both inline and review calls combined):

- If timeout is reached before both calls complete:
  ```
  WARNING: Timeout fetching comments for PR #{prNumber} (30s exceeded). Skipping.
  ```
- Continue processing remaining PRs
- Record the skipped PR in the output with `status: "TIMEOUT"`

### 5.5 Fetch Output

After processing all PRs, produce a raw comment list:

```
Comment Fetch Summary
=====================
Total PRs processed: {processed}/{total}
Total inline comments: {inlineCount}
Total review comments: {reviewCount}
Total raw comments: {inlineCount + reviewCount}
Skipped PRs (timeout): {timeoutCount}
Skipped PRs (error): {errorCount}
```

---

## Step 6 -- Classification Engine (RULE-002)

Classify each fetched comment using the heuristics from `/x-fix-pr-comments`. This engine applies deterministic rules in priority order to assign exactly one classification per comment.

### 6.1 Classification Categories

| Priority | Type | Description | Default Action |
|----------|------|-------------|----------------|
| 1 (highest) | **Resolved** | Thread marked as resolved by reviewer or author | Skip |
| 2 | **Actionable** | Clear request to change code; has suggestion block | Implement fix |
| 3 | **Question** | Reviewer asking for clarification | Skip (human response) |
| 4 | **Suggestion** | Optional improvement, not a hard requirement | Fix if `--include-suggestions` |
| 5 (lowest) | **Praise** | Positive feedback, approval | Skip |

### 6.2 Heuristic Rules

Each rule is evaluated in priority order. The **first match wins** -- once a comment is classified, no further rules are evaluated.

**Priority 1 -- Resolved:**
- The comment thread is marked as `resolved` in the GitHub API response
- If resolved, classification is `resolved` regardless of body content

**Priority 2 -- Actionable:**
- Body contains a GitHub suggestion block (` ```suggestion `)
- Body matches any of (case-insensitive): `"please change"`, `"should be"`, `"must"`, `"fix"`, `"bug"`, `"wrong"`
- Review state is `CHANGES_REQUESTED` and body contains specific change request

**Priority 3 -- Question:**
- Body contains `"?"` AND at least one of: `"why"`, `"what"`, `"how does"`, `"how do"`, `"how is"`
- Body contains `"?"` with interrogative sentence structure (not just rhetorical `?` in code examples)

**Priority 4 -- Suggestion:**
- Body matches any of (case-insensitive): `"consider"`, `"maybe"`, `"could"`, `"suggestion"`, `"nit"`
- Body contains `"might want to"`, `"would be nice"`, `"alternatively"`

**Priority 5 -- Praise:**
- Body matches any of (case-insensitive): `"LGTM"`, `"nice"`, `"good"`, `"great"`, `"looks good"`, `"well done"`, `"clean"`

**Fallback:** If no heuristic matches, classify as `suggestion` (conservative default -- ensures human review).

### 6.3 Suggestion Block Extraction

When a comment contains a GitHub suggestion block, extract the suggested code:

```
```suggestion
suggested code here
`` `
```

Parse the suggestion block and store in the finding:
- `hasSuggestion: true`
- `suggestionCode`: the raw content between the suggestion fences

Comments with suggestion blocks are **always classified as actionable** (Priority 2), even if the body text alone would match a lower-priority rule.

### 6.4 Classification Output

After classifying all comments, produce a classification summary:

```
Classification Summary
======================
Total comments classified: {total}
  Actionable: {count} ({percentage}%)
  Suggestion: {count} ({percentage}%)
  Question:   {count} ({percentage}%)
  Praise:     {count} ({percentage}%)
  Resolved:   {count} ({percentage}%)
```

---

## Step 6B -- Deduplication Cross-PR (RULE-005)

Comments that are identical or near-identical may appear across multiple PRs (e.g., when golden files are copied across profiles, or the same code pattern repeats in multiple stories). Deduplication prevents redundant fixes.

### 6B.1 Fingerprint Algorithm

For each classified comment, compute a fingerprint:

```
fingerprint = SHA-256( normalize(body) + "|" + basename(path) )
```

Where:
- `normalize(body)` = lowercase, collapse all whitespace to single space, strip leading/trailing whitespace, remove line number references (e.g., `line 42`, `L42`, `:42`)
- `basename(path)` = filename without directory path (e.g., `SKILL.md` from `skills/x-review/SKILL.md`)

Using `basename` instead of full path ensures comments on the same file across different profile directories are recognized as duplicates.

### 6B.2 Grouping

1. Group all classified comments by fingerprint
2. For groups with size > 1 (duplicates found):
   - Keep the **first occurrence** (lowest PR number) as the canonical entry
   - Set `sourcePRs` to the PR where the comment was first posted
   - Set `affectedPRs` to ALL PRs where this fingerprint appears
   - Increment `duplicatesRemoved` counter by `(group_size - 1)`
3. For groups with size == 1 (unique):
   - `sourcePRs` and `affectedPRs` both contain only the originating PR

### 6B.3 Deduplication Output

```
Deduplication Summary
=====================
Total classified comments: {totalBefore}
Unique findings after dedup: {uniqueAfter}
Duplicates removed: {duplicatesRemoved}
Dedup ratio: {percentage}%

Top duplicate clusters:
  - "{truncated_body}" (fingerprint: {short_hash}) -> {count} occurrences across PRs #{pr1}, #{pr2}, ...
  - ...
```

---

## Consolidated Data Structure

After Steps 5, 6, and 6B, the skill produces a single consolidated JSON structure used by all subsequent steps (report generation, fix application, reply engine).

### Schema

```json
{
  "epicId": "XXXX",
  "fetchedAt": "2024-01-15T10:30:00Z",
  "totalPRs": 16,
  "processedPRs": 15,
  "skippedPRs": [
    { "prNumber": 999, "reason": "TIMEOUT" }
  ],
  "totalComments": 59,
  "uniqueFindings": 34,
  "findings": [
    {
      "id": "F-001",
      "fingerprint": "abc123def456...",
      "classification": "actionable",
      "file": "path/to/file.md",
      "line": 47,
      "body": "Original comment text from reviewer...",
      "hasSuggestion": true,
      "suggestionCode": "### LGPD (Lei Geral de Protecao de Dados Pessoais)",
      "sourcePRs": [143],
      "affectedPRs": [143, 148, 150],
      "reviewer": "copilot-pull-request-reviewer[bot]",
      "theme": null,
      "createdAt": "2024-01-10T08:00:00Z"
    }
  ],
  "summary": {
    "actionable": 34,
    "suggestion": 33,
    "question": 0,
    "praise": 0,
    "resolved": 0,
    "duplicatesRemoved": 25
  }
}
```

### Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `epicId` | `string` | Yes | 4-digit zero-padded epic identifier |
| `fetchedAt` | `string` (ISO-8601) | Yes | Timestamp when fetch was executed |
| `totalPRs` | `integer` | Yes | Total PRs discovered in Step 3 |
| `processedPRs` | `integer` | Yes | PRs successfully fetched (totalPRs - skipped) |
| `skippedPRs` | `array` | Yes | PRs skipped due to timeout or error |
| `totalComments` | `integer` | Yes | Raw comment count before deduplication |
| `uniqueFindings` | `integer` | Yes | Unique findings after deduplication |
| `findings[].id` | `string` | Yes | Sequential finding ID (`F-001`, `F-002`, ...) |
| `findings[].fingerprint` | `string` | Yes | SHA-256 hash for deduplication |
| `findings[].classification` | `enum` | Yes | One of: `actionable`, `suggestion`, `question`, `praise`, `resolved` |
| `findings[].file` | `string` | Yes | File path from the comment (relative to repo root) |
| `findings[].line` | `integer` | No | Line number (null for review-level comments) |
| `findings[].body` | `string` | Yes | Full original comment body |
| `findings[].hasSuggestion` | `boolean` | Yes | Whether comment contains a suggestion block |
| `findings[].suggestionCode` | `string` | No | Extracted suggestion code (null if no suggestion block) |
| `findings[].sourcePRs` | `array<integer>` | Yes | PR(s) where the comment was originally posted |
| `findings[].affectedPRs` | `array<integer>` | Yes | All PRs affected by this finding (after dedup) |
| `findings[].reviewer` | `string` | Yes | GitHub username of the reviewer |
| `findings[].theme` | `string` | No | Thematic grouping (set by report step, null during fetch) |
| `findings[].createdAt` | `string` (ISO-8601) | Yes | When the comment was originally posted |
| `summary.actionable` | `integer` | Yes | Count of actionable findings |
| `summary.suggestion` | `integer` | Yes | Count of suggestion findings |
| `summary.question` | `integer` | Yes | Count of question findings |
| `summary.praise` | `integer` | Yes | Count of praise findings |
| `summary.resolved` | `integer` | Yes | Count of resolved findings |
| `summary.duplicatesRemoved` | `integer` | Yes | Number of duplicate comments removed |

### Data Flow

```
Step 5 (Fetch) -> raw comments[] -> Step 6 (Classify) -> classified comments[]
  -> Step 6B (Dedup) -> unique findings[] -> Consolidated JSON
  -> Step 7 (Report) reads this JSON
  -> Step 8 (Fix) reads this JSON
  -> Step 10 (Reply) reads this JSON
```

---

## Error Handling

| Error Code | Condition | Message | Recovery |
|------------|-----------|---------|----------|
| `EPIC_DIR_NOT_FOUND` | Epic directory does not exist | `Directory plans/epic-{epicId}/ not found.` | Run `/x-story-epic-full` first |
| `CHECKPOINT_NOT_FOUND` | execution-state.json missing and no `--prs` | `execution-state.json not found. Use --prs flag.` | Provide `--prs` flag |
| `NO_VALID_PRS` | No PR in explicit list exists | `No valid PRs found in the provided list.` | Verify PR numbers |
| `INVALID_EPIC_ID` | Epic ID not 4 digits | `Invalid epic ID format. Expected 4 digits.` | Provide valid 4-digit ID |
| `INVALID_PR_NUMBER` | Non-integer in `--prs` list | `Invalid PR number: {value}.` | Fix the PR list |
| `RATE_LIMIT_EXCEEDED` | GitHub API rate limit hit after 3 retries | `Rate limit exceeded after 3 retries for PR #{prNumber}.` | Wait for rate limit reset or reduce PR count |
| `FETCH_TIMEOUT` | PR comment fetch exceeded 30s | `Timeout fetching comments for PR #{prNumber}.` | PR is skipped; retry manually if needed |
| `API_ERROR` | GitHub API returned non-200/non-429 status | `API error ({status}) fetching PR #{prNumber}: {message}` | Check GitHub token permissions |

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
| RULE-002 | Classification consistency | Reuses `/x-fix-pr-comments` heuristics with priority-ordered matching (Step 6) |
| RULE-005 | Deduplication cross-PR | SHA-256 fingerprint on normalized body + basename deduplicates across PRs (Step 6B) |
| RULE-006 | Fallback without checkpoint | Accepts `--prs` flag for explicit PR list |
| RULE-007 | Mandatory dry-run support | `--dry-run` generates report without applying fixes |
| RULE-010 | Idempotency | Detects existing `fix/epic-{epicId}-pr-comments` branch |

### Future Steps (Subsequent Stories)

| Story | Step | Delivers | Status |
|-------|------|----------|--------|
| story-0026-0002 | Steps 5-6, 6B | Batch comment fetching, classification, and deduplication | **Delivered** |
| story-0026-0003 | Step 7 | Consolidated findings report | Pending |
| story-0026-0004 | Steps 8-9, 11 | Fix orchestration, verification, and PR creation | Pending |
| story-0026-0005 | Step 10 | Reply engine and status tracking | Pending |
