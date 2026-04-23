---
name: x-epic-implement
model: sonnet
description: "Thin orchestrator (~460 lines — story-0049-0018 refactor) that drives an epic end-to-end via 6 delegated phases: Phase 0 (args via x-internal-args-normalize), Phase 1 (load+plan via x-internal-epic-build-plan), Phase 2 (epic branch via x-internal-epic-branch-ensure), Phase 3 (sequential-by-default story loop via x-story-implement), Phase 4 (integrity gate + report via x-internal-epic-integrity-gate + x-internal-report-write), Phase 5 (final PR epic/XXXX → develop via x-git-merge + x-pr-create). Defaults flipped by EPIC-0049: sequential execution (opt-in parallel via --parallel), auto-merge of story PRs into epic/XXXX (target changed from develop). Legacy EPIC-0042 behavior preserved under --legacy-flow (auto-detected via execution-state.json flowVersion=1). Zero inline git/gh/jq/mvn calls — orchestrator uses only Read/Glob + Skill."
user-invocable: true
allowed-tools: Read, Write, Glob, Skill, Agent, AskUserQuestion
argument-hint: "[EPIC-ID] [--parallel] [--legacy-flow] [--phase N] [--story story-XXXX-YYYY] [--resume] [--dry-run] [--skip-review] [--auto-merge-strategy merge|squash|rebase] [--strict-overlap] [--non-interactive] [--skip-pr-comments] [--revert-on-failure] [--skip-smoke]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## CONTEXT MANAGEMENT

- **Story results:** record only the compact envelope returned by `x-story-implement` (`status`, `storyId`, `prNumber`, `prUrl`, `commitSha`, `coverageLine`, `coverageBranch`). Never accumulate full TDD cycle logs.
- **Plan files:** reference by file path only (`plans/epic-XXXX/epic-execution-plan-XXXX.md`). Do NOT read full content back into context after generation.
- **Execution state:** read `execution-state.json` targeted — grep/jq for specific story IDs and status fields via sub-skills, never load the full document inline.
- **Reports:** delegate report generation to `x-internal-report-write`; the orchestrator only consumes `{status, path}`.
- **Integrity gate output:** the envelope `{passed, failures, coverageDelta, dodChecklist}` is the only thing recorded; raw mvn logs stay in the sub-skill context.

# Skill: Epic Implementation (Thin Orchestrator — ADR-0049)

Orchestrate the end-to-end implementation of an entire epic by delegating every substantive responsibility to specialized sub-skills. The orchestrator's only inline responsibilities are: parse argv (via delegate), read the epic metadata envelope (via delegate), drive the story loop (via `x-story-implement`), and close with the final epic PR. All git/gh/jq/mvn side-effects live inside the delegates — this SKILL.md contains zero direct shell invocations of those tools (only `Read` and `Glob` for local file discovery plus `Skill`/`Agent` for delegation).

**EPIC-0049 defaults (flipped from EPIC-0042):**

| Aspect | Before (EPIC-0042) | After (EPIC-0049, this skill) |
| :--- | :--- | :--- |
| Parallelism | default `true` (opt-out `--sequential`) | default `false` (opt-in `--parallel`) |
| Story-PR target | `develop` | `epic/<EPIC-ID>` |
| Story-PR merge | `merge` (fixed) | configurable via `--auto-merge-strategy` |
| Final PR | N/A (each story merged directly to develop) | `epic/<EPIC-ID> → develop` (manual gate, label `epic-integration`) |

## Triggers

- `/x-epic-implement 0049` — full epic run (new default: sequential + auto-merge into `epic/0049`)
- `/x-epic-implement 0049 --parallel` — opt-in parallel story execution via worktrees
- `/x-epic-implement 0049 --legacy-flow` — preserve EPIC-0042 behavior (stories merge directly into `develop`, no final PR)
- `/x-epic-implement 0049 --resume` — continue from `execution-state.json`; auto-detects `flowVersion`
- `/x-epic-implement 0049 --phase 2` — execute only phase 2 stories
- `/x-epic-implement 0049 --story story-0049-0007` — execute a single story in isolation
- `/x-epic-implement 0049 --dry-run` — produce execution plan without executing

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `EPIC-ID` | String (4-digit) | — | Positional, required. Epic identifier (e.g., `0049`). |
| `--parallel` | Boolean | `false` | Opt-in parallel story execution via worktrees; base `epic/<EPIC-ID>`. Mutually exclusive with `--legacy-flow` (legacy implies sequential). |
| `--legacy-flow` | Boolean | `false` | Force EPIC-0042 behavior: stories merge directly into `develop`; Phase 2 (branch setup) and Phase 5 (final PR) become no-ops. Auto-set when `execution-state.json` declares `flowVersion=1`. |
| `--phase N` | Integer | — | Execute only stories belonging to phase N. Mutually exclusive with `--story`. |
| `--story ID` | String | — | Execute a single story (e.g., `story-0049-0007`). Mutually exclusive with `--phase`. |
| `--resume` | Boolean | `false` | Continue from last checkpoint (`execution-state.json` required). Auto-detects `flowVersion` and applies legacy mode with visible warning when `flowVersion=1`. |
| `--dry-run` | Boolean | `false` | Generate execution plan and exit. No stories dispatched. |
| `--skip-review` | Boolean | `false` | Propagated to each `x-story-implement` dispatch — skips the specialist/tech-lead reviews at story level. |
| `--auto-merge-strategy` | Enum | `merge` | Strategy for story-PR auto-merge into `epic/<EPIC-ID>`: `merge \| squash \| rebase`. Propagated to `x-pr-create --auto-merge`. |
| `--strict-overlap` | Boolean | `false` | When set, `x-internal-epic-build-plan --mode parallel --strict-overlap` demotes hard-overlap stories to sequential; default is advisory-only (RULE-005). |
| `--non-interactive` | Boolean | `false` | Skip all `AskUserQuestion` gates. Required for CI / orchestrated calls from `x-epic-orchestrate`. |
| `--skip-pr-comments` | Boolean | `false` | Skip the optional post-gate PR-comment remediation pass (`x-pr-fix-epic`). |
| `--revert-on-failure` | Boolean | `false` | On integrity-gate failure, revert the last story merge instead of dispatching an agent-assisted remediation pass (EPIC-0042 parity). |
| `--skip-smoke` | Boolean | `false` | Bypass the epic smoke gate (advisory only; EPIC-0042 made smoke mandatory, `--skip-smoke` reintroduces opt-out for emergency overrides). |

### Deprecated flags (emit one-time warning; still parsed for back-compat)

| Flag | Fate | Replacement |
| :--- | :--- | :--- |
| `--sequential` | No-op (sequential is now default) | Omit the flag |
| `--auto-merge` | No-op (auto-merge is now default against `epic/<ID>`) | Omit the flag |
| `--no-merge` | Rejected — incompatible with EPIC-0049 defaults | Use `--legacy-flow` for no-PR-merge behavior, or let default run |
| `--interactive-merge` | No-op | Use `--non-interactive=false` (interactive is default when TTY detected) |
| `--manual-batch-approval`, `--batch-approval` | No-op (ADR-0010) | Use `--non-interactive` |
| `--single-pr` | Deprecated alias for `--legacy-flow` | Use `--legacy-flow` |
| `--auto-approve-pr` | Propagated as-is to `x-story-implement` | Keep for task-PR-into-parent-branch flow inside stories |
| `--task-tracking` | Always-on in v2 | Omit the flag |
| `--dry-run-only-comments` | No-op | `--skip-pr-comments` suppresses Phase 4 entirely |

## Output Contract

| Field | Type | Always | Description |
|-------|------|--------|-------------|
| `epicId` | String | Yes | `0049` (normalized, 4-digit zero-padded) |
| `epicBranch` | String | Yes | `epic/0049` (new default) or `develop` in legacy mode |
| `flowVersion` | String | Yes | `"1"` (legacy flow) or `"2"` (new default) |
| `phasesExecuted` | List<{name, durationSec, status}> | Yes | Each of the 6 phases actually run |
| `storiesExecuted` | List<{id, status, prNumber, prUrl}> | Yes | One entry per dispatched story |
| `finalPrUrl` | String \| null | Yes | URL of the final `epic/XXXX → develop` PR (null when `--legacy-flow`) |
| `finalPrNumber` | Integer \| null | Yes | PR number of the final PR (null when `--legacy-flow`) |
| `integrityGatePassed` | Boolean | Yes | Result of Phase 4 gate |
| `coverageLine` / `coverageBranch` | Number \| null | Yes | Filtered coverage from the integrity gate envelope |
| `reportsDir` | String | Yes | `plans/epic-XXXX/reports/` |

## Error Codes

| Exit | Code | Condition | Message |
|------|------|-----------|---------|
| 1 | `ARGS_INVALID` | `x-internal-args-normalize` exit 1 | `"Invalid args: <detail from normalizer>"` |
| 2 | `EPIC_DIR_MISSING` | `plans/epic-XXXX/` absent | `"Epic directory not found: plans/epic-XXXX/"` |
| 3 | `STORY_FAILED` | One or more stories returned `status=FAILED` | `"Story <id> failed: <summary>"` |
| 4 | `INTEGRITY_GATE_FAILED` | Phase 4 gate envelope `passed=false` | `"Integrity gate failed: <first failure>"` |
| 5 | `FINAL_PR_CONFLICTS` | Phase 5 `x-git-merge` reports conflicts syncing develop→epic | `"Conflicts syncing develop into epic/<ID>: manual resolution needed"` |
| 6 | `BRANCH_ENSURE_FAILED` | `x-internal-epic-branch-ensure` exit non-zero | `"Could not ensure epic/<ID> branch: <detail>"` |
| 7 | `PLAN_BUILD_FAILED` | `x-internal-epic-build-plan` exit non-zero (non-cyclic) | `"Execution plan build failed: <detail>"` |
| 8 | `CYCLIC_DEPENDENCY` | `x-internal-epic-build-plan` exit 3 | `"Cyclic story dependency detected: <cycle>"` |

## Delegation Map (RULE-005 — Thin Orchestrator)

| Concern | Skill | Phase |
|---------|-------|-------|
| Argv parsing + deprecation warnings | `x-internal-args-normalize` | 0 |
| Load epic / stories / DAG + render execution plan | `x-internal-epic-build-plan` | 1 |
| `epic/<ID>` branch ensure (idempotent, local + origin) | `x-internal-epic-branch-ensure` | 2 |
| Per-story TDD + PR against `epic/<ID>` | `x-story-implement` | 3 |
| Integrity gate (coverage + DoD + smoke) | `x-internal-epic-integrity-gate` | 4 |
| Epic progress + phase reports | `x-internal-report-write` | 1, 4 |
| Sync `develop → epic/<ID>` before final PR | `x-git-merge` | 5 |
| Final PR `epic/<ID> → develop` (no auto-merge) | `x-pr-create` | 5 |
| Story status + task status atomic writes | `x-internal-status-update` | all (invoked by delegates) |
| Optional PR-comment remediation | `x-pr-fix-epic` | post-4 (Phase 4b) |

## Workflow Overview

```
Phase 0 : Args            (x-internal-args-normalize — ~50 lines)
Phase 1 : Load & Plan     (x-internal-epic-build-plan — ~80 lines)
Phase 2 : Branch Setup    (x-internal-epic-branch-ensure — ~40 lines)
Phase 3 : Execution Loop  (x-story-implement per story, seq or parallel — ~150 lines)
Phase 4 : Integrity Gate  (x-internal-epic-integrity-gate + x-internal-report-write — ~80 lines)
Phase 5 : Final PR        (x-git-merge --source develop --target epic/XXXX + x-pr-create — ~60 lines)
```

Legacy flow (`--legacy-flow` or `flowVersion=1`) skips Phase 2 and Phase 5 entirely; Phase 3 targets `develop` directly.

## Phase 0 — Args Normalization

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-0-Args`

Invoke via Rule 13 Pattern 1 (INLINE-SKILL) against the embedded schema in `references/args-schema.json`:

    Skill(skill: "x-internal-args-normalize", args: "--schema @references/args-schema.json --argv \"{raw argv}\"")

Consume the `{parsed, warnings, errors}` envelope:

1. If `errors` is non-empty → exit `1` with code `ARGS_INVALID` and the error detail.
2. For each warning, print once (deprecated-flag warnings are emitted by the normalizer itself).
3. Extract `epicId` (normalized to 4-digit zero-padded) and the flag map.
4. Mutual-exclusion check: `--phase` and `--story` cannot both be set (normalizer enforces this, but the orchestrator re-validates as defense-in-depth).
5. Detect flow version:
   - If `--legacy-flow=true` → `flowVersion="1"`
   - Else if `plans/epic-XXXX/execution-state.json` exists and its top-level `flowVersion` is `"1"` (or absent, which is treated as `"1"`) → print warning `"[flow-detect] execution-state.json flowVersion=1 — forcing --legacy-flow"` and set `flowVersion="1"`
   - Else → `flowVersion="2"` (new default)
6. Propagate context to subsequent phases: `epicId`, `flowVersion`, parallelMode, autoMergeStrategy, dryRun, resume, phaseFilter, storyFilter, skipReview, strictOverlap, nonInteractive, skipPrComments, revertOnFailure, skipSmoke.

Early exits handled at this phase:

- `EPIC_DIR_MISSING`: `Read("plans/epic-XXXX/")` fails → exit `2`.
- `--dry-run`: defer to Phase 1 (which persists the plan) then stop immediately after Phase 1.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-0-Args ok`

>>> Phase 0 completed. Proceeding to Phase 1...

## Phase 1 — Load & Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-1-Plan`

Invoke `x-internal-epic-build-plan` which handles reading epic-XXXX.md, IMPLEMENTATION-MAP.md, all story-*.md files, building the inter-story DAG via Kahn's algorithm (with cycle detection), optional file-overlap matrix, critical-path computation, and report rendering via `x-internal-report-write`.

    Skill(skill: "x-internal-epic-build-plan", args: "--epic-id <EPIC-ID> --mode <sequential|parallel> --output plans/epic-XXXX/reports/epic-execution-plan-XXXX.md [--strict-overlap]")

Argument mapping:

| Condition | `--mode` |
| :--- | :--- |
| `parallelMode=true` and `flowVersion="2"` | `parallel` |
| otherwise | `sequential` |

On success the sub-skill emits a JSON envelope on stdout:

```json
{
  "status": "OK",
  "epicId": "0049",
  "phases": [[...], [...]],
  "storiesTotal": N,
  "criticalPath": ["story-0049-0001", ...],
  "overlapMatrixPath": "plans/epic-0049/reports/preflight-analysis-phase-1.md" | null,
  "executionPlanPath": "plans/epic-0049/reports/epic-execution-plan-0049.md"
}
```

Consume:

1. Persist the envelope (abridged) in orchestrator memory — never re-read the plan file.
2. On exit `3` (`CYCLIC_DEPENDENCY`) → exit with code `CYCLIC_DEPENDENCY` and the cycle detail.
3. On any other non-zero exit → exit with code `PLAN_BUILD_FAILED`.
4. Apply `--phase` / `--story` filters to the resolved phase array (orchestrator-local; sub-skill still produces the full plan so `execution-state.json` matches reality).
5. If `--dry-run=true` → print `"Dry-run: execution plan saved to <path>. No stories executed."` and **stop here** (exit 0).
6. If `--resume=true`, the sub-skill returns the reclassified story-status map inside the envelope under `resumeProjection` (current `status` per story + PR-verify result). The orchestrator simply uses this projection as the initial iteration filter — no inline `gh pr view` calls.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-1-Plan ok`

>>> Phase 1 completed. Proceeding to Phase 2 (Branch Setup)...

## Phase 2 — Epic Branch Setup

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-2-Branch`

**Skipped entirely when `flowVersion="1"`** (legacy flow); log `"[phase-2] skipped — legacy flow"` and proceed to Phase 3 targeting `develop`.

Otherwise invoke:

    Skill(skill: "x-internal-epic-branch-ensure", args: "--epic-id <EPIC-ID> --base develop --push true")

The sub-skill is idempotent (RULE-001):

- branch absent locally → delegates creation to `x-git-branch --name epic/<EPIC-ID> --base develop`
- branch present locally but not on origin → emits `git push -u origin epic/<EPIC-ID>`
- branch present on both → no-op with `[branch-ensure] epic/<EPIC-ID> already present` log

Consume the single-line JSON envelope `{status, branch, createdLocally, pushedToOrigin}`. On non-zero exit → exit with code `BRANCH_ENSURE_FAILED`.

Record `epicBranch=epic/<EPIC-ID>` in orchestrator memory and propagate to Phase 3 as the target branch for every story-PR.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-2-Branch ok`

>>> Phase 2 completed. Proceeding to Phase 3 (Execution Loop)...

## Phase 3 — Execution Loop (Stories)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-3-Execute`

Iterate the phase-ordered story array from Phase 1. Within each phase batch, either sequentially dispatch each story (`parallelMode=false`, the new default) or launch all phase-batch stories in parallel via sibling `Skill(...)` calls in one assistant message (`parallelMode=true`).

### 3.1 Per-story dispatch contract

For each story, invoke `x-story-implement` with the following argument surface:

    Skill(skill: "x-story-implement", model: "sonnet", args: "<STORY-ID> --target-branch <epicBranch> --auto-merge-strategy <strategy> [--skip-review] [--non-interactive] [--auto-approve-pr]")

Where:

- `<epicBranch>` is `epic/<EPIC-ID>` when `flowVersion="2"`, otherwise `develop`.
- `<strategy>` is `--auto-merge-strategy` (default `merge`); passed through to `x-story-implement`, which forwards to `x-pr-create --auto-merge=<strategy>`.
- `--skip-review` propagates from the epic-level flag.
- `--non-interactive` propagates (required for CI/orchestrated invocations).
- `--auto-approve-pr` propagates (orthogonal to `flowVersion`; controls task-PR-to-parent-branch within each story).

### 3.2 Result envelope

Each `x-story-implement` dispatch returns:

```json
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "storyId": "story-XXXX-YYYY",
  "commitSha": "<sha>",
  "prNumber": 123,
  "prUrl": "https://github.com/.../pull/123",
  "prMergeStatus": "MERGED" | "OPEN" | "CLOSED",
  "coverageLine": 95.8,
  "coverageBranch": 91.2
}
```

The orchestrator records the compact envelope in-memory and persists each status transition via `Skill(skill: "x-internal-status-update", args: "--file plans/epic-XXXX/execution-state.json --type story --id <ID> --field status --value <STATUS>")` — never mutating `execution-state.json` inline.

### 3.3 Phase gating

Before moving to phase N+1, require every story in phase N to be `status=SUCCESS` **and** `prMergeStatus=MERGED`. If any story is `FAILED`, trigger block propagation: every downstream story that transitively depends on a FAILED story is marked `BLOCKED` (status-update via the delegate). On any `FAILED` story the orchestrator aborts Phase 3 with exit code `STORY_FAILED` unless `--revert-on-failure` is set (in which case the failed story's merge commit is reverted via `x-git-merge --source <prev-epic-HEAD> --target epic/<EPIC-ID>` and execution continues). The full retry/circuit-breaker protocol lives in `references/full-protocol.md` §3.

### 3.4 Parallel mode (`--parallel`)

When `parallelMode=true`, sibling `x-story-implement` invocations within a phase batch MUST be emitted in a single assistant message (Rule 13 — SUBAGENT-GENERAL batch). Each dispatch uses `--worktree` so `x-story-implement` provisions its own worktree under `.claude/worktrees/story-XXXX-YYYY/` (Rule 14). Phase-level barrier: the orchestrator waits for all siblings to return before advancing to the next phase batch. Parallelism gate (`x-parallel-eval --scope=epic`) runs inside `x-internal-epic-build-plan` already — the orchestrator does NOT re-invoke it.

### 3.5 Resume (`--resume`)

When the flag is set, Phase 1's envelope already contains `resumeProjection` with reclassified story statuses (PR_CREATED → SUCCESS if PR is merged, IN_PROGRESS → PENDING, etc.). Phase 3 simply skips stories already in `SUCCESS` and enters the loop normally for `PENDING`/`BLOCKED→unblocked` stories. Task-level resume stays inside `x-story-implement` (unchanged).

### 3.6 Partial mode (`--phase` / `--story`)

The resolved phase array from Phase 1 is filtered to the requested subset. No special dispatch logic beyond the filter. The orchestrator does NOT run the integrity gate (Phase 4) in `--story` mode — single-story runs terminate after the story returns (legacy EPIC-0042 behavior preserved).

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-3-Execute ok`

>>> Phase 3 completed. Proceeding to Phase 4 (Integrity Gate)...

## Phase 4 — Integrity Gate + Report

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-4-Gate`

### 4.1 Integrity gate

Invoke on the tip of `epic/<EPIC-ID>` (or `develop` in legacy flow):

    Skill(skill: "x-internal-epic-integrity-gate", args: "--epic-id <EPIC-ID> --branch <epicBranch> [--coverage-threshold-line 95] [--coverage-threshold-branch 90]")

The sub-skill checks out the branch, runs `{{TEST_COMMAND}}` + coverage, runs the declarative DoD checklist (tests present, all tasks DONE, CHANGELOG entry, ADR refs), runs smoke when `{{SMOKE_COMMAND}}` is set (unless `--skip-smoke`), then emits:

```json
{"passed": true|false, "failures": [...], "coverageDelta": {...}, "dodChecklist": [...], "coverageLine": 95.8, "coverageBranch": 91.2}
```

On `passed=false`:

- If `--revert-on-failure` → revert the last merged story via `Skill(skill: "x-git-merge", args: "--source <prior-HEAD> --target epic/<EPIC-ID> --strategy merge --message \"revert: integrity-gate failure\"")`, mark the story `REVERTED` in execution-state, and **re-run the gate once**. On second failure → exit `INTEGRITY_GATE_FAILED`.
- Otherwise dispatch an agent-assisted regression fix via Rule 13 Pattern 2 (SUBAGENT-GENERAL); on return, re-run the gate once. On second failure → exit `INTEGRITY_GATE_FAILED`.

### 4.2 Epic progress report

Invoke `x-internal-report-write` to render `plans/epic-XXXX/reports/epic-execution-report-XXXX.md` from `_TEMPLATE-EPIC-EXECUTION-PLAN.md` with the live story results (status, PR URLs, coverage) collected during Phase 3:

    Skill(skill: "x-internal-report-write", args: "--template _TEMPLATE-EPIC-EXECUTION-PLAN.md --output plans/epic-XXXX/reports/epic-execution-report-XXXX.md --data @plans/epic-XXXX/execution-state.json")

### 4.3 Post-gate PR-comment remediation (optional)

If `--skip-pr-comments` is NOT set, invoke `Skill(skill: "x-pr-fix-epic", args: "<EPIC-ID>")` to consolidate PR review comments across every story PR into a single correction PR. Skipped entirely in legacy flow when all stories already merged into `develop`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-4-Gate ok`

>>> Phase 4 completed. Proceeding to Phase 5 (Final PR)...

## Phase 5 — Final PR (`epic/<EPIC-ID> → develop`)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-implement Phase-5-Final-PR`

**Skipped entirely when `flowVersion="1"`** (legacy flow): log `"[phase-5] skipped — legacy flow (no final PR; stories merged directly into develop)"` and emit final envelope with `finalPrUrl=null`.

### 5.1 Sync `develop → epic/<EPIC-ID>`

Ensure `epic/<EPIC-ID>` is up-to-date with `develop` before opening the final PR (otherwise GitHub will show divergence):

    Skill(skill: "x-git-merge", model: "haiku", args: "--source develop --target epic/<EPIC-ID> --strategy merge")

On conflict (sub-skill returns non-zero with conflict envelope) → exit with code `FINAL_PR_CONFLICTS`. Conflict resolution is human-driven: the orchestrator prints a remediation block and terminates. Re-run with `--resume` after the operator resolves locally and pushes.

### 5.2 Final PR creation (no auto-merge)

Invoke via the extension added in story-0049-0016:

    Skill(skill: "x-pr-create", model: "haiku", args: "--epic-id <EPIC-ID> --head epic/<EPIC-ID> --target-branch develop --auto-merge none --label epic-integration --title \"feat(epic-<EPIC-ID>): integrate all stories\" --description \"Consolidated integration PR for EPIC-<EPIC-ID>. See plans/epic-<EPIC-ID>/reports/epic-execution-report-<EPIC-ID>.md for story breakdown.\"")

Consume `{prUrl, prNumber}`. Record in the final envelope. **Do NOT auto-merge** (manual gate per RULE-004 — the epic PR is the last human review point before `develop` receives the full epic).

### 5.3 Interactive gate (optional)

When stdin is a TTY and `--non-interactive` is NOT set, emit the EPIC-0043 standard 3-option menu via `AskUserQuestion`:

- PROCEED — leave the PR open for human review (default)
- FIX-PR — invoke `Skill(skill: "x-pr-fix", args: "<prNumber>")` against the just-created PR and loop back to the menu
- ABORT — exit 0 leaving the PR in place

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-implement Phase-5-Final-PR ok`

>>> Phase 5 completed. Lifecycle complete.

## Error Envelope

| Scenario | Action |
|----------|--------|
| Epic ID missing / malformed | `x-internal-args-normalize` exits 1 → orchestrator exits `ARGS_INVALID` |
| `plans/epic-XXXX/` absent | Exit `EPIC_DIR_MISSING` |
| IMPLEMENTATION-MAP.md absent | `x-internal-epic-build-plan` exits non-zero → `PLAN_BUILD_FAILED` |
| Cyclic dependency | `x-internal-epic-build-plan` exits 3 → `CYCLIC_DEPENDENCY` |
| Epic branch cannot be ensured | `BRANCH_ENSURE_FAILED` |
| Story failure | Block-propagation + `STORY_FAILED` (unless `--revert-on-failure` triggers recovery) |
| Integrity gate fails twice (with / without revert) | `INTEGRITY_GATE_FAILED` |
| Final PR sync conflict | `FINAL_PR_CONFLICTS` |
| Template missing (`_TEMPLATE-*.md`) | Log WARNING; `x-internal-report-write` degrades to inline format (RULE-012) |
| Sub-skill transient error | Retry once with 2s backoff (max 1 retry); persistent → propagate exit code |
| 3 consecutive story failures in same phase | Circuit-breaker trip; `AskUserQuestion` in interactive mode, else `STORY_FAILED` |

Full retry/backoff/circuit-breaker schedule and `SubagentResult` error shape live in `references/full-protocol.md` §4.

## Backward Compatibility (`--legacy-flow` / `flowVersion=1`)

| Phase | Legacy (v1) behavior | New (v2) behavior |
|-------|----------------------|-------------------|
| 0 | Same (args parsed identically) | Same |
| 1 | `--mode sequential` always; execution plan still rendered | `--mode parallel` when `--parallel`; otherwise sequential |
| 2 | **NO-OP** (stories target `develop`) | `epic/<EPIC-ID>` created |
| 3 | Stories target `develop`; merged directly as PRs close | Stories target `epic/<EPIC-ID>`; auto-merged there |
| 4 | Integrity gate runs on `develop` after last merge | Integrity gate runs on `epic/<EPIC-ID>` |
| 5 | **NO-OP** (no final PR; stories already in develop) | Final `epic/<EPIC-ID> → develop` PR created, NOT auto-merged |

`flowVersion` is persisted in `execution-state.json` at the top level — set by `x-internal-epic-build-plan` on first write, read by `--resume`. This preserves zero regression for pre-EPIC-0049 checkpoints.

## Idempotency (RULE-002)

- Execution plan: regenerated only when `mtime(IMPLEMENTATION-MAP.md) > mtime(epic-execution-plan.md)` (enforced by `x-internal-epic-build-plan`).
- `epic/<EPIC-ID>` branch: ensured idempotently (RULE-001; enforced by `x-internal-epic-branch-ensure`).
- Story status mutations: atomic via `x-internal-status-update` (flock-protected).
- Final PR: re-running Phase 5 against an already-open PR is a no-op (sub-skill `x-pr-create` detects and returns the existing `{prUrl, prNumber}`).

## Integration Notes

| Skill | Relationship | Context |
|-------|--------------|---------|
| `x-internal-args-normalize` | invokes (Phase 0) | Argv parsing, deprecation warnings, flag validation |
| `x-internal-epic-build-plan` | invokes (Phase 1) | Epic metadata, DAG, critical path, execution plan render |
| `x-internal-epic-branch-ensure` | invokes (Phase 2) | Idempotent `epic/<EPIC-ID>` creation (RULE-001) |
| `x-story-implement` | invokes (Phase 3, per story) | Story-level TDD + PR against `epic/<EPIC-ID>` |
| `x-internal-epic-integrity-gate` | invokes (Phase 4) | Coverage + DoD + smoke gate |
| `x-internal-report-write` | invokes (Phases 1, 4) | Template-based report rendering |
| `x-internal-status-update` | invokes (all phases) | Atomic `execution-state.json` mutations |
| `x-git-merge` | invokes (Phase 5.1) | Sync `develop → epic/<EPIC-ID>` before final PR |
| `x-pr-create` | invokes (Phase 5.2) | Final PR `epic/<EPIC-ID> → develop` (no auto-merge) |
| `x-pr-fix-epic` | invokes (Phase 4.3, optional) | Consolidated PR-comment remediation |
| `x-parallel-eval` | referenced (inside `x-internal-epic-build-plan` only) | Parallelism gate — never invoked inline by orchestrator |
| `x-epic-orchestrate` | called by | Epic-orchestration wrapper delegates full-run here |

All `{{PLACEHOLDER}}` tokens (`{{TEST_COMMAND}}`, `{{SMOKE_COMMAND}}`, `{{COVERAGE_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation.

## Full Protocol

Minimum viable orchestrator contract above. Complete Phase 3 retry-backoff schedule + circuit-breaker rules, Phase 4 gate failure recovery algorithm + regression-fix agent prompt, Phase 5 TTY-detection + AskUserQuestion template, legacy-flow detailed phase-by-phase behavior, resume workflow for both `flowVersion`s, and the `--auto-approve-pr` propagation contract all live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012.
