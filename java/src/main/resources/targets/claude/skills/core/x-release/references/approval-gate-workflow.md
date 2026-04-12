# Approval Gate Workflow

> Reference document for the **x-release** skill, Step 8 (APPROVAL-GATE).
> Describes default halt behavior, interactive mode with `AskUserQuestion`,
> state transitions, error codes, and the resume path via
> `--continue-after-merge`.

## Overview

The Approval Gate is the safety checkpoint between opening the release PR
(Step 7 OPEN-RELEASE-PR) and applying irreversible actions (tag,
back-merge). It ensures the human operator explicitly reviews and merges
the release PR before the skill creates the git tag.

## Default Workflow (Non-Interactive)

```
Step 7 completes (PR_OPENED)
          |
          v
Step 8.1: Persist APPROVAL_PENDING in state file
          |
          v
Step 8.2: Print human-readable instructions
          - PR URL and number
          - Manual steps (review, CI, approve, merge)
          - Resume command: /x-release <VERSION> --continue-after-merge
          |
          v
Step 8.3: exit 0 (skill halts)
          |
     [operator reviews PR on GitHub]
     [operator merges PR when ready]
          |
          v
Re-invocation: /x-release <VERSION> --continue-after-merge
          |
          v
Step 0 detects APPROVAL_PENDING -> MODE = RESUME
          |
          v
Phase RESUME-AND-TAG (Step 9+)
```

### State Transition (Default)

| Field | Before | After |
|:---|:---|:---|
| `phase` | `PR_OPENED` | `APPROVAL_PENDING` |
| `phasesCompleted` | `[..., OPEN_RELEASE_PR]` | `[..., OPEN_RELEASE_PR, APPROVAL_GATE_REACHED]` |

## Interactive Workflow (`--interactive`)

When `--interactive` is set, the skill uses `AskUserQuestion` instead of
exiting, giving the operator three choices without leaving the session.

```
Step 8.1: Persist APPROVAL_PENDING (same as default)
          |
          v
Step 8.2: Print instructions (same as default)
          |
          v
Step 8.3: AskUserQuestion with 3 options
          |
          +--[Option 1: Continue]--> gh pr view (defense in depth)
          |                          |
          |                     [MERGED?]--yes--> Step 9 (TAG) in-session
          |                          |
          |                     [OPEN?]---> ABORT APPROVAL_PR_STILL_OPEN
          |                                 (re-present 3 options)
          |
          +--[Option 2: Halt]------> exit 0 (same as default)
          |
          +--[Option 3: Cancel]----> Double confirmation prompt
                                     |
                                [confirmed?]--yes--> Delete state file
                                     |               Print cleanup script
                                     |               exit 2 (APPROVAL_CANCELLED)
                                     |
                                [not confirmed?]----> Re-present 3 options
```

### AskUserQuestion Options

| # | Label | Behavior |
|:---|:---|:---|
| 1 | "PR merged, continue to tag (Recommended)" | Verifies PR state via `gh pr view $PR_NUMBER --json state`. If MERGED, proceeds to RESUME-AND-TAG. If OPEN, aborts with `APPROVAL_PR_STILL_OPEN`. |
| 2 | "Halt -- resume later with --continue-after-merge" | Identical to default non-interactive behavior. Exits with code 0. State preserved as `APPROVAL_PENDING`. |
| 3 | "Cancel release entirely" | Requires double confirmation. If confirmed, deletes the state file, prints manual cleanup script (`gh pr close`, `git push --delete`), exits with code 2. |

## Error Codes

| Code | Condition | Message | Exit |
|:---|:---|:---|:---|
| `APPROVAL_PR_STILL_OPEN` | Interactive option 1, but `gh pr view` returns state != MERGED | `PR #N is still OPEN. Merge first.` | 1 |
| `APPROVAL_CANCELLED` | Interactive option 3, confirmed | `Release cancelled by user.` | 2 |

## Idempotency (RULE-003)

The Approval Gate is inherently idempotent:

1. On first execution, it writes `APPROVAL_PENDING` and halts.
2. On re-invocation **without** `--continue-after-merge`, Step 0 detects
   `phase: APPROVAL_PENDING` (which is not `COMPLETED`) and aborts with
   `STATE_CONFLICT`.
3. On re-invocation **with** `--continue-after-merge`, Step 0 validates
   the phase and jumps directly to RESUME-AND-TAG, bypassing the gate.

The gate never executes twice for the same release.

## Defense in Depth (RULE-004)

Even when the operator selects "PR merged, continue to tag" in interactive
mode, the skill verifies the PR state via `gh pr view` before proceeding.
This prevents accidental tagging when the PR is still open.

## Cancel Path

When the operator cancels (Option 3):

1. State file is **deleted** (not just marked as cancelled).
2. The release PR and branch must be cleaned up manually.
3. The skill prints the exact commands needed for cleanup:
   - `gh pr close <PR_NUMBER>`
   - `git push origin --delete release/<VERSION>`
   - `git branch -d release/<VERSION>`

## Relationship to Other Phases

| Phase | Relationship |
|:---|:---|
| Step 7 (OPEN-RELEASE-PR) | Produces `PR_OPENED` state consumed by Step 8 |
| Step 8 (APPROVAL-GATE) | Produces `APPROVAL_PENDING` state consumed by Step 0 on resume |
| Step 0 (RESUME-DETECT) | Detects `APPROVAL_PENDING` + `--continue-after-merge` to enter RESUME mode |
| Step 9+ (RESUME-AND-TAG) | Consumes the MERGED PR state to create the git tag |
