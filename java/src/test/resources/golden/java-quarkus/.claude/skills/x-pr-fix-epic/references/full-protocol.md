<!-- Returns to [slim body](../SKILL.md) after reading the required phase. -->

# x-pr-fix-epic — Full Protocol

## Step 1 — Input Parsing

`EPIC-ID` is a required positional argument matching `^\d{4}$`. On missing or malformed ID → abort with error.

| Flag | Type | Default | Rule |
|------|------|---------|------|
| `--dry-run` | boolean | `false` | RULE-007 |
| `--prs` | `List<Integer>` | (none) | RULE-006 |
| `--skip-replies` | boolean | `false` | — |
| `--include-suggestions` | boolean | `false` | — |

**Flag precedence for `--prs`:** `--prs` overrides checkpoint discovery regardless of `execution-state.json` presence.

---

## Step 2 — Prerequisite Checks

### Path A (without `--prs`): checkpoint-based discovery

1. `plans/epic-{epicId}/` must exist → `EPIC_DIR_NOT_FOUND` if absent.
2. `plans/epic-{epicId}*/execution-state.json` must exist → `CHECKPOINT_NOT_FOUND` if absent.

### Path B (with `--prs`): explicit PR list

1. All values must be positive integers → `INVALID_PR_NUMBER` if not.
2. At least one PR must be valid (check via `gh pr view {prNumber} --json state`) → `NO_VALID_PRS` if none.

---

## Step 3 — PR Discovery (RULE-001)

### From checkpoint (default)

1. Read `execution-state.json`; parse `stories` object.
2. Filter: `prNumber != null`; skip entries with `status: FAILED` or `status: BLOCKED`.
3. Build `{prNumber, storyId, prMergeStatus, prUrl}` records; sort by `prNumber` ascending.

### From `--prs` flag (RULE-006)

1. Parse comma-separated integers.
2. For each: `gh pr view {N} --json number,state,title` to verify existence.
3. Build records with `storyId: "unknown"`. Filter invalid with warning.

---

## Step 4 — Idempotency Check (RULE-010)

```bash
git branch -a | grep "fix/epic-{epicId}-pr-comments"
```

| Branch exists | `--dry-run` | Action |
|---------------|-------------|--------|
| No | any | Proceed normally |
| Yes | `true` | Skip check; continue with report generation only |
| Yes | `false` | Prompt: update existing (`u`) or create new (`n`) |

---

## Step 5 — Batch Comment Fetching (RULE-002)

For each PR:

**5.1 Inline comments:**
```bash
gh api repos/{owner}/{repo}/pulls/{prNumber}/comments \
  --jq '.[] | {id:.id, path:.path, line:.line, body:.body, createdAt:.created_at, user:.user.login}'
```

**5.2 Review-level comments:**
```bash
gh api repos/{owner}/{repo}/pulls/{prNumber}/reviews \
  --jq '.[] | select(.body != "" and .body != null) | {id:.id, body:.body, state:.state, user:.user.login}'
```

**Rate limiting:** Check `gh api rate_limit --jq '.resources.core.remaining'` before each call. On HTTP 429: read `Retry-After`, wait, retry (max 3). After 3 failures → skip PR with warning.

**Timeout:** 30s per PR fetch cycle. On timeout → `status: "TIMEOUT"`, log warning, continue.

---

## Step 6 — Classification Engine (RULE-002)

5 categories, priority-ordered — **first match wins:**

| Priority | Type | Condition | Action |
|----------|------|-----------|--------|
| 1 | Resolved | Thread marked `resolved` in API | Skip |
| 2 | Actionable | Contains ` ```suggestion ` block OR matches: `please change`, `should be`, `must`, `fix`, `bug`, `wrong` OR `CHANGES_REQUESTED` with specific request | Fix |
| 3 | Question | `?` AND (`why` \| `what` \| `how does` \| `how do` \| `how is`) | Skip (human response) |
| 4 | Suggestion | `consider`, `maybe`, `could`, `suggestion`, `nit`, `might want to`, `would be nice`, `alternatively` | Fix if `--include-suggestions` |
| 5 | Praise | `LGTM`, `nice`, `good`, `great`, `looks good`, `well done`, `clean` | Skip |

Fallback: classify as `suggestion`.

**Suggestion block extraction:** when body contains ` ```suggestion...``` `, set `hasSuggestion: true`, extract `suggestionCode`. Always classifies as Actionable (Priority 2).

---

## Step 6B — Deduplication Cross-PR (RULE-005)

```
fingerprint = SHA-256( normalize(body) + "|" + basename(path) )
normalize(body) = lowercase + collapse whitespace + strip leading/trailing + remove line refs (line 42, L42, :42)
basename(path) = filename without directory
```

Group by fingerprint; keep first occurrence (lowest PR number) as canonical. `sourcePRs` = originating PR; `affectedPRs` = all PRs with matching fingerprint.

---

## Consolidated Data Structure

```json
{
  "epicId": "XXXX",
  "fetchedAt": "<ISO-8601>",
  "totalPRs": 16, "processedPRs": 15,
  "skippedPRs": [{"prNumber": 999, "reason": "TIMEOUT"}],
  "totalComments": 59, "uniqueFindings": 34,
  "findings": [{
    "id": "F-001", "fingerprint": "abc123...", "classification": "actionable",
    "file": "path/to/file.md", "line": 47, "body": "...",
    "hasSuggestion": true, "suggestionCode": "...",
    "sourcePRs": [143], "affectedPRs": [143, 148, 150],
    "reviewer": "copilot-pull-request-reviewer[bot]", "theme": null,
    "createdAt": "<ISO-8601>"
  }],
  "summary": {"actionable": 34, "suggestion": 33, "question": 0, "praise": 0, "resolved": 0, "duplicatesRemoved": 25}
}
```

---

## Step 7 — Consolidated Findings Report (RULE-004)

### 7.1 Theme Detection (applied before report, first match wins)

| Theme | Heuristic | Match Target |
|-------|-----------|--------------|
| `naming` | `rename`, `naming`, `name`, `diacritics` | body |
| `placeholder` | `placeholder`, `{{`, `ambiguous` | body |
| `consistency` | `inconsistent`, `standardize`, `align` | body |
| `testing` | `Test.java`, `test/`, `spec` | file path |
| `golden-files` | `golden/` | file path |
| `security` | `security`, `OWASP`, `injection`, `XSS` | body |
| `other` | no match | fallback |

Body-based heuristics take priority over file-based heuristics.

### 7.2 Report Format

```markdown
# PR Review Comments — Consolidated Report
- **Epic:** EPIC-{epicId}  **Date:** {YYYY-MM-DD}  **PRs Analyzed:** N  **Unique Findings:** N

## Summary
| Category | Count | % |
## Actionable Findings
| # | PRs | File | Line | Summary | Has Suggestion | Theme |
## Suggestion Findings
| # | PRs | File | Line | Summary | Theme |
## Questions Requiring Human Response
| # | PR | File | Line | Reviewer | Question |
## Recurring Themes
| Theme | Count | Affected PRs | Description |
## Dry-Run Summary   ← ONLY included when --dry-run is active
```

### 7.3 Persistence (RULE-004)

1. Create `{epicDir}/reports/` if absent.
2. Write to `{epicDir}/reports/pr-comments-report.md` **BEFORE any fix operations begin**.
3. Idempotent: overwrite if exists.

### 7.6 Dry-Run Integration (RULE-007)

When `--dry-run`: execute Steps 1-7 only. Report includes "Dry-Run Summary" section. **STOP after Step 7.** Do NOT proceed to Steps 8-11.

---

## Step 8 — Fix Orchestration Engine (RULE-003, RULE-010)

### 8.1 Pre-condition

`if actionableFindings.size == 0 → return early` (no fixes, no PR). With `--include-suggestions`, also fix `classification == "suggestion"` findings.

### 8.2 Branch Setup (RULE-010)

Create correction branch from `develop` (branch from `develop`). Resolve base branch from `execution-state.json` `.baseBranch` field, defaulting to `develop`.

```bash
git checkout develop && git pull origin develop
git checkout -b fix/epic-{epicId}-pr-comments
```

Idempotency: branch exists + user chose `u` → checkout existing; user chose `n` → delete and recreate.

### 8.3 Fix Application Loop

Process findings ordered by `file` ascending, then `line` ascending (null lines last).

**Strategy A — Direct suggestion** (`hasSuggestion == true`): locate code at `finding.line`, replace with `suggestionCode`.

**Strategy B — Context-inferred** (`hasSuggestion == false`): read file + body, infer change from comment intent. If ambiguous → `fixStatus: "skipped"`, `fixReason: "ambiguous_comment"`.

**Per-fix compile gate:**
```bash
mvn compile -q 2>&1
```
Non-zero → `git checkout -- {affected_files}` + mark `fixStatus: "failed"`, `fixReason: "compilation_failure"`.

### Step 8-GF — Golden File Handling

When `finding.file` contains `golden/` or `finding.theme == "golden-files"`:
1. Identify source template (reverse-map from golden path).
2. Apply fix to source template first.
3. Run `GoldenFileRegenerator` to propagate across all profiles.
4. Stage all modified golden files.

---

## Step 9 — Post-Correction Verification (RULE-009)

```bash
mvn test 2>&1
```

On failure: **simplified bisect** — revert all; re-apply one-by-one; run `mvn test -q` after each; revert offending fixes; mark `fixReason: "test_regression"`; confirm green with remaining fixes.

### 9.3 Atomic Commits by Theme

Group applied fixes by theme. One commit per theme (alphabetical order):

| Theme | Commit Message |
|-------|---------------|
| `naming` | `fix(epic-{epicId}): correct naming conventions` |
| `placeholder` | `fix(epic-{epicId}): resolve placeholder and template variables` |
| `consistency` | `fix(epic-{epicId}): standardize inconsistent patterns` |
| `testing` | `fix(epic-{epicId}): address test-related review comments` |
| `golden-files` | `fix(epic-{epicId}): update golden files` |
| `security` | `fix(epic-{epicId}): address security review comments` |
| `other` | `fix(epic-{epicId}): address miscellaneous review comments` |

---

## Step 11 — PR Creation (RULE-003)

```bash
git push -u origin fix/epic-{epicId}-pr-comments
# on rejection: git push --force-with-lease origin fix/epic-{epicId}-pr-comments

gh pr create --base develop \
  --title "fix(epic-{epicId}): address PR review comments" \
  --body "{pr_body}"
```

PR output shows `Base: develop`. PR body MUST reference all source PRs for traceability (RULE-003). Includes findings-fixed table, findings-skipped table, findings-failed table, and verification status (compile + tests).

---

## Fix Result Data Contract

```json
{
  "fixBranch": "fix/epic-{epicId}-pr-comments",
  "prUrl": "https://github.com/{owner}/{repo}/pull/{prNumber}",
  "prNumber": 159,
  "fixesApplied": 8, "fixesFailed": 1, "fixesSkipped": 2,
  "testsPass": true, "commitCount": 3,
  "commits": [{"sha":"abc123","message":"fix(epic-{epicId}): correct naming conventions","theme":"naming","findingsCount":5,"filesChanged":3}],
  "findings": [
    {"id":"F-001","fixStatus":"applied","fixReason":null,"compileError":null},
    {"id":"F-002","fixStatus":"failed","fixReason":"compilation_failure","compileError":"error: ';' expected at line 42"},
    {"id":"F-003","fixStatus":"skipped","fixReason":"file_not_found","compileError":null}
  ]
}
```

`fixStatus` values: `applied` | `failed` (compilation_failure, test_regression) | `skipped` (file_not_found, ambiguous_comment).

---

## Cross-Rule Reference

| Rule | How This Skill Implements It |
|------|------------------------------|
| RULE-001 | Reads `execution-state.json` to discover PRs |
| RULE-002 | Reuses `x-pr-fix` classification heuristics with priority-ordered matching (Step 6) |
| RULE-003 | Single correction PR referencing all source PRs (Step 11) |
| RULE-004 | Report persisted BEFORE fixes begin (Step 7.3) |
| RULE-005 | SHA-256 fingerprint deduplication cross-PR (Step 6B) |
| RULE-006 | Accepts `--prs` when no checkpoint exists |
| RULE-007 | `--dry-run` generates report without applying fixes |
| RULE-009 | Full test suite + bisect after all fixes (Step 9) |
| RULE-010 | Detects existing branch; handles update vs. fresh creation (Steps 4, 8.2) |

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-pr-fix` | reads | Reuses classification heuristics (RULE-002) |
| `x-epic-implement` | called-after | Processes review comments from PRs created by epic implementation |
| `x-epic-decompose` | depends-on | Requires epic directory and story files to exist |
