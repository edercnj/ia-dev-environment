# x-pr-watch-ci

**Category:** `core/pr/` | **User-invocable:** yes

Polls a PR's CI checks and Copilot review, blocking until checks complete or the configured timeout expires. Produces a stable exit code and a JSON summary on stdout that orchestrators use to build the EPIC-0043 interactive gate menu.

## Quick Start

```bash
# Watch PR #42 with defaults (timeout=1800s, require Copilot review)
/x-pr-watch-ci --pr-number 42

# Skip Copilot wait
/x-pr-watch-ci --pr-number 42 --require-copilot-review false

# Short timeout for automation
/x-pr-watch-ci --pr-number 42 --timeout-seconds 300 --poll-interval-seconds 30
```

## Invocation by Orchestrators (Rule 13 Pattern 1 INLINE-SKILL)

```markdown
Skill(skill: "x-pr-watch-ci", args: "--pr-number 42")
```

Bare-slash (`/x-pr-watch-ci`) is forbidden in delegation contexts (Rule 13 §Forbidden).

## Exit Codes (Stable Public Contract — RULE-045-05)

| Code | Name | Meaning |
|------|------|---------|
| 0 | `SUCCESS` | All checks green + Copilot review present |
| 10 | `CI_PENDING_PROCEED` | Checks green, Copilot timeout elapsed without review |
| 20 | `CI_FAILED` | A check concluded `failure`/`timed_out`/`cancelled`/`action_required` |
| 30 | `TIMEOUT` | Global timeout elapsed, checks still pending |
| 40 | `PR_ALREADY_MERGED` | PR was already merged |
| 50 | `NO_CI_CONFIGURED` | No CI checks configured on the PR |
| 60 | `PR_CLOSED` | PR closed without merge |
| 70 | `PR_NOT_FOUND` | PR does not exist or caller lacks permission |

## Stdout JSON (last line)

```json
{
  "status": "SUCCESS",
  "prNumber": 42,
  "checks": [
    {"name": "build", "conclusion": "success"},
    {"name": "test",  "conclusion": "success"}
  ],
  "copilotReview": {"present": true, "reviewId": 12345678},
  "elapsedSeconds": 87
}
```

## State File

Default path: `.claude/state/pr-watch-<N>.json`

Enables session resume: if the Claude session is interrupted during a long CI run, the next invocation reads the state file and resumes from the elapsed time.

Use `--no-state-file` to disable persistence (fire-and-forget mode).

## Parameters

| Parameter | Type | Default | Bounds |
|-----------|------|---------|--------|
| `--pr-number <N>` | int | required | >0 |
| `--timeout-seconds <N>` | int | 1800 | 60–7200 |
| `--poll-interval-seconds <N>` | int | 60 | 15–300 |
| `--require-copilot-review` | boolean | true | — |
| `--require-checks-passing` | boolean | true | — |
| `--copilot-review-timeout <N>` | int | 900 | 60–timeout |
| `--state-file <path>` | path | `.claude/state/pr-watch-<N>.json` | max 512 chars |
| `--no-state-file` | flag | — | — |

## Java Helper

The classification logic is extracted to `dev.iadev.adapter.pr.PrWatchStatusClassifier` for testability.

```java
var classifier = new PrWatchStatusClassifier();
var exitCode = classifier.classify(new ClassifyInput(
    checks, copilotPresent, copilotTimeoutElapsed,
    prState, merged, globalTimeoutElapsed, requireCopilotReview));
```

See `PrWatchStatusClassifierTest` for @ParameterizedTest covering all 8 exit code rows (RULE-045-05).

## Rules Applied

- **RULE-045-03** — State-file canonical versioned (`schemaVersion: "1.0"`, atomic write)
- **RULE-045-04** — Copilot login: `copilot-pull-request-reviewer[bot]`
- **RULE-045-05** — Exit codes 0/10/20/30/40/50/60/70 are stable public contract
- **RULE-045-06** — Orchestrators use Rule 13 Pattern 1 INLINE-SKILL
- **Rule 13** — Skill invocation protocol (no bare-slash in delegation)
- **Rule 14** — No worktree creation (runs in caller's working tree)
