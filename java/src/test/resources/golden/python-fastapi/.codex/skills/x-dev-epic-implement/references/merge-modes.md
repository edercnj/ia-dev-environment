# Merge Modes Reference

> **Context:** This reference details the PR merge decision mechanism.
> Part of x-dev-epic-implement skill.

## PR Merge Decision Mechanism (RULE-004)

When stories have dependencies with `status === SUCCESS` but `prMergeStatus !== "MERGED"`,
the orchestrator behavior depends on the `mergeMode`:

**1. Auto-merge mode (`mergeMode === "auto"`, via `--auto-merge`):**

For each dependency with an unmerged PR and approved reviews, execute
`gh pr merge {prNumber} --merge`. Merge order follows `sortByCriticalPath()` (RULE-007).
If merge fails (conflict, failing checks), log warning and fall through to polling
(60s interval, 24h timeout). On timeout: mark dependent stories as `BLOCKED` with
reason `"PR merge timeout"`.

**2. No-merge mode (`mergeMode === "no-merge"`, via `--no-merge`):**

Skip PR merge wait entirely. Dependencies are satisfied by `status === SUCCESS` alone.
Log: `"--no-merge: skipping merge wait for PR #{prNumber} (story-{id}). Dependency satisfied by SUCCESS status."`
Proceed immediately to dispatch dependent stories.

When `--no-merge` is active and a dependent story has dependencies with `prMergeStatus === "OPEN"`,
the dependent story's branch must incorporate the dependency's code. Before dispatching the
dependent story, the orchestrator instructs the subagent to merge dependency branches:

```
Before starting implementation, merge dependency branches into your story branch:
  git fetch origin
  for each dependency branch where prMergeStatus === "OPEN":
    git merge origin/feat/{dep-storyId}-short-description --no-edit
This ensures your story has access to dependency code that has not yet been merged to develop.
```

> **Branch pattern:** `feat/{storyId}-short-description` — consistent with the subagent dispatch
> branch naming in Section 1.4 of the main SKILL.md and x-dev-story-implement Phase 0.

**3. Interactive mode (`mergeMode === "interactive"`, via `--interactive-merge`):**

After all stories in the current phase complete with `status === SUCCESS`, prompt the user
using `AskUserQuestion`:

```
question: "Phase {N} complete. {count} PR(s) created. How would you like to proceed with merging?"
header: "PR Merge"
options:
  - label: "Merge all and continue"
    description: "Auto-merge all open PRs in this phase via gh pr merge, then proceed to next phase"
  - label: "I will merge manually — pause"
    description: "Pause execution. I will merge PRs manually. Resume with --resume after merging."
  - label: "Skip merge — continue without merging"
    description: "Proceed to next phase without merging. Dependencies satisfied by SUCCESS status only."
multiSelect: false
```

- **"Merge all and continue"**: Execute `gh pr merge {prNumber} --merge` for each open PR
  in the phase (critical path order). On success, update `prMergeStatus = "MERGED"`.
  On failure, log error and fall back to "I will merge manually" behavior.
- **"I will merge manually — pause"**: Save checkpoint and pause execution. The user
  runs `--resume` after manually merging PRs. On resume, PR status is verified via
  `gh pr view`.
- **"Skip merge — continue without merging"**: Behave as `mergeMode === "no-merge"` for
  this phase only. Log warning. Proceed without PR merge check for dependent stories.
  Dependent stories will merge dependency branches as described in mode 2.
