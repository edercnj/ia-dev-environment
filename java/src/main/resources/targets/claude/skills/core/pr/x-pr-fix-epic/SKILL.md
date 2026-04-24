---
name: x-pr-fix-epic
model: sonnet
description: "Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--dry-run] [--prs N,M,...] [--skip-replies] [--include-suggestions]"
user-invocable: true
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-pr-fix-epic 0024
/x-pr-fix-epic 0024 --dry-run
/x-pr-fix-epic 0024 --prs 143,144,145
/x-pr-fix-epic 0024 --skip-replies
/x-pr-fix-epic 0024 --include-suggestions
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `EPIC-ID` | positional | (required) | 4-digit zero-padded epic identifier (e.g., `0024`) |
| `--dry-run` | boolean | `false` | Generate consolidated report only; no fixes applied (RULE-007) |
| `--prs` | `List<Integer>` | (none) | Explicit PR list; overrides checkpoint discovery (RULE-006) |
| `--skip-replies` | boolean | `false` | Apply fixes without replying to original PR comments |
| `--include-suggestions` | boolean | `false` | Include suggestion-type comments in fix scope |

## Output Contract

| Artifact | Path | Description |
|----------|------|-------------|
| `pr-comments-report.md` | `plans/epic-{epicId}/reports/pr-comments-report.md` | Consolidated findings report (written BEFORE any fixes — RULE-004). Includes actionable / suggestion / question / praise / resolved counts with deduplication summary and per-finding theme table. |
| Correction PR | `fix/epic-{epicId}-pr-comments` | Single correction PR consolidating all applied fixes. References all source PRs. Created only when actionable findings exist and `--dry-run` is not set. |

**Workflow:** Parse → Validate → Discover PRs → Idempotency check → Batch-fetch comments → Classify (5 categories, priority-ordered) → Dedup cross-PR (SHA-256 fingerprint) → Report → Apply fixes (per-fix compile gate) → Post-correction test suite → Theme-based commits → Push → Create PR.

**Dry-run exits after report generation** (Steps 1-7 only; no fixes, no PR).

## Error Envelope

| Code | Condition | Remediation |
|------|-----------|-------------|
| `EPIC_DIR_NOT_FOUND` | `plans/epic-{epicId}/` missing | Run `/x-epic-decompose` first |
| `CHECKPOINT_NOT_FOUND` | `execution-state.json` missing and no `--prs` | Provide `--prs` flag |
| `NO_VALID_PRS` | No PR in explicit list exists | Verify PR numbers |
| `INVALID_EPIC_ID` | Epic ID not 4 digits | Provide valid 4-digit ID |
| `INVALID_PR_NUMBER` | Non-integer in `--prs` list | Fix the PR list |
| `RATE_LIMIT_EXCEEDED` | GitHub API rate limit after 3 retries | Wait for reset or reduce PR count |
| `FETCH_TIMEOUT` | PR comment fetch exceeded 30s | PR skipped; retry manually if needed |
| `API_ERROR` | GitHub API non-200/non-429 | Check GitHub token permissions |
| `NO_ACTIONABLE_FINDINGS` | Zero actionable findings (non-fatal) | Normal flow — no fixes needed |
| `BRANCH_CREATE_FAILED` | Cannot create correction branch | Check git state and permissions |
| `COMPILE_FAILED_AFTER_FIX` | Compilation failed after fix application | Finding reverted; loop continues |
| `TEST_REGRESSION` | Tests failed; bisect identified offending fix | Offending fix reverted; remaining retained |
| `BISECT_FAILED` | Bisect could not isolate regression | All fixes reverted; manual intervention needed |
| `PUSH_FAILED` | Cannot push correction branch | Check remote permissions and network |
| `PR_CREATE_FAILED` | `gh pr create` failed | Check GitHub token and repository permissions |

## Full Protocol

> Complete 11-step workflow (Steps 1-4 parse/validate/discover/idempotency, Steps 5-6B batch fetch/classify/dedup, Step 7 report with theme detection and dry-run integration, Steps 8-9 fix engine with per-fix compile gate and bisect verification, Step 11 PR creation), consolidated data structures (findings JSON + fix result JSON), golden-file propagation (Step 8-GF), cross-rule reference (RULE-001 through RULE-010), and classification heuristic table in [`references/full-protocol.md`](references/full-protocol.md).
