---
name: x-story-implement
description: "Orchestrates the complete feature implementation cycle with task-centric workflow: branch creation, planning, per-task TDD execution with individual PRs and approval gates, story-level verification, and final cleanup. Schema-aware: v1 (legacy) runs the monolithic coalesce-ad-hoc flow; v2 (EPIC-0038) reads task-implementation-map-STORY-*.md and dispatches x-task-implement in waves (declared parallelism) — ending the 'task embedded in story' anti-pattern. Delegates to x-test-tdd, x-git-commit, x-pr-create, and (v2) x-task-implement."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, TaskCreate, TaskUpdate, AskUserQuestion
argument-hint: "[STORY-ID or feature-name] [--auto-approve-pr] [--task TASK-ID] [--skip-verification] [--skip-smoke] [--full-lifecycle] [--worktree] [--non-interactive] [--manual-contract-approval] [--manual-task-approval] [--no-auto-remediation] [--no-ci-watch]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## CONTEXT MANAGEMENT

- **Task results:** after each task completes via `x-test-tdd`, record only the compact result (status, commitSha, coverage). Do NOT accumulate full TDD cycle logs in the orchestrator context.
- **Plan files:** when checking artifact staleness (Phase 0), use file metadata (mtime) not full content reads.
- **Review outputs:** reference by file path + score summary only. Do NOT read full review content into orchestrator context.
- **Story files:** read once during Phase 0; extract only acceptance criteria, dependencies, tasks. Do NOT re-read in later phases.

# Skill: Feature Lifecycle (Task-Centric Orchestrator, slim — ADR-0012)

Orchestrate the complete feature implementation cycle using a task-centric workflow. Each task in the story produces its own branch, PR, and approval gate. The lifecycle delegates TDD implementation to `x-test-tdd`, commits to `x-git-commit`, and PR creation to `x-pr-create`. Phase 2 is the Task Execution Loop; Phase 3 is Story-Level Verification.

## Triggers

- `/x-story-implement story-XXXX-YYYY` — full lifecycle for a planned story
- `/x-story-implement story-XXXX-YYYY --task TASK-NNN` — execute only a specific task (resume / targeted)
- `/x-story-implement story-XXXX-YYYY --auto-approve-pr` — parent-branch mode (RULE-004)
- `/x-story-implement story-XXXX-YYYY --worktree` — standalone opt-in worktree (ADR-0004 Mode 2)
- `/x-story-implement story-XXXX-YYYY --non-interactive` — skip all gate menus (CI / orchestrated calls)

## Parameters

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--auto-approve-pr` | Boolean | false | Parent-branch mode (RULE-004): task PRs target `feat/story-...` branch, story-level PR human-reviewed |
| `--task` | String | — | Execute only a specific task (`TASK-XXXX-YYYY-NNN`) |
| `--skip-verification` | Boolean | false | **Recovery-only — NEVER use in happy path.** Skips Phase 3 incl. status-finalize (3.8.1–3.8.5). Flagged by LifecycleIntegrityAuditTest outside `## Recovery` contexts |
| `--full-lifecycle` | Boolean | false | Force full execution regardless of scope tier |
| `--worktree` | Boolean | false | Standalone opt-in worktree (ADR-0004 §D2); ignored when already inside a worktree (Rule 14 §3) |
| `--non-interactive` | Boolean | false | Skip interactive gate menus; auto-approve. For CI/automation and orchestrated calls |
| `--manual-contract-approval` | Boolean | false | **DEPRECATED (EPIC-0043).** No-op; emits one-time warning |
| `--manual-task-approval` | Boolean | false | **DEPRECATED (EPIC-0043).** No-op; emits one-time warning |
| `--no-auto-remediation` | Boolean | false | Skip agent-dispatched remediation in Step 3.5 |
| `--no-ci-watch` | Boolean | false | Skip CI-Watch step (2.2.8.5) |
| `--skip-smoke` | Boolean | false | Bypass smoke gate (EPIC-0042); advisory only |

## Output Contract

- **Execution state:** `plans/epic-XXXX/execution-state.json` with per-task status (`PENDING|IN_PROGRESS|PR_CREATED|PR_APPROVED|PR_MERGED|DONE|FAILED|BLOCKED`), branch, prNumber, prUrl, timestamps.
- **Per-task artifact:** one branch `feat/task-XXXX-YYYY-NNN-desc`, N atomic commits via `x-test-tdd` + `x-git-commit`, one PR.
- **Story-level artifacts:** review dashboard at `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`; remediation tracker `remediation-story-XXXX-YYYY.md`; Tech Lead review `techlead-review-story-XXXX-YYYY.md`.
- **Status sync:** IMPLEMENTATION-MAP Status column + story `**Status:**` header + Jira transition (if key present) all moved to `Concluida` / `Done` at end of Phase 3.
- **Phase signalling:** `>>> Phase N completed` markers printed between phases (see §CRITICAL EXECUTION RULE below).

## CRITICAL EXECUTION RULE

**3 phases (0–2) + optional Phase 3. ALL phases through Phase 2 are mandatory. NEVER stop before Phase 2 completes.**

Print markers between phases:

- After Phase 0: `>>> Phase 0 completed. Proceeding to Phase 1...`
- After Phase 1: `>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...`
- After Phase 2: `>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verification)...`
- After Phase 3: `>>> Phase 3 completed. Lifecycle complete.`

## Workflow Overview

```
Phase 0 : Preparation             (inline; artifact pre-checks + resume + worktree mode)
Phase 0.5: API-First Contract     (conditional on has_contract_interfaces)
Phase 1 : Architecture Planning   (Phase 1A Skill(x-arch-plan) or Agent fallback)
Phases 1B-1F: Parallel Planning   (up to 5 planners in ONE assistant message)
Phase 1.5: Parallelism Gate       (Skill(x-parallel-eval) — EPIC-0041 story-0041-0006)
Phase 2 : Task Execution Loop     (per task: branch → x-test-tdd → x-pr-create → approval gate)
Phase 3 : Story-Level Verification (coverage, review, tech-lead, PR, status-finalize, cleanup)
```

**RULE-001 — Task as Unit of Delivery.** Each task = 1 branch = 1 PR. Delegation map:

| Concern | Skill | Invoked By |
|---------|-------|------------|
| TDD implementation | `x-test-tdd` | Phase 2 (per task) |
| Atomic commits | `x-git-commit` | `x-test-tdd` (per cycle) |
| PR creation | `x-pr-create` | Phase 2 (per task) |
| Code formatting | `x-code-format` | `x-git-commit` (pre-commit chain) |
| Code linting | `x-code-lint` | `x-git-commit` (pre-commit chain) |
| Task planning | `x-task-plan` | Phase 1 (if not PRE_PLANNED) |

## Phase 0 — Preparation (Orchestrator Inline)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-0-Prepare`

1. Open tracking: `TaskCreate(description: "Phase 0: Preparation — Story {storyId}")` → record as `phase0TaskId`.
2. Read story file; extract acceptance criteria, sub-tasks, dependencies.
3. Verify predecessor stories complete; extract epic ID from story ID.
4. **Artifact pre-checks (RULE-002 idempotency):** for each of the 7 artifact types (test plan / architecture plan / implementation plan / task breakdown / security / compliance / per-task plans) compare `mtime(story)` vs `mtime(artifact)` — fresh ⇒ reuse, stale ⇒ regen, missing ⇒ generate.
5. **Worktree-first branch creation (Rule 14 + ADR-0004):** invoke `Skill(skill: "x-git-worktree", args: "detect-context")`, apply the 3-way decision (REUSE | CREATE | LEGACY), set `STORY_OWNS_WORKTREE` in-memory (true only for standalone CREATE mode). Full decision table + invariants in `references/full-protocol.md` §1.
6. **Scope assessment:** classify as SIMPLE / STANDARD / COMPLEX (default STANDARD if unclassifiable). SIMPLE skips Phase 1B-1E; COMPLEX pauses after Phase 2 for stakeholder review.
7. **Planning mode detection:** PRE_PLANNED (all 7 artifacts fresh — skip Phase 1 entirely) / HYBRID (some missing) / INLINE (none exist). Log detected mode.
8. **Resume detection:** if `execution-state.json` exists, reclassify each task's status by checking actual PR state via `gh pr view`.
9. Close tracking: `TaskUpdate(id: phase0TaskId, status: "completed")`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-0-Prepare ok`

## Phase 0.5 — API-First Contract Generation (Conditional)

Activates ONLY when story declares interfaces of type `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`. Generates draft OpenAPI 3.1 / Protobuf 3 / AsyncAPI 2.6 in `contracts/{STORY_ID}-*.yaml|proto`, validates via available linter, auto-approves on success (EPIC-0042 default). `--manual-contract-approval` pauses for approval.

## Phase 1 — Architecture Planning + Parallel Planning

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-1-Plan`

Open tracking: `TaskCreate(description: "Phase 1: Planning — Story {storyId}")` → `phase1TaskId`.

**Phase 1A — Architecture plan:** invoke `Skill(skill: "x-arch-plan", args: "{STORY_PATH}")` unless scope is bugfix/docs. On failure, log WARNING and proceed to 1B with inline fallback.

**Phases 1B-1F — Parallel planning in ONE assistant message (Rule 13 SUBAGENT-GENERAL + INLINE-SKILL siblings):**

- 1B Implementation Plan — `Agent(subagent_type:"general-purpose", description:"...", prompt:"<Senior Architect — reads templates, produces plan-story-...md>")`.
- 1B Test Plan — `Skill(skill: "x-test-plan", args: "{STORY_PATH}")`.
- 1C Task Decomposition — `Skill(skill: "x-lib-task-decomposer", args: "{STORY_PATH}")`.
- 1D Event Schema (if `event_driven`) — `Agent(subagent_type:"general-purpose", ...)` for Event Engineer.
- 1E Security Assessment — `Skill(skill: "x-threat-model", args: "{STORY_PATH}")` (fallback: Security Engineer subagent).
- 1F Compliance Assessment (if compliance active) — `Agent(subagent_type:"general-purpose", ...)`.

Planning follows the Batch A (all `TaskCreate` + invocations in ONE message) / wait / Batch B (all `TaskUpdate` in ONE message) parallelism protocol. Per-planner prompt contents, skip-if-reuse logic, and template references live in `references/full-protocol.md` §2.

Close tracking: `TaskUpdate(id: phase1TaskId, status: "completed")`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-1-Plan ok`

## Phase 1.5 — Parallelism Gate

Invoke `Skill(skill: "x-parallel-eval", args: "--scope=story --story={STORY_ID}")` (EPIC-0041 / story-0041-0006). On detected hard/regen collision, degrade the affected wave to serial and record `ExecutionState.parallelismDowngrades`. RULE-004 hotspots apply. Full matrix and degradation rationale in `references/full-protocol.md` §3.

## Phase 2 — Task Execution Loop (RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-story-implement Phase-2-Implement`

Open tracking: `TaskCreate(description: "Phase 2: Task Execution Loop — {M} tasks")` → `phase2TaskId`.

Read tasks from `tasks-story-XXXX-YYYY.md` (Section 8 fallback). For each `TASK-NNN` not in `DONE` / `BLOCKED`:

1. Check deps; if unresolved, mark `BLOCKED` and skip.
2. Open per-task tracking: `TaskCreate(description: "Task TASK-XXXX-YYYY-NNN: {description}")` → `taskLevelTaskId`.
3. Update execution-state: `IN_PROGRESS`; create branch `feat/task-XXXX-YYYY-NNN-desc` from parent (`--auto-approve-pr`) or `develop`.
4. Read per-task plan if PRE_PLANNED.
5. **Run TDD:** `Skill(skill: "x-test-tdd", args: "TASK-XXXX-YYYY-NNN --orchestrated")`. The child runs RED → GREEN → REFACTOR and delegates commits to `x-git-commit`.
6. Push: `git push -u origin feat/task-XXXX-YYYY-NNN-desc`.
7. **CI-watch (step 2.2.8.5, Rule 21):** poll CI + classify Copilot reviews via `Skill(skill: "x-pr-watch-ci", args: "--pr {prNumber}")` unless `--no-ci-watch`.
8. Create PR: `Skill(skill: "x-pr-create", args: "TASK-XXXX-YYYY-NNN")`. Target = parent branch (`--auto-approve-pr`) or `develop`.
9. Update execution-state: `PR_CREATED` + prNumber + prUrl.
10. **Approval gate (EPIC-0042):** default auto-approve & auto-merge (`gh pr merge --squash`) → `PR_MERGED`. `--manual-task-approval` pauses with APPROVE | REJECT | PAUSE menu.
11. Close per-task tracking: `TaskUpdate(id: taskLevelTaskId, status: "completed")`. On FAILED or BLOCKED, prefix description with `(FAILED)` / `(BLOCKED)` first.

Fallback G1-G7 when no test plan / no formal tasks exist: implement layer groups as a single implicit task with WARNING.

Close tracking: `TaskUpdate(id: phase2TaskId, status: "completed")`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-story-implement Phase-2-Implement ok`

## Phase 3 — Story-Level Verification

Open tracking: `TaskCreate(description: "Phase 3: Story-Level Verification — Story {storyId}")` → `phase3TaskId`.

Skipped only under `--skip-verification` (recovery-only — see Rule 22). Initialize progress via `TodoWrite` with 7 items (test-run, coverage, smoke, specialist reviews, remediation, tech-lead, dashboard updates).

| Step | Action |
|------|--------|
| 3.1 | Run `{{TEST_COMMAND}}` + `{{COVERAGE_COMMAND}}`; validate line ≥ 95% / branch ≥ 90%. |
| 3.2 | Cross-file consistency check (error handling / constructor / return-type uniformity per role). |
| 3.3 | Documentation update: interface-dispatch generators (OpenAPI/gRPC/CLI/GraphQL/events), CHANGELOG entry, optional `Skill(skill: "x-arch-update", ...)`. |
| 3.4 | **Specialist reviews:** `Skill(skill: "x-review", args: "{STORY_ID}")` — launches 8 parallel specialists; produces dashboard `dashboard-story-XXXX-YYYY.md`. |
| 3.5 | **Remediation:** map findings → `remediation-story-XXXX-YYYY.md`; fix TDD-discipline; auto-dispatch per-finding remediation agents on CRITICAL/HIGH (unless `--no-auto-remediation`). |
| 3.6 | **Tech Lead review:** `Skill(skill: "x-review-pr", args: "{STORY_ID}")`. On GO, run step 3.6.5 (auto-fix PR comments via `Skill(skill: "x-pr-fix", ...)`). On NO-GO, fix-and-review up to 2 cycles. |
| 3.7 | **Story-level PR (`--auto-approve-pr` only):** push parent branch, `gh pr create --base develop` — human-reviewed, NEVER auto-merged (RULE-004). |
| 3.8 | **Final verification + status-finalize:** DoD checklist, smoke gate (hard — see below), update IMPLEMENTATION-MAP + story `**Status:**` + Jira transition, update execution-state to `COMPLETE`. |
| 3.8b | **Worktree removal gated by `STORY_OWNS_WORKTREE`** — see `references/full-protocol.md` §5 for the full decision table. Mode 1 / Mode 3 skip removal; Mode 2 success removes via `Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")` then re-syncs develop in main checkout; Mode 2 failure preserves for diagnosis (Rule 14 §4). |

**Smoke gate (HARD, EPIC-0042):** when `testing.smoke_tests == true` and `--skip-smoke` is absent, run `/x-test-e2e` or configured smoke; Health Check + Critical Path failures FAIL Phase 3. Response Time + Error Rate are advisory only.

Close tracking: `TaskUpdate(id: phase3TaskId, status: "completed")`.

**Phase 3 is the ONLY legitimate stopping point.**

## Error Envelope

| Scenario | Action |
|----------|--------|
| Story file not found | ABORT: `"Story file not found at {path}"` |
| Dependency story incomplete | ABORT: `"Dependency {storyId} not complete. Complete it first."` |
| Task dependency unresolved | Mark `BLOCKED`; skip in current iteration |
| `x-test-tdd` fails | Mark task `FAILED`; update execution-state; classify error (TRANSIENT / CONTEXT / PERMANENT) and retry or abort |
| Coverage below threshold | Fail Phase 3.1; add tests until thresholds met |
| `x-arch-plan` unavailable | Inline fallback — expand Phase 1B subagent prompt |
| Template file not found (RULE-012) | WARN; use inline format as fallback |
| Review score below approval | Fix all failed items and re-review (max 2 cycles) |
| No test plan (Phase 1B empty) | Phase 2 uses G1-G7 fallback with WARNING |
| PR creation fails | Log error; mark task `FAILED`; update execution-state |
| Corrupted execution-state on resume | Reinitialize from PR statuses via `gh pr view` |
| `x-pr-fix` compile regression | ABORT Step 3.6.5 with `PR_FIX_COMPILE_REGRESSION`; do NOT proceed to 3.7 |
| Context pressure detected | Log warning; set `contextPressureDetected: true` in `SubagentResult`; apply Level 1 actions locally |

**Error classification (transient / context / permanent) + retry-backoff schedule (2s / 4s / 8s, max 3)** and the `SubagentResult` shape for errors reported to `x-epic-implement` live in `references/full-protocol.md` §6.

## Full Protocol

Minimum viable contract above. Complete Phase 0 branching-mode decision table, per-planner subagent prompts for Phases 1A-1F, Phase 1.5 parallelism matrix, expanded Phase 2 approval-gate semantics, Phase 3 per-step detail (specialist prompts, Tech Lead template, DoD checklist, smoke-gate output format, worktree-cleanup decision table), graceful degradation levels, error classification + retry-backoff, template-fallback list, and the **v2 Extensions (EPIC-0038 — Wave-Based Task Orchestration)** appendix (Phase 0f schema detection, Phase 1 (v2) map read, Phase 2 (v2) wave dispatch, Phase 3 (v2) coalesced waves, Phase 4 (v2) story-level aggregation, v2 error codes `MAP_NOT_FOUND` / `WAVE_VERIFICATION_FAILED` / `COALESCED_PARTNER_MISSING` / `CROSS_WAVE_REGRESSION`) all live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012.

## Integration Notes

| Skill | Relationship | Context |
|-------|--------------|---------|
| `x-test-tdd` | invokes (Phase 2, per task) | TDD execution for each task |
| `x-git-commit` | invoked by `x-test-tdd` | Atomic commits with TDD tags + pre-commit chain |
| `x-pr-create` | invokes (Phase 2, per task) | Task-level PR creation |
| `x-code-format` / `x-code-lint` | invoked by `x-git-commit` chain | Pre-commit format + lint |
| `x-task-plan` | invokes (Phase 1 per-task, if not PRE_PLANNED) | Individual task planning |
| `x-arch-plan` / `x-test-plan` / `x-lib-task-decomposer` / `x-threat-model` | invokes (Phase 1A-1F) | Planning artifacts |
| `x-parallel-eval` | invokes (Phase 1.5) | Collision matrix + serial-degradation recommendations |
| `x-pr-watch-ci` | invokes (Phase 2 step 2.2.8.5) | CI polling + Copilot review detection (Rule 21) |
| `x-review` / `x-review-pr` | invokes (Phase 3.4 / 3.6) | Parallel specialist reviews + Tech Lead |
| `x-pr-fix` | invokes (Step 3.6.5 on TL GO) | Auto-fix PR comments after Tech Lead approval |
| `x-arch-update` | invokes (Phase 3.3, conditional) | Architecture document update |
| `x-git-worktree` | invokes (Phase 0 + Phase 3 cleanup) | Worktree context detection + Creator-owned removal (Rule 14 + ADR-0004) |
| `x-epic-implement` | called by | Epic orchestrator delegates story execution |

All `{{PLACEHOLDER}}` tokens (e.g., `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation.
