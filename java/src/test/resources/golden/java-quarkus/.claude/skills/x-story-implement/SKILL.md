---
name: x-story-implement
model: sonnet
description: "Thin orchestrator (~320 lines — story-0049-0019 refactor) that drives a story end-to-end via 4 delegated phases: Phase 0 (args via x-internal-args-normalize + context via x-internal-story-load-context + resume via x-internal-story-resume), Phase 1 (parallel planning via x-internal-story-build-plan), Phase 2 (task execution loop via x-task-implement per task, then final story PR via x-pr-create), Phase 3 (verify via x-internal-story-verify + report via x-internal-story-report + optional worktree cleanup via x-git-worktree). New EPIC-0049 flags --target-branch / --auto-merge / --epic-id propagate OO-style to x-task-implement and x-pr-create. Backward compatible: absent flags preserve legacy EPIC-0048 behavior (target=develop, auto-merge=none)."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, TaskCreate, TaskUpdate, AskUserQuestion
argument-hint: "[STORY-ID] [--target-branch <branch>] [--auto-merge <merge|squash|rebase|none>] [--epic-id <XXXX>] [--auto-approve-pr] [--task TASK-ID] [--resume] [--skip-verification] [--skip-smoke] [--skip-review] [--full-lifecycle] [--worktree] [--non-interactive] [--no-auto-remediation] [--no-ci-watch]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## CONTEXT MANAGEMENT

- **Task results:** record only the compact envelope returned by `x-task-implement` (`status`, `taskId`, `commitSha`, `coverageLine`, `coverageBranch`). Never accumulate full TDD cycle logs.
- **Plan files:** reference by file path only (`plans/epic-XXXX/plans/*.md`). Do NOT re-read plans after Phase 1.
- **Execution state:** never mutate `execution-state.json` inline — delegate every transition to `x-internal-status-update`.
- **Review outputs:** reference by file path + score summary only; never load full review content into orchestrator context.
- **Story file:** read once during Phase 0 via `x-internal-story-load-context`; the envelope is the single source of truth thereafter.

# Skill: Story Implementation (Thin Orchestrator — ADR-0012 + EPIC-0049)

Orchestrate the end-to-end implementation of a single story by delegating every substantive responsibility to specialized sub-skills. The orchestrator's inline responsibilities are: parse argv (via delegate), load story context (via delegate), drive the task loop (via `x-task-implement`), and close with the story-level PR. All git/gh/mvn side-effects live inside the delegates — this SKILL.md contains zero direct shell invocations of those tools (only `Read`/`Glob` for local file discovery plus `Skill`/`Agent` for delegation).

**EPIC-0049 flag propagation (OO-style — RULE-009):**

| Flag | Default | Propagation |
| :--- | :--- | :--- |
| `--target-branch <branch>` | `develop` (legacy) | → `x-task-implement --target-branch`, `x-pr-create --target-branch` |
| `--auto-merge <strategy>` | `none` (legacy) | → `x-pr-create --auto-merge` |
| `--epic-id <ID>` | auto-derived from storyId | → `x-pr-create --epic-id` (adds `epic-XXXX` label) |

When all three flags are absent, behavior is identical to EPIC-0048 (backward compat — RULE-008).

## Triggers

- `/x-story-implement story-XXXX-YYYY` — full lifecycle (legacy default: target=develop, no auto-merge)
- `/x-story-implement story-XXXX-YYYY --target-branch epic/0049 --auto-merge merge --epic-id 0049` — orchestrator-propagated run targeting an epic branch
- `/x-story-implement story-XXXX-YYYY --task TASK-NNN` — execute only a specific task (resume / targeted)
- `/x-story-implement story-XXXX-YYYY --resume` — continue from `execution-state.json` via `x-internal-story-resume`
- `/x-story-implement story-XXXX-YYYY --auto-approve-pr` — parent-branch mode (RULE-004)
- `/x-story-implement story-XXXX-YYYY --worktree` — standalone opt-in worktree (ADR-0004 Mode 2)
- `/x-story-implement story-XXXX-YYYY --non-interactive` — skip interactive gate menus (CI / orchestrated calls)

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `STORY-ID` | String | — | Positional, required. Pattern `story-XXXX-YYYY`. |
| `--target-branch` | String | `develop` | Base branch for task + story PRs. Propagated to `x-task-implement` + `x-pr-create`. |
| `--auto-merge` | Enum | `none` | `merge\|squash\|rebase\|none`. Propagated to `x-pr-create`. Requires `--target-branch` when not `none`. |
| `--epic-id` | String(4) | auto | Auto-derived from story prefix. Propagated to `x-pr-create` (adds `epic-XXXX` label). |
| `--auto-approve-pr` | Boolean | `false` | Parent-branch mode (RULE-004): task PRs target `feat/story-...`. |
| `--task` | String | — | Execute only `TASK-XXXX-YYYY-NNN`. |
| `--resume` | Boolean | `false` | Delegates resume-point detection to `x-internal-story-resume`. |
| `--skip-verification` | Boolean | `false` | **Recovery-only.** Skips Phase 3; flagged outside `## Recovery` blocks. |
| `--skip-smoke`, `--skip-review` | Boolean | `false` | Bypass smoke gate / specialist + TL reviews. |
| `--full-lifecycle`, `--worktree`, `--non-interactive` | Boolean | `false` | Full execution / standalone worktree / CI mode. |
| `--no-auto-remediation`, `--no-ci-watch` | Boolean | `false` | Skip Step 3.5 remediation / Rule 21 CI-watch. |

**Deprecated (no-op, warn-once):** `--manual-contract-approval`, `--manual-task-approval` (both since EPIC-0043).

## Output Contract

| Field | Type | Description |
|-------|------|-------------|
| `storyId`, `epicId` | String | Story + auto-derived epic ID |
| `status` | Enum | `SUCCESS \| FAILED \| PARTIAL` |
| `targetBranch`, `autoMergeStrategy` | String, Enum | Resolved base branch + merge strategy |
| `tasksExecuted` | List<{id, status, commitSha, prNumber}> | One entry per task dispatched |
| `prNumber` / `prUrl` | Integer\|null / String\|null | Story-level PR; null without `--auto-approve-pr` |
| `verifyPassed` | Boolean | Phase 3 verify-gate `passed` |
| `reportPath` | String | Final report from `x-internal-story-report` |
| `coverageLine` / `coverageBranch` | Number\|null | Filtered coverage |

## Error Codes

| Exit | Code | Condition |
|------|------|-----------|
| 1 | `ARGS_INVALID` | `x-internal-args-normalize` exit 1 |
| 2 | `STORY_NOT_LOADABLE` | `x-internal-story-load-context` non-zero |
| 3 | `PLAN_FAILED` | `x-internal-story-build-plan` non-zero |
| 4 | `TASK_FAILED` | One or more tasks returned `FAILED` |
| 5 | `VERIFY_FAILED` | `x-internal-story-verify` `passed=false` |
| 6 | `PR_CREATE_FAILED` | `x-pr-create` non-zero on story-level PR |

## Workflow Overview

```
Phase 0 : Args, Context & Resume  (x-internal-args-normalize + load-context + resume — ~80 lines)
Phase 1 : Plan                    (x-internal-story-build-plan — ~50 lines)
Phase 2 : Task Execution Loop     (x-task-implement per task + x-pr-create — ~120 lines)
Phase 3 : Verify, Report, Cleanup (x-internal-story-verify + x-internal-story-report + x-git-worktree remove — ~70 lines)
```

## CRITICAL EXECUTION RULE

**4 phases (0–3). ALL phases through Phase 2 are mandatory. NEVER stop before Phase 2 completes.**

Print markers between phases:

- After Phase 0: `>>> Phase 0 completed. Proceeding to Phase 1 (Plan)...`
- After Phase 1: `>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...`
- After Phase 2: `>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verify & Report)...`
- After Phase 3: `>>> Phase 3 completed. Lifecycle complete.`

## Phase 0 — Args, Context & Resume

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-0-Prepare`

### 0.1 Args normalization

Invoke via Rule 13 Pattern 1 (INLINE-SKILL) against the embedded schema:

    Skill(skill: "x-internal-args-normalize", args: "--schema @references/args-schema.json --argv \"{raw argv}\"")

Consume the `{parsed, warnings, errors}` envelope:

1. If `errors` is non-empty → exit `1` with code `ARGS_INVALID` and the detail.
2. Print each warning once (deprecation warnings come from the normalizer).
3. Extract `storyId`, `targetBranch`, `autoMerge`, `epicId` (auto-derived from storyId prefix when absent), and the remaining flag map.
4. Propagate to subsequent phases: `storyId`, `epicId`, `targetBranch`, `autoMerge`, `skipReview`, `skipVerification`, `skipSmoke`, `autoApprovePr`, `nonInteractive`, `worktree`, `task`, `resume`, `noAutoRemediation`, `noCiWatch`, `fullLifecycle`.

### 0.2 Load story context

Invoke:

    Skill(skill: "x-internal-story-load-context", args: "--story-id <STORY-ID> --epic-id <EPIC-ID>")

The sub-skill reads the story file, validates predecessor-story completion via `execution-state.json`, runs artifact pre-checks (mtime-based staleness across the 7 planning artifacts — test plan / arch plan / impl plan / task breakdown / security / compliance / per-task plans), classifies scope (SIMPLE / STANDARD / COMPLEX), and detects planning mode (PRE_PLANNED / HYBRID / INLINE). Consume the envelope `{storyPath, tasksCount, scope, planningMode, predecessorsComplete, artifactFreshness[]}`.

On non-zero exit → exit with code `STORY_NOT_LOADABLE`.

### 0.3 Worktree-first branch decision (Rule 14 + ADR-0004)

Invoke `Skill(skill: "x-git-worktree", args: "detect-context")` and apply the 3-way decision (REUSE | CREATE | LEGACY); set `STORY_OWNS_WORKTREE` in-memory (true only for standalone CREATE mode — `--worktree` provided and not already inside a worktree). Full decision table in `references/full-protocol.md` §1.

### 0.4 Resume (conditional on `--resume`)

When `--resume=true`, invoke:

    Skill(skill: "x-internal-story-resume", args: "--story-id <STORY-ID> --epic-id <EPIC-ID>")

Consume the envelope `{resumePoint, tasksCompleted[], tasksPending[], lastCommitSha, staleWarnings[]}`. Use `tasksPending` as the iteration filter for Phase 2 — tasks in `tasksCompleted` are skipped. Print each `staleWarning` once.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-0-Prepare ok`

>>> Phase 0 completed. Proceeding to Phase 1 (Plan)...

## Phase 0.5 — API-First Contract Generation (Conditional)

Activates ONLY when `has_contract_interfaces=True` (story declares interfaces of type `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`). Generates draft OpenAPI 3.1 / Protobuf 3 / AsyncAPI 2.6 in `contracts/{STORY_ID}-*.yaml|proto`, validates via `x-test-contract-lint`, and pauses at **Step 0.5.4** via `AskUserQuestion` (EPIC-0043 gate — replaces legacy `CONTRACT PENDING APPROVAL` text). Full interface-to-format mapping table and generator details live in `references/full-protocol.md` §2.

## Phase 1 — Plan

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-1-Plan`

**Skipped entirely when `planningMode=PRE_PLANNED`** (all 7 artifacts fresh — logged `"[phase-1] skipped — PRE_PLANNED"` and proceeding to Phase 2).

Otherwise invoke:

    Skill(skill: "x-internal-story-build-plan", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> --scope <SCOPE> [--skip-review]")

The sub-skill orchestrates parallel story-planning (Phase 1 carve-out): invokes `x-arch-plan` (Step 1A) then dispatches up to 5 sibling Agent subagents in ONE assistant message for implementation plan, test plan, task breakdown, security assessment, and compliance assessment (Steps 1B-1F). Scope-aware: SIMPLE skips 1E/1F.

Consume the envelope `{artifacts: [{name, path, status}], planningMode, scopeResolved}`. On non-zero exit → exit with code `PLAN_FAILED`. Persist the envelope in orchestrator memory — never re-read the artifact files.

### Parallelism gate (RULE-004 hotspots)

Invoke `Skill(skill: "x-parallel-eval", args: "--scope=story --story=<STORY-ID>")` (EPIC-0041 / story-0041-0006). On detected hard/regen collision, degrade the affected wave to serial and record `ExecutionState.parallelismDowngrades`. Full matrix in `references/full-protocol.md` §3.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-1-Plan ok`

>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...

## Phase 2 — Task Execution Loop (RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-2-Implement`

Read tasks from `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (Section 8 fallback when absent). Apply the `tasksPending` filter from Phase 0.4 when resuming. For each `TASK-XXXX-YYYY-NNN` not in `DONE`/`BLOCKED`:

### 2.1 Per-task dispatch

1. Check dependencies against `execution-state.json`; unresolved → mark `BLOCKED` via `x-internal-status-update` and skip.
2. **Dispatch TDD:** invoke per Rule 13 Pattern 1:

        Skill(skill: "x-task-implement", model: "sonnet", args: "<TASK-ID> --orchestrated --target-branch <targetBranch> [--auto-merge <strategy>] [--epic-id <EPIC-ID>] [--auto-approve-pr] [--non-interactive]")

   The child runs RED → GREEN → REFACTOR, commits atomically via `x-git-commit`, and pushes the task branch `feat/task-XXXX-YYYY-NNN-desc`. It returns `{status, taskId, commitSha, branchName, coverageLine, coverageBranch}`.

3. **CI-watch (Rule 21):** unless `--no-ci-watch`, invoke:

        Skill(skill: "x-pr-watch-ci", args: "--branch <branchName>")

   Poll CI + classify Copilot reviews; block until stable (8 exit codes — see Rule 21).

4. **PR creation:** invoke with OO-propagated flags (RULE-009):

        Skill(skill: "x-pr-create", model: "haiku", args: "<TASK-ID> --target-branch <targetBranch> --auto-merge <strategy> --epic-id <EPIC-ID> [--auto-approve-pr]")

   When `--auto-approve-pr`, the effective task-PR target is the parent story branch (`feat/story-...`); otherwise the propagated `--target-branch`. Consume `{prUrl, prNumber, prMergeStatus}`.

5. **Status transition:** record each task transition via:

        Skill(skill: "x-internal-status-update", args: "--file plans/epic-XXXX/execution-state.json --type task --id <TASK-ID> --field status --value <STATUS>")

### 2.2 Fail-fast + story-level PR

Each `x-task-implement` dispatch returns `{status, taskId, commitSha, branchName, prNumber, prUrl, coverageLine, coverageBranch}`. On any `status=FAILED`, mark downstream dependants `BLOCKED` and exit Phase 2 with `TASK_FAILED` unless invoked with `--task` (single-task mode terminates at the failure).

When `--auto-approve-pr` was set, every task PR targeted the parent story branch `feat/story-XXXX-YYYY-...`. After all tasks succeed, push the parent branch and create the story-level PR with OO-propagated flags:

    Skill(skill: "x-pr-create", model: "haiku", args: "--story-id <STORY-ID> --head feat/story-<STORY-ID> --target-branch <targetBranch> --auto-merge <strategy> --epic-id <EPIC-ID>")

Without `--auto-approve-pr`, each task PR is the unit of delivery — no story-level PR is created. On `x-pr-create` non-zero exit → `PR_CREATE_FAILED`. Fallback G1-G7 (no test plan / no formal tasks) lives in `references/full-protocol.md` §4.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-2-Implement ok`

>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verify & Report)...

## Phase 3 — Verify, Report & Cleanup

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-3-Verify`

**Skipped only under `--skip-verification`** (recovery-only — see Rule 22). The skip is flagged by `LifecycleIntegrityAuditTest` outside a `## Recovery` block.

### 3.1 Verify gate

Invoke:

    Skill(skill: "x-internal-story-verify", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> [--coverage-threshold-line 95] [--coverage-threshold-branch 90]")

The sub-skill identifies files touched by the story, runs the scoped test suite + coverage, performs cross-file consistency checks (constructor / return-type uniformity per role), runs smoke (unless `--skip-smoke`), and validates every Section 7 Gherkin scenario has a matching acceptance test.

Consume `{passed, coverageDelta, failures, acCheckResults, coverageLine, coverageBranch}`. On `passed=false` → exit with code `VERIFY_FAILED` and include `failures[0]` in the message.

### 3.2 Specialist + Tech-Lead reviews (unless `--skip-review`)

Invoke in sequence:

    Skill(skill: "x-review", model: "sonnet", args: "<STORY-ID>")
    Skill(skill: "x-review-pr", model: "sonnet", args: "<STORY-ID>")

On Tech-Lead GO, optionally run PR-comment remediation via `Skill(skill: "x-pr-fix", args: "<prNumber>")` unless `--no-auto-remediation`. On NO-GO, fix-and-review up to 2 cycles; persistent NO-GO keeps `verifyPassed=true` but flags the story with a WARNING in the final report (human gate downstream).

### 3.3 Final report

Invoke:

    Skill(skill: "x-internal-story-report", args: "--story-id <STORY-ID> --epic-id <EPIC-ID> --output plans/epic-XXXX/reports/story-completion-report-STORY-ID.md")

The sub-skill reads `execution-state.json`, collects per-task status + commitSha + PR metadata, coverage delta, and review findings, then renders via `x-internal-report-write` with `_TEMPLATE-STORY-COMPLETION-REPORT.md`.

Consume `{status, path}`. Record `reportPath` in the final envelope.

### 3.4 Status finalize

Finalize story state atomically via `x-internal-status-update`:

- IMPLEMENTATION-MAP Status column → `Concluída`
- Story `**Status:**` header → `Concluída`
- Jira transition (if key present) → `Done`
- `execution-state.json` story-level status → `COMPLETE`

### 3.5 Worktree cleanup (standalone Mode 2 only)

Gated by `STORY_OWNS_WORKTREE` from Phase 0.3:

- `true` + verify passed → `Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")` then re-sync `develop` in main checkout.
- `true` + verify failed → preserve worktree for diagnosis (Rule 14 §4).
- `false` (Modes 1 / 3) → skip removal.

Full decision table in `references/full-protocol.md` §5.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-3-Verify ok`

>>> Phase 3 completed. Lifecycle complete.

## Error Envelope

| Scenario | Action |
|----------|--------|
| Args invalid | `x-internal-args-normalize` non-zero → `ARGS_INVALID` |
| Story file not found / dependency incomplete | `x-internal-story-load-context` non-zero → `STORY_NOT_LOADABLE` |
| Task dependency unresolved | Mark `BLOCKED` via `x-internal-status-update`; continue |
| `x-task-implement` fails | Mark `FAILED`; block propagation; `TASK_FAILED` (unless `--task`) |
| Coverage / AC / consistency failure | `x-internal-story-verify` `passed=false` → `VERIFY_FAILED` |
| `x-pr-create` fails | Task → `FAILED`; story-level → `PR_CREATE_FAILED` |
| `x-pr-fix` compile regression | ABORT Step 3.2 with `PR_FIX_COMPILE_REGRESSION` |
| Template missing (RULE-012) | WARN; `x-internal-report-write` degrades to inline format |
| Corrupted `execution-state.json` on resume | `x-internal-story-resume` reinitializes |
| Review NO-GO > 2 cycles | Keep `verifyPassed=true`; flag human-gate WARNING |
| Sub-skill transient error | Retry once with 2s backoff; persistent → propagate |

Full retry/backoff schedule + `SubagentResult` error shape live in `references/full-protocol.md` §6.

## Backward Compatibility (RULE-008)

| Scenario | Resolved behavior |
|---|---|
| No new flags (`--target-branch` / `--auto-merge` / `--epic-id` all absent) | `targetBranch=develop`, `autoMerge=none`, `epicId` auto-derived from story prefix. Story + task PRs target `develop` with no auto-merge — identical to EPIC-0048. |
| `--target-branch epic/0049` alone | Task + story PRs target `epic/0049`; `autoMerge=none` (manual merge preserved). |
| `--target-branch epic/0049 --auto-merge merge` | Task + story PRs target `epic/0049` with auto-merge; `epic-id` auto-derived. |
| `--auto-merge` set without `--target-branch` | `ARGS_INVALID` (mutex validation — `auto-merge` requires explicit target). Matches `x-pr-create` mutex. |

## Idempotency (RULE-002)

- Story loading: no mutation (`x-internal-story-load-context` is read-only).
- Planning artifacts: regenerated only when `mtime(story) > mtime(artifact)` (enforced by `x-internal-story-build-plan`).
- Task dispatch: `x-task-implement` detects already-merged PRs and short-circuits (delegated).
- Status mutations: atomic via `x-internal-status-update` (flock-protected).
- Story-level PR (`--auto-approve-pr` only): re-running after PR exists is a no-op (sub-skill `x-pr-create` detects and returns the existing `{prUrl, prNumber}`).

## Integration Notes

| Skill | Phase | Context |
|-------|-------|---------|
| `x-internal-args-normalize` | 0.1 | Argv parsing + deprecation warnings |
| `x-internal-story-load-context` | 0.2 | Story load + artifact pre-checks + scope + planning-mode |
| `x-internal-story-resume` | 0.4 (cond.) | Resume-point detection |
| `x-internal-story-build-plan` | 1 | Parallel planning (arch + test + decomposition + security + compliance) |
| `x-task-implement` | 2 (per task) | TDD Red-Green-Refactor + atomic commit + push |
| `x-pr-create` | 2 (per task + story) | PR creation with OO-propagated flags |
| `x-pr-watch-ci` | 2 (per task) | CI polling + Copilot review (Rule 21) |
| `x-parallel-eval` | 1 | Collision matrix + serial-degradation |
| `x-review` / `x-review-pr` / `x-pr-fix` | 3.2 | Specialist + TL reviews + post-gate remediation |
| `x-internal-story-verify` | 3.1 | Verify gate (coverage + DoD + consistency + smoke + AC) |
| `x-internal-story-report` | 3.3 | Final story-completion report |
| `x-internal-status-update` | all | Atomic `execution-state.json` mutations |
| `x-git-worktree` | 0.3 + 3.5 | Worktree context detection + creator-owned removal |
| `x-epic-implement` | caller | Epic orchestrator delegates per-story execution here |

All `{{PLACEHOLDER}}` tokens (`{{TEST_COMMAND}}`, `{{COVERAGE_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — NOT resolved at generation time.

## Full Protocol

Minimum viable orchestrator contract above. Complete Phase 0.3 worktree-mode decision table, Phase 1 parallelism matrix, Phase 2 fallback G1-G7 (no test plan / no formal tasks), Phase 3 verify-gate schemas + specialist-review prompt templates + TL-review NO-GO cycle protocol, resume workflow detail, retry/backoff/circuit-breaker schedule, and the `SubagentResult` error shape all live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012.
