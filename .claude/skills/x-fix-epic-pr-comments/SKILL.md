---
name: x-fix-epic-pr-comments
description: "Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, AskUserQuestion
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

> **story-0026-0001 implements steps 1-4.** **story-0026-0002 implements steps 5-6 (including 6B).** **story-0026-0003 implements step 7.** **story-0026-0004 implements steps 8-9 and 11.** **story-0026-0005 implements step 10 (reply engine and status tracking).**

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
branch="fix/epic-{epicId}-pr-comments"
if git show-ref --verify --quiet "refs/heads/$branch" || git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  echo "Branch exists"
else
  echo "Branch does not exist"
fi
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

3. If a rate-limit response is returned, retry using header-based backoff when available:
   - **HTTP 403 with `X-RateLimit-Remaining: 0`** = primary GitHub rate limit exhausted
     - Read `X-RateLimit-Reset` (UTC epoch seconds)
     - Compute wait time as `max(reset_time - current_time, 0)`
     - Log: `Primary rate limit hit. Waiting until reset ({seconds}s) before retry.`
     - Pause execution for the computed duration
     - Retry the failed request
   - **HTTP 429 (Too Many Requests)** = secondary rate limiting / throttling
     - Read `Retry-After` header value (seconds) if present
     - If `Retry-After` is present, wait that duration before retrying
     - Otherwise, use a short exponential backoff and retry
     - Log: `Secondary rate limit hit. Waiting {seconds}s before retry.`
   - Prefer response headers (`X-RateLimit-Reset`, `Retry-After`) over fixed delays whenever available
   - Treat these rate-limit responses as retryable API errors, not generic failures

4. Maximum 3 retry attempts per API call for rate-limit/API-error retries. After 3 failures, skip the PR with warning.

### 5.4 Timeout Handling

Each PR has a 30-second timeout for the complete fetch cycle (both inline and review calls combined):

- If timeout is reached before both calls complete:
  ```
  WARNING: Timeout fetching comments for PR #{prNumber} (30s exceeded). Skipping.
  ```
- Continue processing remaining PRs
- Record the skipped PR in the output with `reason: "TIMEOUT"`

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

````
```suggestion
suggested code here
```
````

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
    "actionable": 18,
    "suggestion": 12,
    "question": 3,
    "praise": 1,
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
  -> Step 8 (Fix) reads this JSON -> Step 9 (Verify) -> Step 11 (PR)
  -> Step 10 (Reply) reads this JSON + Fix Result
```

---

## Step 7 -- Consolidated Findings Report (RULE-004)

After classification (Step 6) and deduplication (Step 6B), generate a markdown report consolidating all findings. This report is persisted BEFORE any corrections begin (RULE-004) and serves as an audit artifact. In `--dry-run` mode (RULE-007), the report is the final output -- no subsequent steps execute.

### 7.1 Theme Detection

Before generating the report, assign a `theme` to each finding using deterministic heuristics. Rules are evaluated in order; the **first match wins**.

| Theme | Heuristic (case-insensitive) | Match Target |
|-------|------------------------------|--------------|
| `naming` | body contains `"rename"`, `"naming"`, `"name"`, `"diacritics"` | `body` |
| `placeholder` | body contains `"placeholder"`, `{{`, `"ambiguous"` | `body` |
| `consistency` | body contains `"inconsistent"`, `"standardize"`, `"align"` | `body` |
| `testing` | file path contains `Test.java`, `test/`, `spec` | `file` |
| `golden-files` | file path contains `golden/` | `file` |
| `security` | body contains `"security"`, `"OWASP"`, `"injection"`, `"XSS"` | `body` |
| `other` | no heuristic matched | fallback |

**Implementation notes:**
- All keyword matches are **case-insensitive**.
- `body` refers to the finding's `body` field (the original comment text).
- `file` refers to the finding's `file` field (relative path from repo root).
- The `{{` literal match for `placeholder` theme checks for double-brace template variables (e.g., `{{PROJECT_NAME}}`).
- When a finding matches both a `body`-based and a `file`-based heuristic, the `body`-based match wins (body heuristics have higher priority since they appear first in the evaluation order).
- After theme detection, update each finding's `theme` field in the consolidated data structure (was `null` during fetch).

### 7.2 Report Format (Markdown Template)

The report follows this exact structure:

````markdown
# PR Review Comments -- Consolidated Report

- **Epic:** EPIC-{epicId}
- **Date:** {YYYY-MM-DD}
- **PRs Analyzed:** {totalPRs}
- **Total Comments:** {totalComments}
- **Unique Findings:** {uniqueFindings} (after deduplication)

## Summary

| Category | Count | % |
|----------|-------|---|
| Actionable | {N} | {X}% |
| Suggestion | {N} | {X}% |
| Question | {N} | {X}% |
| Praise | {N} | {X}% |
| Resolved | {N} | {X}% |
| Duplicates Removed | {N} | -- |

## Actionable Findings

| # | PRs | File | Line | Summary | Has Suggestion | Theme |
|---|-----|------|------|---------|----------------|-------|
| 1 | #{pr1},#{pr2} | {file} | {line} | {truncated body, max 80 chars} | Yes/No | {theme} |

> If no actionable findings: display `(none)` row.

## Suggestion Findings

| # | PRs | File | Line | Summary | Theme |
|---|-----|------|------|---------|-------|
| 1 | #{pr1} | {file} | {line} | {truncated body, max 80 chars} | {theme} |

> If no suggestion findings: display `(none)` row.

## Questions Requiring Human Response

| # | PR | File | Line | Reviewer | Question |
|---|-----|------|------|----------|----------|
| 1 | #{pr1} | {file} | {line} | {reviewer} | {truncated body, max 120 chars} |

> If no questions: display `(none)` row.

## Recurring Themes

| Theme | Count | Affected PRs | Description |
|-------|-------|--------------|-------------|
| {theme} | {N} | #{pr1}, #{pr2} | {auto-generated description} |

> Only themes with count >= 1 appear. `other` theme is listed last if present.

## Dry-Run Summary

> This section is ONLY included when `--dry-run` is active.

Fixes NOT applied. Review the actionable findings above and re-run without --dry-run to apply corrections.

- Actionable findings ready for fix: {actionableCount}
- Suggestion findings (requires --include-suggestions): {suggestionCount}
- Questions requiring human response: {questionCount}
````

### 7.3 Report Field Mapping

| Report Field | Source (from Consolidated JSON) | Transformation |
|--------------|-------------------------------|----------------|
| `Epic` | `epicId` | Prefix with `EPIC-` |
| `Date` | Current date | `YYYY-MM-DD` format |
| `PRs Analyzed` | `totalPRs` | Integer |
| `Total Comments` | `totalComments` | Integer |
| `Unique Findings` | `uniqueFindings` | Integer |
| `Category Count` | `summary.{category}` | Integer per category |
| `Category %` | `summary.{category} / uniqueFindings * 100` | Round to 1 decimal place. If `uniqueFindings == 0`, display `0.0%` |
| `Duplicates Removed` | `summary.duplicatesRemoved` | Integer, percentage column shows `--` |
| `PRs` (in finding tables) | `finding.affectedPRs` | Comma-separated with `#` prefix |
| `File` | `finding.file` | As-is (relative path) |
| `Line` | `finding.line` | Integer or `--` if null |
| `Summary` | `finding.body` | Truncated to 80 chars (120 for questions), append `...` if truncated |
| `Has Suggestion` | `finding.hasSuggestion` | `Yes` or `No` |
| `Theme` | `finding.theme` | As detected in Step 7.1 |
| `Reviewer` | `finding.reviewer` | GitHub username |

### 7.4 Theme Description Auto-Generation

Each theme in the "Recurring Themes" table includes an auto-generated description:

| Theme | Description |
|-------|-------------|
| `naming` | Inconsistent placeholder or entity naming conventions |
| `placeholder` | Ambiguous or incorrect placeholder/template variables |
| `consistency` | Inconsistent patterns requiring standardization |
| `testing` | Test-related improvements or corrections |
| `golden-files` | Golden file inconsistencies (typically auto-fixable) |
| `security` | Security-related findings (OWASP, injection, XSS) |
| `other` | Uncategorized findings requiring manual review |

### 7.5 Persistence (RULE-004)

1. Resolve the epic directory path (`{epicDir}`) from Step 2. This accounts for suffix variants (e.g., `plans/epic-{epicId}-description/`).

2. Create directory `{epicDir}/reports/` if it does not exist:
   ```bash
   mkdir -p {epicDir}/reports/
   ```

3. Write the report to: `{epicDir}/reports/pr-comments-report.md`

4. **Timing constraint:** The report file MUST be written to disk BEFORE any fix operations (Step 8) begin. This ensures the report serves as a pre-correction audit artifact.

5. **Idempotency:** If `pr-comments-report.md` already exists (from a previous execution), overwrite it with the updated report. No backup of the previous version is created.

6. Log after persistence:
   ```
   Report saved to {epicDir}/reports/pr-comments-report.md
   ```

### 7.6 Dry-Run Integration (RULE-007)

When `--dry-run` is active:

1. Execute Steps 1-7 normally (parse, validate, discover, idempotency, fetch, classify, report).
2. Generate the full report including the "Dry-Run Summary" section.
3. **STOP execution after Step 7.** Do NOT proceed to Step 8 (Fix), Step 9 (Verify), Step 10 (Reply), or Step 11 (PR).
4. Output the report path and summary to the user:
   ```
   DRY-RUN COMPLETE
   ================
   Report: plans/epic-{epicId}/reports/pr-comments-report.md
   Actionable: {N} findings ready for fix
   Suggestions: {N} findings (use --include-suggestions to fix)
   Questions: {N} findings requiring human response
   
   Re-run without --dry-run to apply corrections.
   ```

When `--dry-run` is NOT active:
1. Generate the report WITHOUT the "Dry-Run Summary" section.
2. Persist the report (Step 7.5).
3. Proceed to Step 8 (Fix).

### 7.7 Report Output Data Contract

The report step produces the following output used by subsequent steps:

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `reportPath` | `String` | Yes | Absolute path to the generated report file |
| `actionableCount` | `Integer` | Yes | Total actionable findings in the report |
| `suggestionCount` | `Integer` | Yes | Total suggestion findings in the report |
| `questionCount` | `Integer` | Yes | Total question findings requiring human response |
| `themes` | `List<Theme>` | Yes | Detected themes with counts |
| `themes[].name` | `String` | Yes | Theme identifier (e.g., `naming`, `placeholder`) |
| `themes[].count` | `Integer` | Yes | Number of findings with this theme |
| `themes[].affectedPRs` | `List<Integer>` | Yes | Union of all affected PRs for findings in this theme |
| `dryRun` | `boolean` | Yes | Whether `--dry-run` was active |

### 7.8 Edge Cases

| Scenario | Behavior |
|----------|----------|
| Zero findings (no comments on any PR) | Report generated with all counts = 0, empty finding tables show `(none)`, themes table is empty |
| All findings are `praise` or `resolved` | Actionable and Suggestion tables show `(none)`, Recurring Themes may be empty |
| Single PR analyzed | PRs column shows single `#{prNumber}`, no comma separation needed |
| Finding body contains markdown special chars | Escape `|` as `\|` in table cells to prevent markdown rendering issues |
| Finding body contains newlines | Replace newlines with spaces before truncation |
| `--dry-run` + `--include-suggestions` | Report includes Dry-Run Summary; suggestion count reflects all suggestions (informational only, no fixes applied) |

### 7.9 Error Handling (Report-Specific)

| Error Code | Condition | Message | Recovery |
|------------|-----------|---------|----------|
| `REPORT_DIR_CREATE_FAILED` | Cannot create `reports/` directory | `Failed to create reports directory: {path}` | Check filesystem permissions |
| `REPORT_WRITE_FAILED` | Cannot write report file | `Failed to write report to: {path}` | Check disk space and permissions |
| `NO_FINDINGS_TO_REPORT` | Zero findings after dedup (not an error) | (no error -- generate empty report) | Normal flow continues |

---

## Step 8 -- Fix Orchestration Engine (RULE-003, RULE-010)

After the consolidated report is persisted (Step 7), apply corrections for all actionable findings. In `--dry-run` mode, this step is skipped entirely (execution stops after Step 7).

### 8.1 Pre-Condition Check

Before starting fixes, verify there are actionable findings to process:

```
if actionableFindings.size == 0:
  log "No actionable findings to fix. Skipping Steps 8-11."
  return {
    fixesApplied: 0,
    fixesFailed: 0,
    fixesSkipped: 0,
    fixBranch: null,
    testsPass: null,
    commitCount: 0,
    commits: [],
    findings: [],
    prUrl: null,
    prNumber: null
  }
```

If `--include-suggestions` is active, also include findings with `classification == "suggestion"` in the fix scope.

### 8.2 Branch Creation and Setup (RULE-010)

Create the correction branch from `main`:

```bash
git checkout main
git pull origin main
git checkout -b fix/epic-{epicId}-pr-comments
```

**Idempotency handling** (from Step 4 decision):

| Condition | Action |
|-----------|--------|
| Branch does not exist | Create new branch from `main` |
| Branch exists, user chose `u` (update) | `git checkout fix/epic-{epicId}-pr-comments` and continue with incremental fixes |
| Branch exists, user chose `n` (new) | `git branch -D fix/epic-{epicId}-pr-comments` then create fresh from `main` |

After branch setup, log:

```
Branch fix/epic-{epicId}-pr-comments ready. Starting fix application.
```

### 8.3 Fix Application Loop

Process each finding in the fix scope, **ordered by file path** (ascending) to minimize context switches when working on the same file.

**Ordering rule:** Sort findings by `file` field lexicographically. When multiple findings target the same file, sort by `line` ascending (null lines go last).

For each finding:

#### 8.3.1 Locate File

```bash
test -f "{finding.file}"
```

If the file does not exist (deleted, renamed, or moved since the review comment was posted):

```
WARNING: File not found: {finding.file}. Skipping finding {finding.id}.
```

Mark finding as `fixStatus: "skipped"`, `fixReason: "file_not_found"`. Continue to next finding.

#### 8.3.2 Apply Correction

Two correction strategies based on finding properties:

**Strategy A -- Direct suggestion application** (when `hasSuggestion == true`):

1. Read the target file.
2. Locate the code region around `finding.line`.
3. Replace the existing code with the content from `finding.suggestionCode`.
4. This is a deterministic, mechanical replacement.

**Strategy B -- Context-inferred correction** (when `hasSuggestion == false`):

1. Read the target file.
2. Read the finding's `body` to understand the requested change.
3. Analyze the surrounding code context at `finding.line`.
4. Infer the appropriate correction based on the comment's intent.
5. Apply the inferred change.

**Important:** Strategy B requires LLM reasoning. If the comment is ambiguous and the correction cannot be confidently inferred, mark the finding as `fixStatus: "skipped"`, `fixReason: "ambiguous_comment"` and continue.

#### 8.3.3 Compile Verification (Per-Fix)

After each individual fix, verify compilation:

```bash
mvn -f java/pom.xml compile -q 2>&1
```

| Result | Action |
|--------|--------|
| Exit code 0 | Fix accepted. Continue to next finding. |
| Exit code != 0 | Revert the fix, mark as failed, continue. |

**Revert on compilation failure:**

```bash
git checkout -- {affected_files}
```

Mark finding as `fixStatus: "failed"`, `fixReason: "compilation_failure"`, `compileError: "{first 200 chars of error output}"`.

Log:

```
FIX FAILED (compilation): {finding.id} on {finding.file}:{finding.line}
  Error: {truncated compile error}
  Reverted.
```

Continue to the next finding.

#### 8.3.4 Fix Loop Output

After processing all findings, produce a fix application summary:

```
Fix Application Summary
=======================
Total findings in scope: {totalInScope}
  Applied successfully: {applied}
  Failed (compilation): {failedCompile}
  Skipped (file not found): {skippedNotFound}
  Skipped (ambiguous): {skippedAmbiguous}
```

---

## Step 9 -- Post-Correction Verification (RULE-009)

After ALL fixes are applied (Step 8 complete), run the full test suite to detect regressions that may not be caught by compilation alone.

### 9.1 Full Test Suite

```bash
mvn -f java/pom.xml test 2>&1
```

| Result | Action |
|--------|--------|
| Exit code 0 | All tests pass. Proceed to commit (Step 9.3). |
| Exit code != 0 | Test failure detected. Run bisect (Step 9.2). |

### 9.2 Simplified Bisect

When tests fail after the full batch of fixes, identify the offending fix(es) using a simplified bisect approach:

**Algorithm:**

1. Collect the list of all successfully applied fixes (ordered by application sequence).
2. Revert ALL fixes: `git checkout -- .`
3. Re-apply fixes one at a time, running `mvn -f java/pom.xml test -q` after each.
4. The first fix that causes test failure is the offending fix.
5. Revert the offending fix: `git checkout -- {affected_files}`
6. Mark it as `fixStatus: "failed"`, `fixReason: "test_regression"`.
7. Continue re-applying remaining fixes (skip the offending one).
8. If another fix fails tests, repeat steps 5-7.
9. After all fixes are re-applied (minus offending ones), run `mvn -f java/pom.xml test` one final time to confirm green.

**Performance note:** The bisect process may require up to N+1 test runs (where N = number of applied fixes). For large fix batches, this is acceptable because correctness is non-negotiable.

**Bisect output:**

```
Bisect Summary
==============
Total fixes bisected: {totalFixes}
Offending fixes found: {offendingCount}
  - {finding.id}: {finding.file}:{finding.line} -- test regression in {test_class}
Fixes retained: {retainedCount}
Final test status: PASS
```

### 9.3 Atomic Commits by Theme

After verification passes, commit fixes grouped by theme (from Step 7.1 theme detection). Each theme produces one commit:

**Commit strategy:**

1. Group all successfully applied fixes by `finding.theme`.
2. For each theme group:
   a. Stage only the files affected by fixes in this theme group: `git add {file1} {file2} ...`
   b. Commit with Conventional Commits format:
      ```
      fix(epic-{epicId}): {theme description}
      ```

**Theme-to-commit-message mapping:**

| Theme | Commit Message |
|-------|---------------|
| `naming` | `fix(epic-{epicId}): correct naming conventions` |
| `placeholder` | `fix(epic-{epicId}): resolve placeholder and template variables` |
| `consistency` | `fix(epic-{epicId}): standardize inconsistent patterns` |
| `testing` | `fix(epic-{epicId}): address test-related review comments` |
| `golden-files` | `fix(epic-{epicId}): update golden files` |
| `security` | `fix(epic-{epicId}): address security review comments` |
| `other` | `fix(epic-{epicId}): address miscellaneous review comments` |

3. If all fixes belong to the same theme, produce a single commit.
4. If fixes span multiple themes, produce multiple commits (one per theme, in theme name alphabetical order).

**Commit output:**

```
Commits Created
===============
  1. fix(epic-{epicId}): correct naming conventions (3 files, 5 findings)
  2. fix(epic-{epicId}): resolve placeholder and template variables (2 files, 3 findings)
Total commits: 2
```

---

## Step 8-GF -- Golden File Handling

This is an execution-time sub-step of **Step 8**. Perform it during Step 8 fix application, immediately after identifying and applying the relevant source-template fix, and **before any Step 9 verification or commit creation**. It is documented here only as a reference section; do not execute it after Step 9.

When a fix targets a source template that generates golden files, special handling is required to propagate the fix across all profiles.

### 8-GF.1 Golden File Detection

A finding targets a golden file when:

- `finding.file` contains `golden/` in the path, OR
- `finding.theme == "golden-files"`

A finding targets a source template when:

- `finding.file` contains `resources/` or `templates/` in the path, AND
- The file is referenced by the golden file generation process

### 8-GF.2 Source Template Fix

When the fix targets a source template:

1. Apply the fix to the source template file (as per Step 8.3.2).
2. Identify if a golden file regeneration mechanism exists:
   ```bash
   # Check for regeneration script or test
   find . \( -name 'GoldenFileRegenerator*' -o -name 'golden*regenerat*' \) 2>/dev/null
   ```
3. If regeneration is available: execute it to propagate the fix to all profiles.
4. If regeneration is NOT available: manually apply the same fix to all affected golden files across profiles.

### 8-GF.3 Direct Golden File Fix

When the fix targets a golden file directly:

1. Identify the corresponding source template (reverse-map from golden path to template path).
2. Apply the fix to the source template FIRST.
3. Regenerate or manually propagate to all profile variants.
4. Verify that golden file tests pass after regeneration:
   ```bash
   mvn -f java/pom.xml verify -q 2>/dev/null || mvn -f java/pom.xml test -q
   ```

### 8-GF.4 Profile Propagation

Golden files exist across multiple profiles. When a fix affects one profile's golden file:

1. Identify all profiles that share the same source template.
2. Apply the fix to the source template (single source of truth).
3. Regenerate golden files for ALL affected profiles.
4. Stage all modified golden files for commit.

**Log:**

```
Golden file propagation: {sourceTemplate} -> {profileCount} profiles updated
```

---

## Step 10 -- Reply Engine (RULE-008, RULE-010)

After fixes are applied and verified (Steps 8-9), post replies to the original PR comments to close the feedback loop. Replies use pt-BR templates (RULE-008) and are idempotent (RULE-010).

### 10.1 Pre-Condition: `--skip-replies` Flag

If `--skip-replies` is active, skip the entire reply phase:

```
Reply phase skipped (--skip-replies)
```

Return immediately with:

```json
{
  "repliesSent": 0,
  "repliesSkipped": 0,
  "repliesFailed": 0
}
```

### 10.2 Reply Templates (pt-BR -- RULE-008)

Each finding receives a reply based on its classification and fix status. Templates use pt-BR as mandated by RULE-008.

| Classification | Fix Status | Template | Reply? |
|----------------|-----------|----------|--------|
| Actionable | `applied` | `Corrigido no PR #{fixPR}. {descricao}. Commit: {shortSha}` | Yes |
| Actionable | `failed` | `Tentei corrigir, mas causou falha de compilacao/testes. Necessita intervencao manual.` | Yes |
| Actionable | `skipped` | `Arquivo nao encontrado no working tree atual. Possivelmente renomeado/deletado.` | Yes |
| Suggestion | `applied` | `Sugestao aceita no PR #{fixPR}. {descricao}. Commit: {shortSha}` | Yes |
| Suggestion | not applied | -- | **No** |
| Question | any | -- | **No** (requires human response) |
| Praise | any | -- | **No** |
| Resolved | any | -- | **No** |

**Template variable resolution:**

| Variable | Source | Example |
|----------|--------|---------|
| `{fixPR}` | `fixResult.prNumber` | `173` |
| `{descricao}` | First 80 chars of `finding.body`, truncated with `...` if needed | `Rename variable to follow conventions...` |
| `{shortSha}` | First 7 chars of `fixResult.commits[].sha` (matching the finding's theme commit) | `abc123d` |

### 10.3 Reply Mechanism

For each finding that requires a reply (per 10.2 table):

#### 10.3.1 Post Reply to Inline Comment

```bash
gh api repos/{owner}/{repo}/pulls/comments/{commentId}/replies \
  -f body="{resolvedTemplate}"
```

Where `{commentId}` comes from `finding.id` (the original review comment ID from Step 5). Note: the GitHub REST API reply endpoint does not include `{prNumber}` in the path -- the comment ID is globally unique.

#### 10.3.2 Post Reply to Review-Level Comment

For review-level comments (those without a `line` field):

```bash
gh api repos/{owner}/{repo}/pulls/{prNumber}/reviews/{reviewId}/comments \
  -f body="{resolvedTemplate}"
```

#### 10.3.3 Error Handling per Reply

| HTTP Status | Action |
|-------------|--------|
| 201 Created | Reply posted successfully. Increment `repliesSent`. |
| 404 Not Found | Comment or PR no longer exists. Log warning, increment `repliesSkipped`. |
| 422 Unprocessable | Invalid request (e.g., thread locked). Log warning, increment `repliesFailed`. |
| 429 Too Many Requests | Rate limit hit. Apply rate limiting (Step 10.4). |
| Other 4xx/5xx | Log error, increment `repliesFailed`. Continue to next finding. |

### 10.4 Rate Limiting (30 replies/minute)

GitHub enforces secondary rate limits on mutation endpoints. The reply engine enforces a ceiling of **30 replies per minute**.

**Algorithm:**

1. Maintain a counter `repliesInCurrentWindow` and a timestamp `windowStart`.
2. Before each reply:
   - If `repliesInCurrentWindow >= 30` AND `(now - windowStart) < 60 seconds`:
     - Calculate wait time: `waitSeconds = 60 - (now - windowStart)`
     - Log: `Rate limit ceiling reached (30/min). Pausing {waitSeconds}s.`
     - Sleep for `waitSeconds`
     - Reset counter: `repliesInCurrentWindow = 0`, `windowStart = now`
   - If `(now - windowStart) >= 60 seconds`:
     - Reset counter: `repliesInCurrentWindow = 0`, `windowStart = now`
3. After posting reply: increment `repliesInCurrentWindow`.

**On HTTP 429 from GitHub API:**

1. Read `Retry-After` header value (seconds). Default to 60s if header is absent.
2. Log: `GitHub rate limit hit (429). Waiting {retryAfter}s.`
3. Sleep for the indicated duration.
4. Retry the failed reply (max 3 retries per reply).
5. After 3 failures, mark reply as failed and continue.

### 10.5 Idempotency Check (RULE-010)

Before posting each reply, check if a reply already exists to prevent duplicates on re-execution.

**Check algorithm:**

1. Fetch existing replies for the comment:
   ```bash
   gh api repos/{owner}/{repo}/pulls/comments/{commentId}/replies \
     --jq '.[].body'
   ```

2. Scan reply bodies for the idempotency pattern:
   - For actionable (fixed) and suggestion (accepted): pattern `"Corrigido no PR #"` or `"Sugestao aceita no PR #"`
   - For actionable (failed): pattern `"Tentei corrigir, mas causou falha"`
   - For actionable (skipped): pattern `"Arquivo nao encontrado no working tree"`

3. If any existing reply matches the pattern:
   ```
   Reply already posted for comment {commentId}, skipping
   ```
   Increment `repliesSkipped`. Do NOT post a duplicate reply.

4. If no match found: proceed with posting the reply.

### 10.6 Status Tracking (Report Update)

After the reply phase completes, update the `pr-comments-report.md` with Status and Reply columns.

#### 10.6.1 Updated Report Table Format

The Actionable Findings table gains two new columns:

| # | PRs | File | Line | Summary | Has Suggestion | Theme | Status | Reply |
|---|-----|------|------|---------|----------------|-------|--------|-------|
| 1 | #{pr1},#{pr2} | {file} | {line} | {summary} | Yes/No | {theme} | Fixed | Replied |
| 2 | #{pr1} | {file} | {line} | {summary} | No | {theme} | Failed | Replied |
| 3 | #{pr1} | {file} | {line} | {summary} | Yes | {theme} | Skipped | -- |

**Status values:**

| Status | Meaning |
|--------|---------|
| `Fixed` | Fix applied successfully (`fixStatus == "applied"`) |
| `Failed` | Fix attempted but caused regression (`fixStatus == "failed"`) |
| `Skipped` | Fix not attempted (`fixStatus == "skipped"`) |
| `--` | Not in fix scope (question, praise, resolved) |

**Reply values:**

| Reply | Meaning |
|-------|---------|
| `Replied` | Reply posted successfully to PR comment |
| `Failed` | Reply attempted but API returned error |
| `Skipped` | Reply already existed (idempotency) |
| `--` | No reply applicable (question, praise, resolved, `--skip-replies`) |

#### 10.6.2 Suggestion Findings Table Update

When `--include-suggestions` is active, the Suggestion Findings table also gains Status and Reply columns:

| # | PRs | File | Line | Summary | Theme | Status | Reply |
|---|-----|------|------|---------|-------|--------|-------|
| 1 | #{pr1} | {file} | {line} | {summary} | {theme} | Accepted | Replied |

#### 10.6.3 Report Persistence

1. Re-read the existing `pr-comments-report.md` from Step 7.
2. Replace the Actionable Findings table with the updated version including Status and Reply columns.
3. Replace the Suggestion Findings table if `--include-suggestions` was active.
4. Write the updated report back to disk.
5. Log: `Report updated with Status and Reply columns: {reportPath}`

### 10.7 Reply Result Data Contract

The reply engine produces the following output:

```json
{
  "repliesSent": 8,
  "repliesSkipped": 2,
  "repliesFailed": 1,
  "replyDetails": [
    {
      "findingId": "F-001",
      "commentId": 123456789,
      "prNumber": 143,
      "status": "sent",
      "template": "Corrigido no PR #173. Rename variable to follow conventions. Commit: abc123d"
    },
    {
      "findingId": "F-002",
      "commentId": 123456790,
      "prNumber": 143,
      "status": "skipped",
      "reason": "reply_already_exists"
    },
    {
      "findingId": "F-003",
      "commentId": 123456791,
      "prNumber": 146,
      "status": "failed",
      "reason": "api_error_422"
    }
  ]
}
```

### Field Reference

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `repliesSent` | `Integer` | Yes | Count of replies successfully posted |
| `repliesSkipped` | `Integer` | Yes | Count of replies skipped (idempotency, question, praise, resolved) |
| `repliesFailed` | `Integer` | Yes | Count of replies that failed (API errors) |
| `replyDetails[]` | `Array` | Yes | Detail per finding processed |
| `replyDetails[].findingId` | `String` | Yes | Finding ID from consolidated JSON |
| `replyDetails[].commentId` | `Integer` | Yes | Original GitHub comment ID |
| `replyDetails[].prNumber` | `Integer` | Yes | PR where the reply was posted |
| `replyDetails[].status` | `Enum` | Yes | One of: `sent`, `skipped`, `failed` |
| `replyDetails[].template` | `String` | No | Resolved template text (only when `status == "sent"`) |
| `replyDetails[].reason` | `String` | No | Reason for skip/failure (only when `status != "sent"`) |

### Reply Status Values

| Status | Description | Possible Reasons |
|--------|-------------|------------------|
| `sent` | Reply posted successfully | -- |
| `skipped` | Reply not posted | `reply_already_exists`, `no_reply_applicable` (question, praise, resolved) |
| `failed` | Reply attempted but failed | `api_error_{status}`, `max_retries_exceeded` |

### 10.8 Reply Phase Output

After processing all findings, produce a reply phase summary:

```
Reply Phase Summary
===================
Total findings processed: {totalProcessed}
  Replies sent: {repliesSent}
  Replies skipped (idempotent): {skippedIdempotent}
  Replies skipped (no reply applicable): {skippedNoReply}
  Replies failed: {repliesFailed}
Rate limit pauses: {pauseCount}
Report updated: {reportPath}
```

### 10.9 Error Handling (Reply-Specific)

| Error Code | Condition | Message | Recovery |
|------------|-----------|---------|----------|
| `REPLY_API_ERROR` | GitHub API returned non-201 status | `Failed to post reply for comment {commentId}: HTTP {status}` | Reply marked as failed; loop continues |
| `REPLY_RATE_LIMITED` | HTTP 429 received | `GitHub rate limit hit. Waiting {retryAfter}s.` | Auto-retry after wait (max 3 retries) |
| `REPLY_MAX_RETRIES` | 3 retry attempts exhausted for a single reply | `Max retries (3) exceeded for comment {commentId}. Skipping.` | Reply marked as failed; loop continues |
| `REPLY_IDEMPOTENT_SKIP` | Existing reply detected matching pattern | `Reply already posted for comment {commentId}, skipping` | Normal flow -- idempotent skip |
| `REPORT_UPDATE_FAILED` | Cannot write updated report | `Failed to update report with Status/Reply columns: {path}` | Log error; reply results still returned |

### 10.10 Edge Cases

| Scenario | Behavior |
|----------|----------|
| Zero findings requiring reply | Reply phase completes instantly with all counters at 0. Report not modified. |
| `--skip-replies` active | Skip entire reply phase. Report retains original format without Status/Reply columns. |
| `--dry-run` active | Step 10 never executes (execution stops after Step 7). |
| Finding has multiple `sourcePRs` (dedup) | Reply posted only to `sourcePRs[0]` (the canonical occurrence). Other PRs do not receive replies. |
| PR was deleted between fix and reply phase | API returns 404. Reply marked as failed. Loop continues. |
| Comment thread is locked | API returns 422. Reply marked as failed. Loop continues. |
| 50+ replies needed | Rate limiter pauses at 30 replies/minute. Total time increases but all replies are posted. |

---

## Step 11 -- PR Creation (RULE-003)

After all fixes are committed (Step 9.3), create a single correction PR.

### 11.1 Push to Remote

```bash
git push -u origin fix/epic-{epicId}-pr-comments
```

If the push fails (e.g., branch already exists on remote from a previous run):

```bash
git push --force-with-lease origin fix/epic-{epicId}-pr-comments
```

### 11.2 Build PR Body

The PR body MUST reference all source PRs for traceability (RULE-003):

````markdown
## Summary

Fixes {fixedCount} actionable findings from {prCount} PRs.

Fixes comments from #{pr1}, #{pr2}, #{pr3}, ...

Part of EPIC-{epicId}

## Findings Fixed

| # | Finding | File | Line | Theme | Source PRs |
|---|---------|------|------|-------|------------|
| 1 | {finding.id} | {file} | {line} | {theme} | #{sourcePR1}, #{sourcePR2}, ... |
| 2 | ... | ... | ... | ... | ... |

## Findings Skipped

| # | Finding | File | Reason |
|---|---------|------|--------|
| 1 | {finding.id} | {file} | {fixReason} |

## Findings Failed

| # | Finding | File | Reason | Error |
|---|---------|------|--------|-------|
| 1 | {finding.id} | {file} | {fixReason} | {truncated error} |

## Verification

- Compilation: PASS
- Tests: PASS
- Commits: {commitCount}
````

### 11.3 Create PR

```bash
gh pr create --base main \
  --title "fix(epic-{epicId}): address PR review comments" \
  --body "{pr_body}"
```

Capture the PR URL and number from the output:

```bash
gh pr view --json number,url --jq '{number: .number, url: .url}'
```

### 11.4 PR Output

```
PR Created
==========
URL: {prUrl}
Number: #{prNumber}
Title: fix(epic-{epicId}): address PR review comments
Base: main
Branch: fix/epic-{epicId}-pr-comments
Commits: {commitCount}
Findings fixed: {fixedCount}
Findings skipped: {skippedCount}
Findings failed: {failedCount}
```

---

## Fix Result Data Contract

The fix engine (Steps 8-9, 11) produces the following output:

### Schema

```json
{
  "fixBranch": "fix/epic-{epicId}-pr-comments",
  "prUrl": "https://github.com/{owner}/{repo}/pull/{prNumber}",
  "prNumber": 159,
  "fixesApplied": 8,
  "fixesFailed": 1,
  "fixesSkipped": 2,
  "testsPass": true,
  "commitCount": 3,
  "commits": [
    {
      "sha": "abc123",
      "message": "fix(epic-{epicId}): correct naming conventions",
      "theme": "naming",
      "findingsCount": 5,
      "filesChanged": 3
    }
  ],
  "findings": [
    {
      "id": "F-001",
      "fixStatus": "applied",
      "fixReason": null,
      "compileError": null
    },
    {
      "id": "F-002",
      "fixStatus": "failed",
      "fixReason": "compilation_failure",
      "compileError": "error: ';' expected at line 42"
    },
    {
      "id": "F-003",
      "fixStatus": "skipped",
      "fixReason": "file_not_found",
      "compileError": null
    }
  ]
}
```

### Field Reference

| Field | Type | Always Present | Description |
|-------|------|----------------|-------------|
| `fixBranch` | `String` | Yes | Name of the correction branch |
| `prUrl` | `String` | No | URL of the created PR (null if no fixes applied) |
| `prNumber` | `Integer` | No | PR number (null if no fixes applied) |
| `fixesApplied` | `Integer` | Yes | Count of successfully applied fixes |
| `fixesFailed` | `Integer` | Yes | Count of fixes that failed compilation or test verification |
| `fixesSkipped` | `Integer` | Yes | Count of skipped fixes (file not found, ambiguous) |
| `testsPass` | `Boolean` | Yes | Whether the final test suite passed after all fixes |
| `commitCount` | `Integer` | Yes | Number of commits created (one per theme) |
| `commits[]` | `Array` | Yes | Details of each commit |
| `commits[].sha` | `String` | Yes | Git commit SHA |
| `commits[].message` | `String` | Yes | Full commit message |
| `commits[].theme` | `String` | Yes | Theme that this commit addresses |
| `commits[].findingsCount` | `Integer` | Yes | Number of findings addressed in this commit |
| `commits[].filesChanged` | `Integer` | Yes | Number of files modified in this commit |
| `findings[].id` | `String` | Yes | Finding ID (matches consolidated JSON) |
| `findings[].fixStatus` | `Enum` | Yes | One of: `applied`, `failed`, `skipped` |
| `findings[].fixReason` | `String` | No | Reason for failure/skip (null if applied) |
| `findings[].compileError` | `String` | No | Truncated compile error (null unless `fixReason == "compilation_failure"`) |

### Fix Status Values

| Status | Description | Possible Reasons |
|--------|-------------|------------------|
| `applied` | Fix successfully applied and verified | -- |
| `failed` | Fix applied but caused regression | `compilation_failure`, `test_regression` |
| `skipped` | Fix not attempted | `file_not_found`, `ambiguous_comment` |

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
| `NO_ACTIONABLE_FINDINGS` | Zero actionable findings after classification (not an error) | `No actionable findings to fix. Skipping Steps 8-11.` | Normal flow -- no fixes needed |
| `BRANCH_CREATE_FAILED` | Cannot create correction branch | `Failed to create branch fix/epic-{epicId}-pr-comments: {error}` | Check git state and permissions |
| `COMPILE_FAILED_AFTER_FIX` | Compilation failed after applying a fix | `Compilation failed after fix {finding.id}. Reverted.` | Finding marked as failed; loop continues |
| `TEST_REGRESSION` | Tests failed after full batch; bisect identified offending fix | `Test regression caused by fix {finding.id}. Reverted.` | Offending fix reverted; remaining fixes retained |
| `BISECT_FAILED` | Bisect could not isolate offending fix | `Bisect failed: unable to isolate regression.` | All fixes reverted; manual intervention required |
| `PUSH_FAILED` | Cannot push correction branch to remote | `Failed to push fix/epic-{epicId}-pr-comments: {error}` | Check remote permissions and network |
| `PR_CREATE_FAILED` | GitHub CLI failed to create PR | `Failed to create PR: {error}` | Check GitHub token and repository permissions |

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
| RULE-003 | Single correction PR | Creates one PR consolidating all fixes with body referencing all source PRs (Step 11) |
| RULE-004 | Report persisted before corrections | Generates and saves `pr-comments-report.md` BEFORE any fixes are applied (Step 7) |
| RULE-005 | Deduplication cross-PR | SHA-256 fingerprint on normalized body + basename deduplicates across PRs (Step 6B) |
| RULE-006 | Fallback without checkpoint | Accepts `--prs` flag for explicit PR list |
| RULE-007 | Mandatory dry-run support | `--dry-run` generates report without applying fixes; report is final output in dry-run mode (Step 7.6) |
| RULE-008 | Replies in pt-BR | Reply engine posts pt-BR replies with classification-specific templates (Step 10) |
| RULE-009 | Post-correction verification | Runs compile + test after all fixes; reverts individual fixes causing regression via bisect (Step 9) |
| RULE-010 | Idempotency | Detects existing `fix/epic-{epicId}-pr-comments` branch; handles update vs. fresh creation (Steps 4, 8.2); checks for existing replies before posting (Step 10.5) |

### Future Steps (Subsequent Stories)

| Story | Step | Delivers | Status |
|-------|------|----------|--------|
| story-0026-0002 | Steps 5-6, 6B | Batch comment fetching, classification, and deduplication | **Delivered** |
| story-0026-0003 | Step 7 | Consolidated findings report | **Delivered** |
| story-0026-0004 | Steps 8-9, 11 | Fix orchestration, verification, and PR creation | **Delivered** |
| story-0026-0005 | Step 10 | Reply engine and status tracking | **Delivered** |
