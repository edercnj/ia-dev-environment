---
name: x-dev-lifecycle
description: "Orchestrates the complete feature implementation cycle with task-centric workflow: branch creation, planning, per-task TDD execution with individual PRs and approval gates, story-level verification, and final cleanup. Delegates implementation to x-tdd, commits to x-commit, PRs to x-pr-create."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID or feature-name] [--auto-approve-pr] [--task TASK-ID] [--skip-verification] [--full-lifecycle]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Feature Lifecycle (Task-Centric Orchestrator)

## Purpose

Orchestrate the complete feature implementation cycle using a task-centric workflow. Each task in the story produces its own branch, PR, and approval gate. The lifecycle delegates TDD implementation to `x-tdd`, commits to `x-commit`, and PR creation to `x-pr-create`. Phase 2 is a Task Execution Loop; Phase 3 is Story-Level Verification that absorbs the old review/fix/PR/verification phases.

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
Phase 2: Task Execution Loop      (for each task: branch -> x-tdd -> x-pr-create -> approval gate)
Phase 3: Story-Level Verification (coverage, cross-file consistency, smoke test, final report)
```

### RULE-001: Task as Unit of Delivery

Each task = 1 branch = 1 PR. Tasks are the atomic unit of delivery. The lifecycle orchestrates, individual skills implement:

| Concern | Skill | Invoked By |
|---------|-------|------------|
| TDD implementation | `x-tdd` | Phase 2 (per task) |
| Atomic commits | `x-commit` | `x-tdd` (per TDD cycle) |
| PR creation | `x-pr-create` | Phase 2 (per task) |
| Code formatting | `x-format` | `x-commit` (pre-commit chain) |
| Code linting | `x-lint` | `x-commit` (pre-commit chain) |
| Task planning | `x-plan-task` | Phase 1 (if not PRE_PLANNED) |

---

## Phase 0 -- Preparation (Orchestrator -- Inline)

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
6. **Branch Creation:**
   - If `--auto-approve-pr`: create parent branch `feat/story-XXXX-YYYY-desc` from `develop`. All task branches will be created from and target this parent branch.
   - If NOT `--auto-approve-pr`: no parent branch needed. Task branches will target `develop`.
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

Invoke `/x-contract-lint {CONTRACT_PATH}` to validate the generated contract against
its specification. If validation errors are found:
1. Fix the errors in the generated contract
2. Re-run validation until the contract passes

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

## Phase 1 -- Architecture Planning (Skill Invocation + Subagent Fallback)

**If the architecture plan file already exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` (as checked in Phase 0), skip Step 1A and proceed directly to Step 1B, ensuring Step 1B reads the existing plan.**

### Step 1A: Architecture Plan via x-dev-architecture-plan

Evaluate change scope using the decision tree:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

**If Full or Simplified:**

Invoke skill `/x-dev-architecture-plan {STORY_PATH}`.

- Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
- If the skill invocation fails: emit `WARNING: Architecture plan generation failed. Continuing without architecture plan.` and proceed to Step 1B.

**If Skip:**

Log `"Architecture plan not needed for this change scope"` and proceed to Step 1B.

### Step 1B: Implementation Plan (Subagent via Task)

**Skip condition:** If Phase 0 pre-check marked the implementation plan as "Reuse", skip this step entirely and log `"Reusing existing implementation plan from {date}"`.

Launch a **single** `general-purpose` subagent with `model: opus` (RULE-009):

> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
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

### Fallback: Inline Architecture Planning

If skill `x-dev-architecture-plan` is not available in the project (skill file `skills/x-dev-architecture-plan/SKILL.md` does not exist), combine architecture planning into the Step 1B subagent by expanding its prompt to also read:
- `skills/protocols/references/` -- protocol conventions
- `skills/security/references/` -- OWASP, headers, secrets
- `skills/observability/references/` -- tracing, metrics, logging
- `skills/resilience/references/` -- circuit breaker, retry, fallback

This preserves the pre-integration behavior for projects that do not include the architecture-plan skill.

## Phases 1B-1F -- Parallel Planning (Subagents via Task -- SINGLE message)

**CRITICAL: ALL planning subagents (except those skipped by pre-checks) MUST be launched in a SINGLE message.**

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)

**Skip condition:** If Phase 0 pre-check marked the test plan as "Reuse", skip this step entirely and log `"Reusing existing test plan from {date}"`.

Invoke skill `x-test-plan` -> produces `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

The test plan is the **implementation roadmap** for Phase 2. It produces:
- Acceptance tests (AT-N) as outer loop (Double-Loop TDD)
- Unit tests (UT-N) in TPP order as inner loop (Levels 1-6: degenerate -> edge cases)
- Integration tests (IT-N) positioned after related UTs
- `Depends On: TASK-N` and `Parallel` markers per scenario

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition

**Skip condition:** If Phase 0 pre-check marked the task breakdown as "Reuse", skip this step entirely and log `"Reusing existing task breakdown from {date}"`.

Invoke skill `x-lib-task-decomposer` -> produces `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

The task decomposer auto-detects decomposition mode:
- If test plan with TPP markers exists -> test-driven tasks (RED/GREEN/REFACTOR per task, with `Parallel` flags)
- If no test plan -> fallback to G1-G7 layer-based decomposition

### 1D: Event Schema Design (if event_driven)
Launch `general-purpose` subagent:

> You are an **Event Engineer** designing event schemas.
> Read `skills/protocols/references/event-driven-conventions.md` for standards.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce event schema design: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.

### 1E: Security Assessment (MANDATORY)

**Skip condition:** If Phase 0 pre-check marked the security assessment as "Reuse", skip this step entirely and log `"Reusing existing security assessment from {date}"`.

Invoke skill `/x-threat-model` via the Skill tool:
  Skill(skill: "x-threat-model", args: "{STORY_PATH}")

Output: `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`

If `/x-threat-model` is unavailable, fall back to `general-purpose` subagent:

> You are a **Security Engineer** assessing security impact.
> Read template at `.claude/templates/_TEMPLATE-SECURITY-ASSESSMENT.md` for required output format (RULE-007). If the template file does not exist, log `"WARNING: Template _TEMPLATE-SECURITY-ASSESSMENT.md not found, using inline format"` and use inline format as fallback (RULE-012).
> Read `skills/security/SKILL.md` -> then read its references.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce security assessment: threat model, OWASP Top 10 mapping, authentication/authorization review, input validation, data protection, secrets management.
> Save to `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`.

### 1F: Compliance Assessment (CONDITIONAL -- if compliance active)

**Activation:** This phase executes ONLY when the project has compliance enabled in setup config (compliance field is not "none"). If compliance is not active, skip entirely with log `"Compliance assessment skipped (compliance not active)"`.

**Skip condition:** If Phase 0 pre-check marked the compliance assessment as "Reuse", skip this step entirely and log `"Reusing existing compliance assessment from {date}"`.

Launch `general-purpose` subagent:

> You are a **Security Engineer** assessing compliance impact.
> Read template at `.claude/templates/_TEMPLATE-COMPLIANCE-ASSESSMENT.md` for required output format (RULE-007). If the template file does not exist, log `"WARNING: Template _TEMPLATE-COMPLIANCE-ASSESSMENT.md not found, using inline format"` and use inline format as fallback (RULE-012).
> Read `skills/compliance/SKILL.md` -> then read its references.
> Read the implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations.
> Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.

---

## Phase 2 -- Task Execution Loop (RULE-001: 1 Task = 1 Branch = 1 PR)

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
  2.2.2  Update execution-state: task.status = IN_PROGRESS
  2.2.3  Create task branch:
         - If --auto-approve-pr: branch from parent branch (feat/story-XXXX-YYYY-desc)
         - If NOT --auto-approve-pr: branch from develop
         - Branch name: feat/task-XXXX-YYYY-NNN-desc
  2.2.4  Read task plan (if PRE_PLANNED mode):
         - Read plans/epic-XXXX/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md
         - Use Implementation Guide and TDD cycles from the plan
  2.2.5  Invoke /x-tdd TASK-XXXX-YYYY-NNN:
         - x-tdd reads the task plan, executes Red-Green-Refactor cycles
         - x-tdd delegates each commit to /x-commit with TDD tags
         - x-commit runs pre-commit chain: x-format -> x-lint -> compile -> commit
  2.2.6  Push branch: git push -u origin feat/task-XXXX-YYYY-NNN-desc
  2.2.7  Invoke /x-pr-create TASK-XXXX-YYYY-NNN:
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
                        "Task TASK-NNN rejected. Fix issues and resume with /x-dev-lifecycle --task TASK-NNN"
             PAUSE   -> task.status = PR_CREATED, exit lifecycle:
                        "Lifecycle paused at TASK-NNN. Resume later with /x-dev-lifecycle {STORY_ID}"
  2.2.10 Update execution-state with final task status and completedAt timestamp
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

## Phase 3 -- Story-Level Verification (Absorbs Old Phases 3-8)

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
1. Invoke `x-dev-arch-update` to incrementally update `steering/service-architecture.md`
2. New components, integrations, flows, and ADR references are added to the appropriate sections
3. If `steering/service-architecture.md` does not exist, create it from the template

### Step 3.4 -- Review (Invoke /x-review)

Invoke skill `/x-review` for the current story. The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

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
4. Use atomic commits via `/x-commit` for fixes
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
   - If `testing.smoke_tests` is `true`, execute checks (invoke `/run-e2e` or configured smoke test):
     - **Health Check**: GET /health (or configured endpoint) -> 200 OK
     - **Critical Path**: Execute primary request flow -> valid response
     - **Response Time**: Verify p95 latency < configured SLO
     - **Error Rate**: Verify error rate < 1% threshold
   - Non-blocking: emit result for human decision, do NOT auto-rollback
9. Report PASS/FAIL/SKIP result with task-level summary
10. `git checkout develop && git pull origin develop`

**Phase 3 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer (via x-tdd) | Phase 2 | Adaptive (per task) |
| Specialist Reviews | Phase 3.4 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 3.6 | Adaptive (story max tier) |

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with message: `Story file not found at {path}` |
| Dependency story not complete | Abort with message: `Dependency {storyId} not complete. Complete it first.` |
| Task dependency not resolved | Mark task as BLOCKED, skip in current iteration |
| x-tdd fails during Phase 2 | Mark task as FAILED, update execution-state, abort or retry |
| Coverage below thresholds | Fail Phase 3.1, add tests until thresholds met |
| Architecture plan skill unavailable | Use inline fallback -- expand Phase 1B subagent |
| Template file not found (RULE-012) | Log warning, use inline format as fallback |
| Review score below approval (Phase 3) | Fix all failed items and re-review (max 2 cycles) |
| Phase 1B test plan produces no output | Phase 2 uses G1-G7 fallback mode with warning |
| PR creation fails | Log error, mark task as FAILED, update execution-state |
| Resume with corrupted execution-state | Reinitialize state from PR statuses via `gh pr view` |

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
| `x-tdd` | Invokes (Phase 2, per task) | TDD execution for each task |
| `x-commit` | Invoked by x-tdd | Atomic commits with TDD tags and pre-commit chain |
| `x-pr-create` | Invokes (Phase 2, per task) | Task-level PR creation |
| `x-format` | Invoked by x-commit chain | Code formatting in pre-commit |
| `x-lint` | Invoked by x-commit chain | Code linting in pre-commit |
| `x-plan-task` | Invokes (Phase 1, per task) | Individual task planning |
| `x-dev-architecture-plan` | Invokes (Phase 1, conditional) | Architecture planning |
| `x-test-plan` | Invokes (Phase 1B) | Test plan as implementation roadmap |
| `x-lib-task-decomposer` | Invokes (Phase 1C) | Task decomposition with TDD markers |
| `x-review` | Invokes (Phase 3.4) | Parallel specialist reviews |
| `x-review-pr` | Invokes (Phase 3.6) | Tech Lead holistic review |
| `x-dev-arch-update` | Invokes (Phase 3.3, conditional) | Architecture document update |
| `x-dev-epic-implement` | Called by | Epic orchestrator delegates story execution |
| `x-git-push` | Invokes (Phase 2, branch creation) | Branch creation for tasks |

All `{{PLACEHOLDER}}` tokens (e.g., `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration -- they are NOT resolved during generation.
