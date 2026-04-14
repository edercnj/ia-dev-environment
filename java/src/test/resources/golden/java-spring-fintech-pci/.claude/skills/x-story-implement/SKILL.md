---
name: x-story-implement
description: "Orchestrates the complete feature implementation cycle with task-centric workflow: branch creation, planning, per-task TDD execution with individual PRs and approval gates, story-level verification, and final cleanup. Delegates implementation to x-test-tdd, commits to x-git-commit, PRs to x-pr-create."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill, Agent, TaskCreate, TaskUpdate
argument-hint: "[STORY-ID or feature-name] [--auto-approve-pr] [--task TASK-ID] [--skip-verification] [--full-lifecycle] [--worktree]"
context-budget: heavy
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## CONTEXT MANAGEMENT

Do NOT read full files into context when partial data suffices.
Use targeted reads (offset/limit) or grep for specific fields.

- **Task results**: After each task completes via x-test-tdd, record only the compact result (status, commitSha, coverage). Do NOT accumulate full TDD cycle logs in the orchestrator context.
- **Plan files**: When checking artifact staleness (Phase 0), use file metadata (mtime) not full content reads.
- **Review outputs**: Reference review results by file path and score summary. Do NOT read full review content into orchestrator context.
- **Story files**: Read story file once during Phase 0 and extract only required fields (acceptance criteria, dependencies, tasks). Do NOT re-read story files in later phases.

# Skill: Feature Lifecycle (Task-Centric Orchestrator)

## Purpose

Orchestrate the complete feature implementation cycle using a task-centric workflow. Each task in the story produces its own branch, PR, and approval gate. The lifecycle delegates TDD implementation to `x-test-tdd`, commits to `x-git-commit`, and PR creation to `x-pr-create`. Phase 2 is a Task Execution Loop; Phase 3 is Story-Level Verification that absorbs the old review/fix/PR/verification phases.

## When to Use

- Full story/feature implementation with review cycle
- End-to-end development workflow (plan -> code -> review -> PR)
- Task-centric delivery with per-task PRs and approval gates

## CLI Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--auto-approve-pr` | Boolean | false | Activate auto-approve mode with parent branch (RULE-004) |
| `--task` | String | -- | Execute only a specific task (TASK-XXXX-YYYY-NNN) |
| `--skip-verification` | Boolean | false | Skip Phase 3 (story-level verification) |
| `--full-lifecycle` | Boolean | false | Force full execution regardless of scope tier |
| `--worktree` | Boolean | false | Opt-in: create a dedicated worktree for the story branch (standalone mode). Ignored when the skill is already running inside a worktree (Rule 14 §3 — non-nesting invariant). See ADR-0004 §D2. |

## CRITICAL EXECUTION RULE

**3 phases (0-2) + optional Phase 3. ALL phases through Phase 2 mandatory. NEVER stop before Phase 2 completes.**

After Phase 0: `>>> Phase 0 completed. Proceeding to Phase 1...`
After Phase 1: `>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...`
After Phase 2: `>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verification)...`
After Phase 3: `>>> Phase 3 completed. Lifecycle complete.`

## Workflow Overview

```
Phase 0: Preparation             (orchestrator -- inline, includes artifact pre-checks and resume)
{% if has_contract_interfaces == 'True' %}Phase 0.5: API-First Contract     (orchestrator -- conditional, pauses for approval)
{% endif %}Phase 1: Planning                 (subagent -- reads architecture KPs, produces plans)
Phase 1B-1F: Parallel Planning    (up to 5 subagents -- SINGLE message, skips reusable artifacts)
Phase 2: Task Execution Loop      (for each task: branch -> x-test-tdd -> x-pr-create -> approval gate)
Phase 3: Story-Level Verification (coverage, cross-file consistency, smoke test, final report)
```

### RULE-001: Task as Unit of Delivery

Each task = 1 branch = 1 PR. Tasks are the atomic unit of delivery. The lifecycle orchestrates, individual skills implement:

| Concern | Skill | Invoked By |
|---------|-------|------------|
| TDD implementation | `x-test-tdd` | Phase 2 (per task) |
| Atomic commits | `x-git-commit` | `x-test-tdd` (per TDD cycle) |
| PR creation | `x-pr-create` | Phase 2 (per task) |
| Code formatting | `x-code-format` | `x-git-commit` (pre-commit chain) |
| Code linting | `x-code-lint` | `x-git-commit` (pre-commit chain) |
| Task planning | `x-task-plan` | Phase 1 (if not PRE_PLANNED) |

## Context Budget Decision Logic

Before invoking any skill inline via the `Skill` tool, evaluate the accumulated context budget:

1. Check the `context-budget` field in the target skill's frontmatter (light/medium/heavy)
2. Track the accumulated budget of all skills loaded inline in the current conversation
3. Apply the delegation rule:
   - If the accumulated budget is `heavy` and the next skill is `medium` or `heavy`: delegate via `Agent` tool (subagent) instead of `Skill` tool (inline)
   - If the accumulated budget is `medium` and the next skill is `heavy`: delegate via `Agent` tool
   - Otherwise: invoke inline via `Skill` tool
4. When delegating due to budget: log `"Context budget exceeded ({accumulated}). Delegating {skill-name} to subagent."`

> **Note:** The `context-budget` field is informational only — it does not affect how Claude Code loads the skill. The delegation decision is made by this orchestrator.
---

## Phase 0 -- Preparation (Orchestrator -- Inline)

**First action of Phase 0 — Create a tracking task (TaskCreate):**

    TaskCreate(description: "Phase 0: Preparation — Story {storyId}")

Record the returned task ID as `phase0TaskId` for the closing TaskUpdate call. This gives the user real-time visibility into Phase 0 progress in the Claude Code task list (Story 0033-0002, Level 2 visibility).

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. **Artifact Pre-checks (RULE-002 -- Idempotency via Staleness Check):**
   For each artifact type listed below, check existence and staleness. If `mtime(story) <= mtime(plan)`, the plan is fresh -- reuse it. If `mtime(story) > mtime(plan)`, the plan is stale -- mark for regeneration. If the plan does not exist, mark for generation.

   | # | Artifact Type | File Pattern | Phase that Generates | Template Reference |
   |---|---------------|--------------|----------------------|--------------------|
   | 1 | Test Plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | 1B (parallel) | `_TEMPLATE-TEST-PLAN.md` |
   | 2 | Architecture Plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | 1A | `_TEMPLATE-ARCHITECTURE-PLAN.md` |
   | 3 | Implementation Plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | 1 (Step 1B) | `_TEMPLATE-IMPLEMENTATION-PLAN.md` |
   | 4 | Task Breakdown | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | 1C (parallel) | `_TEMPLATE-TASK-BREAKDOWN.md` |
   | 5 | Security Assessment | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | 1E | `_TEMPLATE-SECURITY-ASSESSMENT.md` |
   | 6 | Compliance Assessment | `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | 1F | `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` |

   **Staleness Check Logic:**

   | Condition | Action | Log Message |
   |-----------|--------|-------------|
   | Plan does not exist | Generate new | `"Generating {type} for {story}"` |
   | `mtime(story) > mtime(plan)` | Regenerate (stale) | `"Regenerating stale {type} for {story}"` |
   | `mtime(story) <= mtime(plan)` | Reuse existing | `"Reusing existing {type} from {date}"` |
   | `mtime(story) == mtime(plan)` | Reuse existing | `"Reusing existing {type} from {date}"` |

   For artifacts marked as "Reuse", the corresponding generation phase is skipped. Reused artifacts are read by subsequent phases as if freshly generated.

4. Extract epic ID from story ID (e.g., `story-0001-0003` -> epic ID `0001`)
5. Ensure directories exist: `mkdir -p plans/epic-XXXX/plans plans/epic-XXXX/reviews`
6. **Worktree-First Branch Creation (Rule 14 + ADR-0004):**

   Branch creation in `x-story-implement` follows the **worktree-first policy** defined in [ADR-0004 — Worktree-First Branch Creation Policy](../../../adr/ADR-0004-worktree-first-branch-creation-policy.md) and the normative invariants of [Rule 14 — Worktree Lifecycle](../../rules/14-worktree-lifecycle.md). The routine below is **mandatory** and MUST execute before any `git checkout -b` or `/x-git-worktree create` call.

   **Step 6a — Detect worktree context (Rule 14 §3 — Non-Nesting Invariant).** Invoke the `x-git-worktree` skill via the Skill tool to classify the current execution context (Rule 13 Pattern 1 — INLINE-SKILL):

       Skill(skill: "x-git-worktree", args: "detect-context")

   The skill returns a JSON envelope of the form:

   ```json
   {
     "inWorktree": true,
     "worktreePath": "/abs/path/to/.claude/worktrees/story-XXXX-YYYY",
     "mainRepoPath": "/abs/path/to/repo"
   }
   ```

   Record `inWorktree`, `worktreePath`, and `mainRepoPath` for use in the following steps. This call is **mandatory** before any branch creation — skipping it risks creating a nested worktree (Rule 14 §3) or causing the harness to apply file operations in the wrong checkout, which can wipe sibling files outside the intended worktree (see Rule 14 §7 — Anti-Patterns and ADR-0004 Context §3 — Lack of operator visibility).

   **Step 6b — Select the branching mode.** Apply the three-way decision table below. This table is the single source of truth for branch creation in this skill:

   | # | Condition | Mode | Action |
   | :--- | :--- | :--- | :--- |
   | 1 | `inWorktree == true` | **REUSE (orchestrated)** | Reuse the current worktree. Do NOT invoke `/x-git-worktree create`. Do NOT create a nested worktree (Rule 14 §3). Branch creation inside the reused worktree follows the `--auto-approve-pr` legacy behavior below (via `git checkout -b`). The creator of the outer worktree owns its removal (Rule 14 §5). |
   | 2 | `inWorktree == false` AND `--worktree` flag present | **CREATE (standalone opt-in)** | Provision a dedicated worktree for the story branch by invoking the `x-git-worktree` skill (see Step 6c, Mode 2, for the canonical `Skill(...)` call). This is the opt-in path documented in ADR-0004 §D2 for operators who want isolation when running `x-story-implement` standalone. `x-story-implement` is the creator and owns removal (Rule 14 §5 — end of Phase 3 on success; preserved on failure per Rule 14 §4). |
   | 3 | `inWorktree == false` AND `--worktree` flag absent | **LEGACY (main checkout)** | Fall back to the legacy / interactive behavior: branches are created directly in the main working tree via `git checkout -b`. This preserves backward compatibility for developers who run `/x-story-implement` interactively without requesting isolation. |

   > **Orchestrator auto-path.** When this skill is dispatched by `x-epic-implement` (via `Skill(skill: "x-story-implement", ...)` or `Agent(...)`), the parent orchestrator creates the worktree **before** dispatching, and this invocation detects `inWorktree == true` and selects Mode 1 (REUSE) automatically. No flag is required from the caller. This is the expected automatic path described in ADR-0004 §D2.

   > **Anti-pattern (DO NOT USE):** `Agent(isolation:"worktree")` is DEPRECATED (see ADR-0004 and Rule 14 §7). The harness-native isolation is replaced by explicit `/x-git-worktree create` calls from orchestrators so the worktree lifecycle is visible in logs and recoverable on failure.

   **Step 6c — Execute the selected branching mode.**

   - **Mode 1 (REUSE — orchestrated).** The parent orchestrator has already placed the process inside the worktree at `worktreePath`. Proceed with branch creation *inside* that worktree:
     - If `--auto-approve-pr`: create parent branch `feat/story-XXXX-YYYY-desc` from `develop` via `git checkout -b`. All task branches are created from and target this parent branch.
     - If NOT `--auto-approve-pr`: no parent branch is created at this stage. Task branches are created later (Phase 2) and target `develop` directly.
     - Do NOT call `/x-git-worktree remove` here or at end of Phase 3. The orchestrator is the creator and owns removal (Rule 14 §5).

   - **Mode 2 (CREATE — standalone opt-in).** Invoke `x-git-worktree` via the Skill tool to provision the worktree (Rule 13 Pattern 1 — INLINE-SKILL):

         Skill(skill: "x-git-worktree", args: "create --branch feat/story-XXXX-YYYY-desc --base develop --id story-XXXX-YYYY")

     The operation creates `.claude/worktrees/story-XXXX-YYYY/` with `feat/story-XXXX-YYYY-desc` checked out. In Mode 2, that branch always exists because it anchors the standalone worktree. Record the returned worktree path; subsequent phases execute with that path as their working directory.
     - If `--auto-approve-pr`: the branch created by `/x-git-worktree create` IS the parent branch. All task branches are created from and target `feat/story-XXXX-YYYY-desc`.
     - If NOT `--auto-approve-pr`: the branch created by `/x-git-worktree create` is an isolation branch only; it is **not** treated as the parent branch for task PR flow. No additional parent branch is created. Task branches are created later in Phase 2 from `develop` and target `develop` directly, even though execution remains inside the standalone worktree.
     - In standalone mode `x-story-implement` is the creator and MUST invoke `/x-git-worktree remove --id story-XXXX-YYYY` at the end of Phase 3 on success. On failure, the worktree is preserved for diagnosis (Rule 14 §4).

   - **Mode 3 (LEGACY — main checkout).** Execute the pre-EPIC-0037 behavior unchanged, operating directly in the main working tree:
     - If `--auto-approve-pr`: `git checkout -b feat/story-XXXX-YYYY-desc develop` creates the parent branch. All task branches will be created from and target this parent branch.
     - If NOT `--auto-approve-pr`: no parent branch is created at this stage. Task branches (Phase 2) target `develop` directly.

   **Step 6d — Logging.** After mode selection, log one of:

   - `"Branch creation mode: REUSE (inside worktree {worktreePath})"`
   - `"Branch creation mode: CREATE (standalone --worktree, provisioning worktree for story-XXXX-YYYY)"`
   - `"Branch creation mode: LEGACY (main checkout, no --worktree flag)"`

   These lines are required for operator diagnostics — Rule 14 §3 detection SHOULD be auditable from the log stream alone.

7. **Scope Assessment** -- classify the story to determine lifecycle phase optimization:

### Scope Assessment

Analyze the story content to classify its scope tier. The classification determines which phases execute:

**Classification Criteria:**

| Criterion | How to detect |
|-----------|--------------|
| Components affected | Count distinct `.java`/`.kt`/`.py`/`.ts`/`.go`/`.rs` file mentions in tech description |
| New endpoints | Count `POST/GET/PUT/DELETE/PATCH /path` patterns in data contracts |
| Schema changes | Presence of "migration script", "ALTER TABLE", "CREATE TABLE", "DROP TABLE", "ADD COLUMN" |
| Compliance | `compliance:` field with value other than "none" |
| Dependents | Count stories that depend on this one (from IMPLEMENTATION-MAP) |

**Tier Classification:**

| Tier | Criteria | Phase Behavior |
|------|----------|---------------|
| SIMPLE | <=1 component, 0 endpoints, 0 schema changes, no compliance | Skip phases 1B, 1C, 1D, 1E |
| STANDARD | 2-3 components OR 1-2 new endpoints | All phases execute normally |
| COMPLEX | >=4 components OR schema changes OR compliance requirement | All phases + stakeholder review after Phase 2 |

**Elevation Rules:**
- Compliance **always** elevates to COMPLEX regardless of other criteria
- Schema changes **always** elevate to at least COMPLEX
- A single COMPLEX criterion is sufficient for COMPLEX classification

**Display the assessment before proceeding:**

```
Scope Assessment: [TIER]
> [Phases that will execute]
> Rationale: [justification]
> [Override instruction if SIMPLE]
```

**`--full-lifecycle` Flag:**
If the user passes `--full-lifecycle`, force full execution regardless of tier:
- All phases execute (equivalent to STANDARD)
- Display: "Scope override: running full lifecycle as requested"

**SIMPLE Execution Flow:**
Phases 0 (Prepare) > 1A (Plan) > 2 (Task Execution Loop) > 3 (Verify) -- skips 1B, 1C, 1D, 1E

**COMPLEX Execution Flow:**
All phases execute normally. After Phase 2, **pause** with:
"Scope COMPLEX -- stakeholder review required. Review all task PRs and confirm to proceed with story-level verification."
Wait for developer confirmation before executing Phase 3.

**Default Behavior:**
If scope assessment cannot be performed (e.g., story content unavailable), default to STANDARD (all phases execute). No error is raised.

### Phase 0.4 -- Planning Mode Detection

Before executing Phase 1, detect the planning mode by checking for pre-existing planning artifacts:

**Artifacts to check (7 types):**

| # | Type | Pattern | Generated By |
|---|------|---------|-------------|
| 1 | Architecture Plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | x-story-plan Phase 2A |
| 2 | Test Plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | x-story-plan Phase 2B |
| 3 | Implementation Plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | x-story-plan Phase 2A |
| 4 | Task Breakdown | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | x-story-plan Phase 4 |
| 5 | Security Assessment | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | x-story-plan Phase 2C |
| 6 | Compliance Assessment | `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | x-story-plan Phase 2C |
| 7 | Per-Task Plans | `plans/epic-XXXX/plans/task-plan-TASK-*-story-XXXX-YYYY.md` | x-story-plan Phase 4 |

**Mode determination:**

| Mode | Condition | Behavior |
|------|-----------|----------|
| PRE_PLANNED | All 7 types exist AND `mtime(story) <= mtime(artifact)` for all | Skip Phase 1A-1F entirely. Phase 2 uses task plans directly. |
| HYBRID | Some artifacts exist but not all | Generate ONLY the missing artifacts in Phase 1. Reuse existing fresh ones. |
| INLINE | No artifacts exist | Execute Phase 1A-1F as currently defined (backward compatible). |

Log the detected mode: `"Planning mode: {MODE}"`
When HYBRID, also log: `"Planning mode: HYBRID -- generating {N} missing artifact(s): {list}"`

### Phase 0.5 -- Resume Detection (RULE-014)

Check for existing `execution-state.json` in the story's plan directory:

1. If `execution-state.json` exists, read task statuses
2. For each task, reclassify status by checking actual PR state:

   | Status Anterior | Condition | Novo Status |
   |-----------------|-----------|-------------|
   | IN_PROGRESS | No PR found (`gh pr view` fails) | PENDING |
   | IN_PROGRESS | PR open | PR_CREATED |
   | PR_CREATED | PR approved | PR_APPROVED |
   | PR_CREATED | PR merged | DONE |
   | PR_CREATED | PR closed | FAILED |
   | BLOCKED | Dependencies now resolved | PENDING |
   | BLOCKED | Dependencies still blocked | BLOCKED (keep) |
   | DONE | -- | DONE (skip) |

3. Log reclassification: `"Resume: reclassified TASK-NNN from {old} to {new}"`
4. Tasks with status DONE are skipped in Phase 2
5. Tasks with status FAILED are reclassified to PENDING for retry

If no `execution-state.json` exists, initialize with all tasks as PENDING.

{% if has_contract_interfaces == 'True' %}

## Phase 0.5 -- API-First Contract Generation (Orchestrator -- Conditional)

> **RULE-005:** Formal contract before implementation. This phase generates and validates
> API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) before any implementation code.

**Activation:** This phase is ONLY executed when the story declares interfaces of type
`rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`. Stories without
these interface types skip directly to Phase 1.

### Step 0.5.1 -- Interface Detection

Read the story file and identify declared interface types:

| Interface Type | Contract Format | Output Path |
|---------------|----------------|-------------|
| `rest` | OpenAPI 3.1 | `contracts/{STORY_ID}-openapi.yaml` |
| `grpc` | Protobuf 3 | `contracts/{STORY_ID}.proto` |
| `event-consumer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `event-producer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `websocket` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |

### Step 0.5.2 -- Contract Generation

Generate a draft contract in the appropriate format using data contracts from the story:

- **REST (OpenAPI 3.1):** Extract endpoints, DTOs, status codes from story data contracts.
  Generate `openapi: "3.1.0"` spec with `info`, `paths`, `components/schemas`, RFC 7807 errors.
- **gRPC (Protobuf 3):** Extract service definitions, request/response messages.
  Generate `.proto` file with `syntax = "proto3"`, service, and message definitions.
- **Event (AsyncAPI 2.6):** Extract event names, channels, payload schemas.
  Generate `asyncapi: "2.6.0"` spec with `channels`, `components/messages`, `components/schemas`.

Ensure directory exists: `mkdir -p contracts/`

### Step 0.5.3 -- Contract Validation

Perform inline contract validation using the appropriate linter for `{CONTRACT_FORMAT}`:

- **OpenAPI 3.1:** `npx @redocly/cli lint {CONTRACT_PATH}` or `spectral lint {CONTRACT_PATH}`
- **Protobuf 3:** `protoc --lint_out=. {CONTRACT_PATH}` or `buf lint {CONTRACT_PATH}`
- **AsyncAPI 2.6:** `npx @asyncapi/cli validate {CONTRACT_PATH}` or `spectral lint {CONTRACT_PATH}`

If validation errors are found:
1. Fix the errors in the generated contract
2. Re-run validation until the contract passes

> **Note:** A dedicated `x-test-contract-lint` skill does not exist in `core/` at the time of writing (the reference was an orphan removed in EPIC-0033 / STORY-0033-0001). If `x-test-contract-lint` is added in the future, convert this step to `Skill(skill: "x-test-contract-lint", args: "{CONTRACT_PATH}")` following Rule 13 — Skill Invocation Protocol (INLINE-SKILL pattern).

### Step 0.5.4 -- Approval Gate

Emit the following message and **pause the lifecycle**:

```
CONTRACT PENDING APPROVAL

Contract generated: {CONTRACT_PATH}
Format: {CONTRACT_FORMAT}
Status: PENDING_APPROVAL

Please review the contract and respond with:
  - APPROVE: Proceed to Phase 1 (Architecture Planning)
  - REJECT: Return to Step 0.5.2 for regeneration with feedback
```

- **On APPROVE:** Set `contractStatus = APPROVED` and proceed to Phase 1.
- **On REJECT:** Return to Step 0.5.2, incorporating user feedback into regeneration.
{% endif %}

**Last action of Phase 0 — Update the phase task (TaskUpdate):**

    TaskUpdate(id: phase0TaskId, status: "completed")

Here `phase0TaskId` refers to the numeric task ID returned by the earlier `TaskCreate` call at the start of Phase 0 (substitute with the actual integer, do NOT pass the literal string `"phase0TaskId"`).

## Phase 1 -- Architecture Planning (Skill Invocation + Subagent Fallback)

**First action of Phase 1 — Create a tracking task (TaskCreate):**

    TaskCreate(description: "Phase 1: Planning — Story {storyId}")

Record the returned task ID as `phase1TaskId` for the closing TaskUpdate call. Planning subagents launched inside Phase 1 (architecture plan, test plan, task decomposer, etc.) will receive their own task entries in a later story (0033-0003).

**If the architecture plan file already exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` (as checked in Phase 0), skip Step 1A and proceed directly to Step 1B, ensuring Step 1B reads the existing plan.**

### Step 1A: Architecture Plan via x-arch-plan

Evaluate change scope using the decision tree:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

**If Full or Simplified:**

**Orchestrator tracking (Story 0033-0003, planning subagent visibility):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Architecture Plan — Story {storyId}")

Record the returned integer task ID as `archPlanTaskId` for the closing TaskUpdate.

Invoke `x-arch-plan` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-arch-plan", args: "{STORY_PATH}")

After the skill returns (success, failure, or WARNING fallback), close the tracking task:

    TaskUpdate(id: archPlanTaskId, status: "completed")

- Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
- If the skill invocation fails: emit `WARNING: Architecture plan generation failed. Continuing without architecture plan.`, still close the tracking task via `TaskUpdate(id: archPlanTaskId, status: "completed")`, and proceed to Step 1B.

**If Skip:**

Log `"Architecture plan not needed for this change scope"` and proceed to Step 1B.

### Step 1B: Implementation Plan (Subagent via Task)

**Skip condition:** If Phase 0 pre-check marked the implementation plan as "Reuse", skip this step entirely and log `"Reusing existing implementation plan from {date}"` (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

Launch a single `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern). The subagent itself emits the TaskCreate/TaskUpdate for its own tracking task (per Story 0033-0003) and uses `model: opus` per RULE-009 (senior architect tier).

**Canonical Agent invocation:**

    Agent(
      subagent_type: "general-purpose",
      description: "Plan implementation for story {storyId}",
      prompt: "<prompt content from the Senior Architect quote block below, with {{PROJECT_NAME}} and {storyId} substituted>"
    )

**Prompt content to pass as the `prompt` argument above** (runtime LLM assembles the literal string from the quoted block):

> **FIRST ACTION (Story 0033-0003):** Create a tracking task to report progress to the parent orchestrator's task list:
>
>     TaskCreate(description: "Planning: Implementation Plan — Story {storyId}")
>
> Record the returned integer ID as `implPlanTaskId` for the LAST ACTION below.
>
> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
>
> CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
>
> **Step 1 -- Read context:**
> - Read template at `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` for required output format (RULE-007). If the template file does not exist, log `"WARNING: Template _TEMPLATE-IMPLEMENTATION-PLAN.md not found, using inline format"` and use the inline section list below as fallback (RULE-012).
> - Read story file: `{STORY_PATH}`
> - Read `skills/architecture/references/architecture-principles.md` -- layer structure, dependency direction
> - Read `skills/layer-templates/SKILL.md` -- code templates per architecture layer
> - Read any relevant ADRs in `adr/`
> - If architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`, read it for architectural decisions and constraints
>
> **Step 2 -- Produce implementation plan** following the template structure. If template was unavailable, use these sections as fallback:
> 1. Affected layers and components
> 2. New classes/interfaces to create (with package locations)
> 3. Existing classes to modify
> 4. Class diagram (Mermaid classDiagram)
> 5. Method signatures per new class
> 6. Dependency direction validation
> 7. Integration points
> 8. Database changes (if applicable)
> 9. API changes (if applicable)
> 10. Event changes (if applicable)
> 11. Configuration changes
> 12. TDD strategy -- map classes to test plan scenarios (UT-N, AT-N, IT-N references)
> 13. Architecture decisions -- mini-ADRs (Context, Decision, Rationale, Consequences)
> 14. Risk assessment
>
> Save to `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (where XXXX is the epic ID and YYYY is the story sequence, extracted from the story ID).
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task created in the FIRST ACTION:
>
>     TaskUpdate(id: implPlanTaskId, status: "completed")

### Fallback: Inline Architecture Planning

If skill `x-arch-plan` is not available in the project (skill file `skills/x-arch-plan/SKILL.md` does not exist), combine architecture planning into the Step 1B subagent by expanding its prompt to also read:
- `skills/protocols/references/` -- protocol conventions
- `skills/security/references/` -- OWASP, headers, secrets
- `skills/observability/references/` -- tracing, metrics, logging
- `skills/resilience/references/` -- circuit breaker, retry, fallback

This preserves the pre-integration behavior for projects that do not include the architecture-plan skill.

## Phases 1B-1F -- Parallel Planning (Subagents via Task -- SINGLE message)

**CRITICAL: ALL planning subagents (except those skipped by pre-checks) MUST be launched in a SINGLE message.**

**Parallelism + tracking batching (Story 0033-0003):** The per-planner instructions below LOOK sequential (TaskCreate → Skill/Agent → TaskUpdate), but to preserve SINGLE-message parallelism AND per-planner tracking, execute them in this 3-step batched pattern:

1. **Batch A — First assistant message (all TaskCreate + all invocations together):**
   - Emit every active planner's `TaskCreate(description: "Planning: {artifact} — Story {storyId}")` call
   - Emit every active planner's `Skill(skill: "...", args: "...")` or `Agent(subagent_type: "general-purpose", ...)` launch
   - Record the returned task IDs in an in-memory map indexed by planner name (e.g., `planningTasks[\"testPlan\"] = <id>`)
   - All of these tool calls MUST be siblings in the SAME assistant message so the runtime dispatches them in parallel. Do NOT emit them across separate messages — that serializes execution and defeats the Phase 1B-1F parallelism.

2. **Wait for all planners to return** (the runtime handles this — subsequent assistant messages only start after all parallel tool calls in Batch A complete).

3. **Batch B — Second assistant message (all TaskUpdate together):**
   - For each planner that was launched in Batch A, emit `TaskUpdate(id: planningTasks[\"...\"], status: "completed")`
   - Again as siblings in ONE message so they commit in parallel
   - Subagent-managed planners (1B Impl Plan, 1D Event Schema, 1E fallback, 1F Compliance) close their OWN tracking tasks from inside their prompts — the orchestrator does NOT emit TaskUpdate for those in Batch B (it would double-close)

**Summary of who emits what:**

| Planner | Strategy | Batch A | Batch B |
|---|---|---|---|
| 1B Implementation Plan | Subagent | Orchestrator launches Agent (subagent self-tracks via FIRST/LAST ACTION) | — |
| 1B Test Plan | Skill-invoked | Orchestrator emits TaskCreate + Skill(x-test-plan) | Orchestrator emits TaskUpdate |
| 1C Task Decomposition | Skill-invoked | Orchestrator emits TaskCreate + Skill(x-lib-task-decomposer) | Orchestrator emits TaskUpdate |
| 1D Event Schema | Subagent | Orchestrator launches Agent (subagent self-tracks) | — |
| 1E Security (primary) | Skill-invoked | Orchestrator emits TaskCreate + Skill(x-threat-model) | Orchestrator emits TaskUpdate (success) OR emits TaskUpdate pre-fallback then launches fallback subagent |
| 1F Compliance | Subagent | Orchestrator launches Agent (subagent self-tracks) | — |

The per-planner sections below describe the per-planner details — read them as "what goes into Batch A / Batch B for this planner", NOT as "execute sequentially one planner at a time".

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)

**Skip condition:** If Phase 0 pre-check marked the test plan as "Reuse", skip this step entirely and log `"Reusing existing test plan from {date}"` (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Orchestrator tracking (Story 0033-0003):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Test Plan — Story {storyId}")

Record the returned integer task ID as `testPlanTaskId` for the closing TaskUpdate.

Invoke `x-test-plan` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-test-plan", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: testPlanTaskId, status: "completed")

Produces `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.

The test plan is the **implementation roadmap** for Phase 2. It produces:
- Acceptance tests (AT-N) as outer loop (Double-Loop TDD)
- Unit tests (UT-N) in TPP order as inner loop (Levels 1-6: degenerate -> edge cases)
- Integration tests (IT-N) positioned after related UTs
- `Depends On: TASK-N` and `Parallel` markers per scenario

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition

**Skip condition:** If Phase 0 pre-check marked the task breakdown as "Reuse", skip this step entirely and log `"Reusing existing task breakdown from {date}"` (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Orchestrator tracking (Story 0033-0003):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Task Decomposition — Story {storyId}")

Record the returned integer task ID as `taskDecompTaskId` for the closing TaskUpdate.

Invoke `x-lib-task-decomposer` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-lib-task-decomposer", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: taskDecompTaskId, status: "completed")

Produces `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`.

The task decomposer auto-detects decomposition mode:
- If test plan with TPP markers exists -> test-driven tasks (RED/GREEN/REFACTOR per task, with `Parallel` flags)
- If no test plan -> fallback to G1-G7 layer-based decomposition

### 1D: Event Schema Design (if event_driven)

Launch a `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern). The subagent emits its own TaskCreate/TaskUpdate (per Story 0033-0003).

**Canonical Agent invocation:**

    Agent(
      subagent_type: "general-purpose",
      description: "Design event schemas for story {storyId}",
      prompt: "<prompt content from the Event Engineer quote block below>"
    )

**Prompt content to pass as the `prompt` argument above:**

> **FIRST ACTION (Story 0033-0003):** Create a tracking task:
>
>     TaskCreate(description: "Planning: Event Schema — Story {storyId}")
>
> Record the returned integer ID as `eventSchemaTaskId` for the LAST ACTION below.
>
> You are an **Event Engineer** designing event schemas.
>
> CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
> Read `skills/protocols/references/event-driven-conventions.md` for standards.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce event schema design: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task:
>
>     TaskUpdate(id: eventSchemaTaskId, status: "completed")

### 1E: Security Assessment (MANDATORY)

**Skip condition:** If Phase 0 pre-check marked the security assessment as "Reuse", skip this step entirely and log `"Reusing existing security assessment from {date}"` (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Primary path — `x-threat-model` via Skill tool (orchestrator-managed tracking):**

Create the tracking task and invoke the skill:

    TaskCreate(description: "Planning: Security Assessment — Story {storyId}")

Record the returned integer task ID as `securityTaskId` for the closing TaskUpdate.

Invoke `x-threat-model` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-threat-model", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: securityTaskId, status: "completed")

Output: `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`

**Fallback path — if `x-threat-model` is unavailable** (skill file not found in `core/`):

The orchestrator's TaskCreate above already fired, so the orchestrator MUST close `securityTaskId` explicitly before launching the fallback subagent (otherwise the tracking task stays open forever):

    TaskUpdate(id: securityTaskId, status: "completed")

Then launch a `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern). The fallback subagent emits its OWN independent TaskCreate/TaskUpdate pair (per Story 0033-0003).

**Canonical Agent invocation:**

    Agent(
      subagent_type: "general-purpose",
      description: "Security assessment (fallback) for story {storyId}",
      prompt: "<prompt content from the Security Engineer quote block below>"
    )

**Prompt content to pass as the `prompt` argument above:**

> **FIRST ACTION (Story 0033-0003 fallback):** Create a tracking task:
>
>     TaskCreate(description: "Planning: Security Assessment (fallback) — Story {storyId}")
>
> Record the returned integer ID as `securityFallbackTaskId` for the LAST ACTION below.
>
> You are a **Security Engineer** assessing security impact.
>
> CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
> Read template at `.claude/templates/_TEMPLATE-SECURITY-ASSESSMENT.md` for required output format (RULE-007). If the template file does not exist, log `"WARNING: Template _TEMPLATE-SECURITY-ASSESSMENT.md not found, using inline format"` and use inline format as fallback (RULE-012).
> Read `skills/security/references/application-security.md` -- OWASP Top 10, security headers, dependency security
> Read `skills/security/references/security-principles.md` -- data classification, input validation, secure error handling
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce security assessment: threat model, OWASP Top 10 mapping, authentication/authorization review, input validation, data protection, secrets management.
> Save to `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003 fallback):** Close the fallback tracking task:
>
>     TaskUpdate(id: securityFallbackTaskId, status: "completed")

### 1F: Compliance Assessment (CONDITIONAL -- if compliance active)

**Activation:** This phase executes ONLY when the project has compliance enabled in setup config (compliance field is not "none"). If compliance is not active, skip entirely with log `"Compliance assessment skipped (compliance not active)"` (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Skip condition:** If Phase 0 pre-check marked the compliance assessment as "Reuse", skip this step entirely and log `"Reusing existing compliance assessment from {date}"` (same rule).

Launch a `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern). The subagent emits its own TaskCreate/TaskUpdate (per Story 0033-0003).

**Canonical Agent invocation:**

    Agent(
      subagent_type: "general-purpose",
      description: "Compliance assessment for story {storyId}",
      prompt: "<prompt content from the Security Engineer quote block below>"
    )

**Prompt content to pass as the `prompt` argument above:**

> **FIRST ACTION (Story 0033-0003):** Create a tracking task:
>
>     TaskCreate(description: "Planning: Compliance Assessment — Story {storyId}")
>
> Record the returned integer ID as `complianceTaskId` for the LAST ACTION below.
>
> You are a **Security Engineer** assessing compliance impact.
>
> CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
> Do NOT expect source code, diffs, or knowledge pack content in this prompt.
> Read template at `.claude/templates/_TEMPLATE-COMPLIANCE-ASSESSMENT.md` for required output format (RULE-007). If the template file does not exist, log `"WARNING: Template _TEMPLATE-COMPLIANCE-ASSESSMENT.md not found, using inline format"` and use inline format as fallback (RULE-012).
> Read the project's active compliance reference under `skills/compliance/references/` (e.g., `gdpr.md`, `lgpd.md`, `pci-dss.md`, `hipaa.md`, or `sox.md` -- read only the one matching the project's compliance configuration).
> Read `skills/security/references/security-principles.md` -- data classification, sensitive data handling
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations.
> Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task:
>
>     TaskUpdate(id: complianceTaskId, status: "completed")

---

**Last action of Phase 1 — Update the phase task (TaskUpdate):**

    TaskUpdate(id: phase1TaskId, status: "completed")

Here `phase1TaskId` refers to the numeric task ID returned by the earlier `TaskCreate` call at the start of Phase 1 (substitute with the actual integer).

## Phase 2 -- Task Execution Loop (RULE-001: 1 Task = 1 Branch = 1 PR)

**First action of Phase 2 — Create a tracking task (TaskCreate):**

    TaskCreate(description: "Phase 2: Task Execution Loop — {M} tasks")

Record the returned task ID as `phase2TaskId` for the closing TaskUpdate call. `{M}` is the count of tasks parsed from Step 2.0. Individual task executions within the loop receive their own per-task entries (see Step 2.2).

Phase 2 replaces the monolithic TDD implementation phase with a per-task execution loop. Each task is implemented independently with its own branch, TDD cycles, and PR.

### Step 2.0 -- Read Task List

1. Read tasks from story Section 8 (Sub-tarefas) or from the task breakdown at `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
2. Parse tasks into ordered list respecting dependency declarations
3. If `--task TASK-XXXX-YYYY-NNN` is specified, filter to only that task
4. **Backward compatibility:** If the story has NO formal tasks in Section 8 (no TASK-XXXX-YYYY-NNN entries), treat the entire story as a single implicit task: `TASK-{EPIC_ID}-{STORY_SEQ}-001` with the story title as description

### Step 2.1 -- Initialize Execution State

Create or update `plans/epic-XXXX/execution-state.json` with task-level tracking:

```json
{
  "storyId": "story-XXXX-YYYY",
  "mode": "MANUAL|AUTO_APPROVE",
  "parentBranch": "feat/story-XXXX-YYYY-desc|null",
  "tasks": {
    "TASK-XXXX-YYYY-001": {
      "status": "PENDING|IN_PROGRESS|PR_CREATED|PR_APPROVED|PR_MERGED|DONE|FAILED|BLOCKED",
      "branch": "feat/task-XXXX-YYYY-001-desc",
      "prNumber": null,
      "prUrl": null,
      "startedAt": null,
      "completedAt": null
    }
  }
}
```

### Step 2.2 -- Task Execution Loop

For each task in dependency order (tasks with unresolved dependencies are BLOCKED):

```
FOR each TASK-NNN where status != DONE and status != BLOCKED:
  2.2.1  Check dependencies: if any dependency task is not DONE -> mark BLOCKED, skip
  2.2.1a Create a per-task tracking entry via TaskCreate (Story 0033-0002, Level 2 visibility):
             TaskCreate(description: "Task TASK-XXXX-YYYY-NNN: {description}")
         Record the returned ID as `taskLevelTaskId` for the closing TaskUpdate in step 2.2.11.
  2.2.2  Update execution-state: task.status = IN_PROGRESS
  2.2.3  Create task branch:
         - If --auto-approve-pr: branch from parent branch (feat/story-XXXX-YYYY-desc)
         - If NOT --auto-approve-pr: branch from develop
         - Branch name: feat/task-XXXX-YYYY-NNN-desc
  2.2.4  Read task plan (if PRE_PLANNED mode):
         - Read plans/epic-XXXX/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md
         - Use Implementation Guide and TDD cycles from the plan
  2.2.5  Invoke `x-test-tdd` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

             Skill(skill: "x-test-tdd", args: "TASK-XXXX-YYYY-NNN --orchestrated")

         - The `--orchestrated` flag (see STORY-0033-0003) signals x-test-tdd to render in compact per-cycle log mode instead of full multi-line output.
         - x-test-tdd reads the task plan, executes Red-Green-Refactor cycles.
         - x-test-tdd delegates each commit to `x-git-commit` via the Skill tool with TDD tags.
         - x-git-commit runs pre-commit chain: x-code-format -> x-code-lint -> compile -> commit.
  2.2.6  Push branch: git push -u origin feat/task-XXXX-YYYY-NNN-desc
  2.2.7  Invoke `x-pr-create` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

             Skill(skill: "x-pr-create", args: "TASK-XXXX-YYYY-NNN")

         - If --auto-approve-pr: PR targets parent branch
         - If NOT --auto-approve-pr: PR targets develop
         - PR body includes task description, TDD cycle summary, coverage
  2.2.8  Update execution-state: task.status = PR_CREATED, task.prNumber, task.prUrl
  2.2.9  APPROVAL GATE:
         - If --auto-approve-pr:
           Auto-merge task PR into parent branch (gh pr merge --squash)
           Update execution-state: task.status = PR_MERGED
         - If NOT --auto-approve-pr:
           Present AskUserQuestion with options:
             APPROVE -> task.status = PR_APPROVED, continue to next task
             REJECT  -> task.status = FAILED, abort lifecycle with message:
                        "Task TASK-NNN rejected. Fix issues and resume with /x-story-implement --task TASK-NNN"
             PAUSE   -> task.status = PR_CREATED, exit lifecycle:
                        "Lifecycle paused at TASK-NNN. Resume later with /x-story-implement {STORY_ID}"
  2.2.10 Update execution-state with final task status and completedAt timestamp
  2.2.11 Update the per-task tracking entry from step 2.2.1a via TaskUpdate:
             TaskUpdate(id: taskLevelTaskId, status: "completed")
         Here `taskLevelTaskId` refers to the numeric task ID returned by the
         TaskCreate call in step 2.2.1a (substitute with the actual integer).
         If the task ended in FAILED, first TaskUpdate the description to prefix
         "(FAILED) " so the failure surfaces in the Claude Code task list.
         If the task ended in BLOCKED, first TaskUpdate the description to prefix
         "(BLOCKED) " so the blocked state surfaces distinctly in the Claude Code
         task list (BLOCKED is a separate terminal state from FAILED — the task
         did not execute at all due to an unresolved dependency).
         Then mark the task completed. execution-state.json remains the
         authoritative record of SUCCESS/FAILED/BLOCKED per CR-04 of EPIC-0033.
END FOR
```

### Step 2.3 -- Approval Gate Detail

The approval gate per task ensures human oversight of every code change:

```
TASK PR READY FOR REVIEW

Task: TASK-XXXX-YYYY-NNN — {task description}
PR: #{prNumber} — {prUrl}
Branch: feat/task-XXXX-YYYY-NNN-desc
TDD Cycles: {count} completed
Coverage: line {linePercent}%, branch {branchPercent}%

Please review the PR and respond with:
  - APPROVE: Mark task as approved, proceed to next task
  - REJECT: Mark task as failed, abort lifecycle (fix and resume later)
  - PAUSE: Save state and exit lifecycle (resume later)
```

### Auto-Approve Mode (--auto-approve-pr, RULE-004)

When `--auto-approve-pr` is active:

1. **Parent branch** `feat/story-XXXX-YYYY-desc` is created from `develop` in Phase 0
2. Each task PR targets the parent branch (NOT `develop`)
3. Task PRs are auto-merged into the parent branch without approval gate
4. After ALL tasks complete, the parent branch contains all task changes
5. The parent branch **NEVER** auto-merges into `develop` or `main` -- it requires a human review PR
6. Phase 3 creates a story-level PR from the parent branch to `develop`

### G1-G7 Fallback (No Test Plan / No Tasks)

If no test plan with TPP markers was produced by Phase 1B and no formal tasks exist, use legacy group-based implementation as a single implicit task:

> **Step 2 (Fallback) -- Implement groups G1-G7** following the task breakdown:
> - For each group: implement all tasks, then compile: `{{COMPILE_COMMAND}}`
> - If compilation fails: fix errors before proceeding
> - After G7: run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
> - Coverage targets: line >= 95%, branch >= 90%

Emit warning: `WARNING: No TDD test plan available. Using G1-G7 group-based implementation as single implicit task. Consider running /x-test-plan for future implementations.`

---

**Last action of Phase 2 — Update the phase task (TaskUpdate):**

    TaskUpdate(id: phase2TaskId, status: "completed")

Here `phase2TaskId` refers to the numeric task ID returned by the earlier `TaskCreate` call at the start of Phase 2 (substitute with the actual integer).

## Phase 3 -- Story-Level Verification (Absorbs Old Phases 3-8)

**First action of Phase 3 — Create a tracking task (TaskCreate):**

    TaskCreate(description: "Phase 3: Story-Level Verification — Story {storyId}")

Record the returned task ID as `phase3TaskId` for the closing TaskUpdate call.

Phase 3 executes after all tasks have approved/merged PRs. It consolidates verification across all task changes.

**Skip condition:** If `--skip-verification` is passed, skip Phase 3 entirely with log `"Phase 3 skipped (--skip-verification)"`.

### Step 3.1 -- Coverage Consolidation

1. Run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}` across all story files
2. Validate coverage thresholds: line >= 95%, branch >= 90%
3. If below thresholds: identify gaps and emit WARNING with specific files/methods

### Step 3.2 -- Cross-File Consistency Check

1. Verify error handling patterns are uniform across classes of the same role
2. Verify constructor patterns, return types, and internal type definitions follow the same shape within a module
3. Inconsistency across files of the same role is a MEDIUM-severity violation
4. Report findings with file paths and specific inconsistencies

### Step 3.3 -- Documentation Update

Read the `interfaces` field from the project identity to determine which documentation generators to invoke.

**Interface Dispatch:**

| Interface | Generator | Output |
|-----------|-----------|--------|
| `rest` | OpenAPI/Swagger generator | `contracts/api/openapi.yaml` |
| `grpc` | gRPC/Proto documentation generator | `contracts/api/grpc-reference.md` |
| `cli` | CLI documentation generator | `contracts/api/cli-reference.md` |
| `graphql` | GraphQL schema documentation generator | `contracts/api/graphql-reference.md` |
| `websocket`, `kafka`, `event-consumer`, `event-producer` | Event-driven documentation generator | `contracts/api/event-reference.md` |

If no documentable interfaces configured: skip interface generators with log `"No documentable interfaces configured"`. Always generate changelog entry.

**Changelog Entry:**
- Read commits since branch point (`git log develop..HEAD --oneline`)
- Generate Conventional Commits summary by type (feat, fix, refactor, test, docs, chore)
- Append to CHANGELOG.md

**Architecture Document Update (Recommended):**
If an architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`:
1. Invoke `x-arch-update` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

       Skill(skill: "x-arch-update", args: "plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md")

   This incrementally updates `steering/service-architecture.md`.
2. New components, integrations, flows, and ADR references are added to the appropriate sections
3. If `steering/service-architecture.md` does not exist, create it from the template

### Step 3.4 -- Review (invoke x-review via Skill tool)

Invoke the `x-review` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-review", args: "{STORY_ID}")

The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

**Template Reference (RULE-007):** Instruct each of the 8 specialist subagents: "Read template at `.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md` for required output format." If the template file does not exist, log `"WARNING: Template _TEMPLATE-SPECIALIST-REVIEW.md not found, using inline format"` and proceed with existing inline format as fallback (RULE-012).

If an architecture plan was generated in Phase 1, provide it as additional context to reviewers.

Collect the consolidated review report with scores and severity counts.

**Consolidated Review Dashboard (RULE-006):**
After collecting all specialist review results, generate a consolidated dashboard:
1. Read template at `.claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` for required output format (RULE-007). If not found, use inline format as fallback (RULE-012).
2. Aggregate scores from all specialists into a single dashboard.
3. Save to `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`.

### Step 3.5 -- Fixes + Remediation

1. **Remediation Tracking (RULE-006):**
   - Read template at `.claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md` for required output format (RULE-007). If not found, use inline format as fallback (RULE-012).
   - Map open findings from the review dashboard to remediation items.
   - For each finding: record original finding, assigned fix action, status (Open/Fixed/Deferred/Accepted), and resolution notes.
   - Save to `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`.
2. Fix ALL failed items from review (every specialist must reach STATUS: Approved)
3. For each fix, follow TDD discipline: write/update the test FIRST, then apply the fix
4. Use atomic commits via `/x-git-commit` for fixes
5. Run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}`
6. Update remediation tracking: mark fixed items as "Fixed" with commit reference.

### Step 3.6 -- Tech Lead Review

Invoke skill `x-review-pr` for holistic review. Requires all items passing for GO. If NO-GO, fix all failed items and re-review (max 2 cycles).

**Dashboard Update (RULE-006):**
After the Tech Lead review completes, update the consolidated review dashboard at `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` with Tech Lead findings.

### Step 3.7 -- Story-Level PR (Auto-Approve Mode Only)

If `--auto-approve-pr` is active:
1. Push parent branch: `git push -u origin feat/story-XXXX-YYYY-desc`
2. Create story-level PR via `gh pr create --base develop` with:
   - Title: `feat(story-XXXX-YYYY): {story title}`
   - Body: consolidated review summary, task list with PR links, coverage report
   - This PR requires human review -- it is NEVER auto-merged (RULE-004)

If NOT `--auto-approve-pr`: skip (individual task PRs already target develop).

### Step 3.8 -- Final Verification + Cleanup

1. Update README if needed
2. Update IMPLEMENTATION-MAP:
   a. Read `plans/epic-XXXX/IMPLEMENTATION-MAP.md`
   b. Find the current story's row in the dependency matrix
   c. Update the Status column: replace current value with `Concluida`
   d. Write the updated file
3. Update Story File Status:
   a. Read `plans/epic-XXXX/story-XXXX-YYYY.md`
   b. Update the `**Status:**` line from `Pendente` to `Concluida`
   c. In Section 8 (Sub-tarefas), mark completed sub-tasks: change `- [ ]` to `- [x]`
   d. Write the updated file
4. Jira Status Sync (conditional):
   a. Read the story file's `**Chave Jira:**` field
   b. If the value is not `-` and not `<CHAVE-JIRA>` (i.e., has a real Jira key):
      - Call `mcp__atlassian__getTransitionsForJiraIssue` with the story's Jira key
      - Find the transition to "Done"
      - Call `mcp__atlassian__transitionJiraIssue` to transition the issue
      - If transition fails: log warning, continue (non-blocking)
5. Update execution-state.json:
   - Set all completed task statuses to DONE
   - Set storyStatus to COMPLETE
6. Run DoD checklist:
   - [ ] All task PRs approved/merged
   - [ ] Coverage >= 95% line, >= 90% branch
   - [ ] Zero compiler/linter warnings
   - [ ] Commits show test-first pattern (test precedes or accompanies implementation in git log)
   - [ ] Acceptance tests exist and pass (AT-N GREEN)
   - [ ] Tests follow TPP ordering (simple to complex)
   - [ ] No test-after commits (all tests written before or with implementation)
   - [ ] Story markdown file updated with Status: Concluida
   - [ ] IMPLEMENTATION-MAP Status column updated for this story
   - [ ] At least 1 automated test validates the story's primary acceptance criterion
   - [ ] Smoke test passes (if testing.smoke_tests == true)
7. Conditional DoD items:
   - Contract tests pass (if testing.contract_tests == true)
   - Event schemas registered (if event_driven)
   - Compliance requirements met (if security.compliance active)
   - Gateway configuration updated (if api_gateway != none)
   - gRPC proto backward compatible (if interfaces contain grpc)
   - GraphQL schema backward compatible (if interfaces contain graphql)
   - [ ] Threat model updated (if security findings with severity >= Medium)
   - Post-deploy verification passed or skipped (if testing.smoke_tests == true)
8. Post-Deploy Verification (conditional: `testing.smoke_tests == true`):
   - If `testing.smoke_tests` is `false` -> SKIP with log: "Post-deploy verification skipped (testing.smoke_tests=false)"
   - If `testing.smoke_tests` is `true`, execute checks (invoke `/x-test-e2e` or configured smoke test):
     - **Health Check**: GET /health (or configured endpoint) -> 200 OK
     - **Critical Path**: Execute primary request flow -> valid response
     - **Response Time**: Verify p95 latency < configured SLO
     - **Error Rate**: Verify error rate < 1% threshold
   - Non-blocking: emit result for human decision, do NOT auto-rollback
9. Report PASS/FAIL/SKIP result with task-level summary
10. **Mode-aware worktree removal + repository sync (Rule 14 §2 forbids `develop` checkout inside a worktree; Rule 14 §5 — Creator Owns Removal):**
    - If branch creation ran under **Mode 1 (REUSE — orchestrated)**: do NOT remove the worktree (the orchestrator `x-epic-implement` is the creator and owns removal) and do NOT run `git checkout develop && git pull origin develop` here (would violate Rule 14 §2 by checking out a protected branch inside a worktree).
    - If branch creation ran under **Mode 2 (CREATE — standalone opt-in)** AND the DoD checklist above passed:
      1. Remove the worktree first via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL):

             Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")

      2. After removal, switch execution context back to `mainRepoPath` (captured at Step 6a) and run:

             git checkout develop && git pull origin develop

    - If branch creation ran under **Mode 2 (CREATE — standalone opt-in)** AND the story FAILED or had unrecovered errors: preserve the worktree for diagnosis (Rule 14 §4). Log the preserved path and instruct the operator to run `Skill(skill: "x-git-worktree", args: "remove --force --id story-XXXX-YYYY")` after triage. Do NOT run `git checkout develop && git pull origin develop` while the worktree is preserved (same Rule 14 §2 protection).
    - If branch creation ran under **Mode 3 (LEGACY — main checkout)**: no worktree was created. Run `git checkout develop && git pull origin develop` in the main checkout as before.

**Last action of Phase 3 — Update the phase task (TaskUpdate):**

    TaskUpdate(id: phase3TaskId, status: "completed")

Here `phase3TaskId` refers to the numeric task ID returned by the earlier `TaskCreate` call at the start of Phase 3 (substitute with the actual integer).

**Phase 3 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer (via x-test-tdd) | Phase 2 | Adaptive (per task) |
| Specialist Reviews | Phase 3.4 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 3.6 | Adaptive (story max tier) |


## Graceful Degradation

When invoked by `x-epic-implement`, the lifecycle skill respects context pressure levels communicated via the subagent prompt. The epic orchestrator manages pressure detection and level advancement (see Section 1.7 in `x-epic-implement`).

### Pressure-Aware Behavior

| Level | Lifecycle Behavior |
|-------|--------------------|
| Level 0 (Normal) | Full execution — all phases, full logging, reviews enabled |
| Level 1 (Warning) | Reduce log output to status lines only; use slim mode when invoking review skills; skip non-essential documentation generation |
| Level 2 (Critical) | Skip Phase 3 reviews (specialist + tech lead); minimize output in all tool calls; include `"CONTEXT PRESSURE: minimize output"` when delegating to nested skills |
| Level 3 (Emergency) | Not applicable — epic orchestrator saves state and exits before dispatching at Level 3 |

### Detection Within Lifecycle

If the lifecycle skill detects pressure signals independently (e.g., tool calls returning "output too large", truncated responses), it MUST:

1. Log: `"CONTEXT PRESSURE signal detected in x-story-implement for {storyId}"`
2. Include `"contextPressureDetected": true` in the `SubagentResult` returned to the orchestrator
3. Apply Level 1 actions locally (reduce verbosity, slim mode) as a defensive measure
4. Continue execution — the orchestrator handles level advancement

## Error Classification

When a tool call or subagent fails, classify the error before deciding on recovery action:

| Category | Detection Patterns (case-insensitive) | Action |
|----------|---------------------------------------|--------|
| **TRANSIENT** | `"overloaded"`, `"rate limit"`, `"429"`, `"503"`, `"504"`, `"timeout"`, `"ETIMEDOUT"`, `"capacity"`, `"502"` | Retry with exponential backoff |
| **CONTEXT** | `"context"`, `"token limit"`, `"too long"`, `"exceeded"`, `"output too large"`, `"truncated"` | Graceful degradation |
| **PERMANENT** | All errors not matching TRANSIENT or CONTEXT patterns | Fail immediately with contextual error message |

**Default:** If no pattern matches, classify as **PERMANENT** and fail immediately.

### Retry with Exponential Backoff (Tool Calls)

| Retry | Delay |
|-------|-------|
| 1 | 2 seconds |
| 2 | 4 seconds |
| 3 | 8 seconds |
| After 3 failures | Mark task as FAILED |

**PERMANENT errors MUST NOT be retried.**

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with message: `Story file not found at {path}` |
| Dependency story not complete | Abort with message: `Dependency {storyId} not complete. Complete it first.` |
| Task dependency not resolved | Mark task as BLOCKED, skip in current iteration |
| x-test-tdd fails during Phase 2 | Mark task as FAILED, update execution-state, abort or retry |
| Coverage below thresholds | Fail Phase 3.1, add tests until thresholds met |
| Architecture plan skill unavailable | Use inline fallback -- expand Phase 1B subagent |
| Template file not found (RULE-012) | Log warning, use inline format as fallback |
| Review score below approval (Phase 3) | Fix all failed items and re-review (max 2 cycles) |
| Phase 1B test plan produces no output | Phase 2 uses G1-G7 fallback mode with warning |
| PR creation fails | Log error, mark task as FAILED, update execution-state |
| Resume with corrupted execution-state | Reinitialize state from PR statuses via `gh pr view` |
| Context pressure signal detected | Log warning, set `contextPressureDetected: true` in result, apply Level 1 actions locally |
| Reference file not found (RULE-002) | Log warning, continue without reference |

**Error Reporting to Epic Orchestrator:** When `x-story-implement` is invoked as a subagent by `x-epic-implement`, errors MUST be reported back via the `SubagentResult` JSON fields: `errorType`, `errorMessage`, and `errorCode`.

## Template Fallback

Templates referenced by this skill follow RULE-012. When a template file does not exist, the skill degrades gracefully with a logged warning:

- `_TEMPLATE-IMPLEMENTATION-PLAN.md` -- inline section list used in Step 1B subagent
- `_TEMPLATE-TEST-PLAN.md` -- test plan skill handles its own fallback
- `_TEMPLATE-TASK-BREAKDOWN.md` -- task decomposer handles its own fallback
- `_TEMPLATE-SECURITY-ASSESSMENT.md` -- inline format for security assessment
- `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` -- inline format for compliance assessment
- `_TEMPLATE-SPECIALIST-REVIEW.md` -- inline format for specialist reports
- `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` -- inline format for dashboard
- `_TEMPLATE-REVIEW-REMEDIATION.md` -- inline format for remediation tracking
- `_TEMPLATE-TECH-LEAD-REVIEW.md` -- inline format for Tech Lead review

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-test-tdd` | Invokes (Phase 2, per task) | TDD execution for each task |
| `x-git-commit` | Invoked by x-test-tdd | Atomic commits with TDD tags and pre-commit chain |
| `x-pr-create` | Invokes (Phase 2, per task) | Task-level PR creation |
| `x-code-format` | Invoked by x-git-commit chain | Code formatting in pre-commit |
| `x-code-lint` | Invoked by x-git-commit chain | Code linting in pre-commit |
| `x-task-plan` | Invokes (Phase 1, per task) | Individual task planning |
| `x-arch-plan` | Invokes (Phase 1, conditional) | Architecture planning |
| `x-test-plan` | Invokes (Phase 1B) | Test plan as implementation roadmap |
| `x-lib-task-decomposer` | Invokes (Phase 1C) | Task decomposition with TDD markers |
| `x-review` | Invokes (Phase 3.4) | Parallel specialist reviews |
| `x-review-pr` | Invokes (Phase 3.6) | Tech Lead holistic review |
| `x-arch-update` | Invokes (Phase 3.3, conditional) | Architecture document update |
| `x-git-worktree` | Invokes (Phase 0 Step 6a/6c, Phase 3 Step 3.8) | Worktree context detection (mandatory pre-branch), standalone worktree creation (`--worktree` flag, Mode 2), and Creator-Owned removal at end of Phase 3 (Rule 14 + ADR-0004) |
| `x-epic-implement` | Called by | Epic orchestrator delegates story execution |
| `x-git-push` | Invokes (Phase 2, branch creation) | Branch creation for tasks |

All `{{PLACEHOLDER}}` tokens (e.g., `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration -- they are NOT resolved during generation.
