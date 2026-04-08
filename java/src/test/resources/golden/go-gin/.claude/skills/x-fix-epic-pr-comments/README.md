# x-fix-epic-pr-comments

> Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution.

| | |
|---|---|
| **Category** | Review |
| **Invocation** | `/x-fix-epic-pr-comments [EPIC-ID] [--dry-run] [--prs N,M,...] [--skip-replies] [--include-suggestions]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Automates addressing PR review comments across an entire epic in batch. Instead of processing PRs one by one, it discovers all PRs from the epic's execution state, fetches and classifies all review comments, generates a consolidated findings report, applies fixes for actionable items, and creates a single correction PR. Supports dry-run mode for report-only execution and explicit PR list fallback when no checkpoint exists.

## Usage

```
/x-fix-epic-pr-comments 0024
/x-fix-epic-pr-comments 0024 --dry-run
/x-fix-epic-pr-comments 0024 --prs 143,144,145
/x-fix-epic-pr-comments 0024 --include-suggestions
```

## Workflow

1. **Parse** -- Parse epic ID and optional flags (dry-run, prs, skip-replies)
2. **Validate** -- Check prerequisites (epic directory, checkpoint, or --prs)
3. **Discover** -- Extract PR list from execution-state.json or --prs flag
4. **Idempotency** -- Check for existing correction branch
5. **Fetch** -- Batch-fetch review comments from all discovered PRs
6. **Classify** -- Classify each comment using actionable/suggestion/question heuristics
7. **Report** -- Generate consolidated findings report across all PRs

## Outputs

| Artifact | Path |
|----------|------|
| Findings report | `plans/epic-XXXX/reviews/epic-pr-comments-report.md` |
| Correction PR | Created via `gh pr create` on correction branch |

## See Also

- [x-fix-pr-comments](../x-fix-pr-comments/) -- Single-PR version of this skill
- [x-dev-epic-implement](../x-dev-epic-implement/) -- Epic orchestrator that produces the PRs this skill processes
- [x-review](../x-review/) -- Specialist reviews that generate findings to fix
