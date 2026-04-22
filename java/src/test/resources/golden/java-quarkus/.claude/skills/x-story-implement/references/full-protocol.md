# x-story-implement — Full Protocol

> **Slim/Full split** per [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md).
> The `SKILL.md` sibling carries the minimum viable contract (Triggers,
> Parameters, Output Contract, phase skeleton with canonical delegation
> snippets, Error Envelope). This file carries the full behavioral
> protocol: detailed decision tables, per-planner subagent prompts,
> step-by-step prose, template-fallback matrix, graceful degradation,
> error classification with retry-backoff, and the v2 Wave-Dispatch
> appendix.

## 1. Phase 0 — Preparation (Full)

### 1.1 Artifact Pre-checks (RULE-002 — Idempotency via Staleness Check)

For each artifact type listed below, check existence and staleness. If `mtime(story) <= mtime(plan)`, the plan is fresh — reuse it. If `mtime(story) > mtime(plan)`, the plan is stale — mark for regeneration. If the plan does not exist, mark for generation.

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

### 1.2 Worktree-First Branch Creation (Rule 14 + ADR-0004)

Branch creation in `x-story-implement` follows the **worktree-first policy** defined in [ADR-0004](../../../../../../../../../adr/ADR-0004-worktree-first-branch-creation-policy.md) and the normative invariants of Rule 14. The routine below is **mandatory** and MUST execute before any `git checkout -b` or `/x-git-worktree create` call.

**Step 6a — Detect worktree context (Rule 14 §3 — Non-Nesting Invariant).** Invoke the `x-git-worktree` skill via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL):

    Skill(skill: "x-git-worktree", args: "detect-context")

The skill returns a JSON envelope:

```json
{
  "inWorktree": true,
  "worktreePath": "/abs/path/to/.claude/worktrees/story-XXXX-YYYY",
  "mainRepoPath": "/abs/path/to/repo"
}
```

Record `inWorktree`, `worktreePath`, and `mainRepoPath` for use in subsequent steps. Skipping this call risks creating a nested worktree (Rule 14 §3) or causing the harness to apply file operations in the wrong checkout.

**Step 6b — Select the branching mode:**

| # | Condition | Mode | Action |
| :--- | :--- | :--- | :--- |
| 1 | `inWorktree == true` | **REUSE (orchestrated)** | Reuse the current worktree. Do NOT invoke `/x-git-worktree create`. Do NOT create a nested worktree (Rule 14 §3). Branch creation inside the reused worktree follows the `--auto-approve-pr` legacy behavior via `git checkout -b`. The creator of the outer worktree owns its removal (Rule 14 §5). |
| 2 | `inWorktree == false` AND `--worktree` present | **CREATE (standalone opt-in)** | Provision a dedicated worktree via `Skill(skill: "x-git-worktree", args: "create --branch feat/story-XXXX-YYYY-desc --base develop --id story-XXXX-YYYY")`. `x-story-implement` is the creator and owns removal (Rule 14 §5 — end of Phase 3 on success; preserved on failure per Rule 14 §4). |
| 3 | `inWorktree == false` AND `--worktree` absent | **LEGACY (main checkout)** | Create branches directly in the main working tree via `git checkout -b`. Preserves backward compatibility. |

> **Orchestrator auto-path.** When this skill is dispatched by `x-epic-implement`, the parent creates the worktree **before** dispatching, and this invocation detects `inWorktree == true` and selects Mode 1 (REUSE) automatically. No flag is required from the caller.

> **Anti-pattern (DO NOT USE):** `Agent(isolation:"worktree")` is DEPRECATED (see ADR-0004 and Rule 14 §7). The harness-native isolation is replaced by explicit `/x-git-worktree create` calls so the worktree lifecycle is visible in logs and recoverable on failure.

**Step 6c — Execute the selected branching mode.**

- **Mode 1 (REUSE).** The parent orchestrator has already placed the process inside the worktree at `worktreePath`. Proceed with branch creation *inside* that worktree:
  - If `--auto-approve-pr`: create parent branch `feat/story-XXXX-YYYY-desc` from `develop` via `git checkout -b`. All task branches are created from and target this parent branch.
  - If NOT `--auto-approve-pr`: no parent branch is created at this stage. Task branches are created later (Phase 2) and target `develop` directly.
  - Do NOT call `/x-git-worktree remove` here or at end of Phase 3. The orchestrator is the creator and owns removal.

- **Mode 2 (CREATE).** Invoke `x-git-worktree` via the Skill tool:

      Skill(skill: "x-git-worktree", args: "create --branch feat/story-XXXX-YYYY-desc --base develop --id story-XXXX-YYYY")

  The operation creates `.claude/worktrees/story-XXXX-YYYY/` with `feat/story-XXXX-YYYY-desc` checked out. That branch always exists because it anchors the standalone worktree.
  - If `--auto-approve-pr`: that branch IS the parent branch.
  - If NOT `--auto-approve-pr`: that branch is an isolation branch only; it is **not** treated as the parent branch for task PR flow. Task branches target `develop` directly.
  - In standalone mode `x-story-implement` is the creator and MUST invoke `Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")` at end of Phase 3 on success. On failure, the worktree is preserved for diagnosis.

- **Mode 3 (LEGACY).** Execute the pre-EPIC-0037 behavior unchanged, operating directly in the main working tree:
  - If `--auto-approve-pr`: `git checkout -b feat/story-XXXX-YYYY-desc develop` creates the parent branch.
  - If NOT `--auto-approve-pr`: no parent branch; task branches target `develop` directly.

**Step 6d — Logging.** Log one of:

- `"Branch creation mode: REUSE (inside worktree {worktreePath})"`
- `"Branch creation mode: CREATE (standalone --worktree, provisioning worktree for story-XXXX-YYYY)"`
- `"Branch creation mode: LEGACY (main checkout, no --worktree flag)"`

**Step 6e — Record `STORY_OWNS_WORKTREE`:**

| Selected Mode | `STORY_OWNS_WORKTREE` | Rationale |
| :--- | :--- | :--- |
| Mode 1 (REUSE) | `false` | Outer orchestrator is creator. |
| Mode 2 (CREATE) | `true` | `x-story-implement` is creator. |
| Mode 3 (LEGACY) | `false` | No worktree created. |

Phase 3 Step 3.8b reads this variable to decide whether to invoke removal. `STORY_ID` throughout MUST match `story-\d{4}-\d{4}` (Rule 14 §1).

### 1.3 Scope Assessment

Classify the story to determine lifecycle phase optimization.

**Classification criteria:**

| Criterion | How to detect |
|-----------|--------------|
| Components affected | Count distinct `.java`/`.kt`/`.py`/`.ts`/`.go`/`.rs` file mentions in tech description |
| New endpoints | Count `POST/GET/PUT/DELETE/PATCH /path` patterns in data contracts |
| Schema changes | Presence of "migration script", "ALTER TABLE", "CREATE TABLE", "DROP TABLE", "ADD COLUMN" |
| Compliance | `compliance:` field with value other than `"none"` |
| Dependents | Count stories that depend on this one (from IMPLEMENTATION-MAP) |

**Tier classification:**

| Tier | Criteria | Phase Behavior |
|------|----------|---------------|
| SIMPLE | ≤1 component, 0 endpoints, 0 schema changes, no compliance | Skip phases 1B, 1C, 1D, 1E |
| STANDARD | 2–3 components OR 1–2 new endpoints | All phases execute normally |
| COMPLEX | ≥4 components OR schema changes OR compliance | All phases + stakeholder review after Phase 2 |

**Elevation rules:** compliance always elevates to COMPLEX; schema changes always elevate to at least COMPLEX; a single COMPLEX criterion is sufficient.

`--full-lifecycle` forces STANDARD-equivalent execution regardless of tier. Unclassifiable stories default to STANDARD (no error).

### 1.4 Planning Mode Detection

| Mode | Condition | Behavior |
|------|-----------|----------|
| PRE_PLANNED | All 7 artifact types exist AND `mtime(story) <= mtime(artifact)` for all | Skip Phase 1A-1F entirely. Phase 2 uses task plans directly. |
| HYBRID | Some artifacts exist but not all | Generate ONLY the missing artifacts in Phase 1. Reuse existing fresh ones. |
| INLINE | No artifacts exist | Execute Phase 1A-1F as currently defined (backward compatible). |

Log: `"Planning mode: {MODE}"`. When HYBRID, also log `"Planning mode: HYBRID -- generating {N} missing artifact(s): {list}"`.

### 1.5 Resume Detection (RULE-014)

Check for `execution-state.json` in the story's plan directory.

| Status Anterior | Condition | Novo Status |
|-----------------|-----------|-------------|
| IN_PROGRESS | No PR found (`gh pr view` fails) | PENDING |
| IN_PROGRESS | PR open | PR_CREATED |
| PR_CREATED | PR approved | PR_APPROVED |
| PR_CREATED | PR merged | DONE |
| PR_CREATED | PR closed | FAILED |
| BLOCKED | Dependencies now resolved | PENDING |
| BLOCKED | Dependencies still blocked | BLOCKED (keep) |
| DONE | — | DONE (skip) |

Log: `"Resume: reclassified TASK-NNN from {old} to {new}"`. DONE tasks skip in Phase 2. FAILED tasks reclassify to PENDING for retry. Empty state → all tasks PENDING.

## 1.6 Phase 0.5 — API-First Contract Generation (Conditional)

> **RULE-005:** Formal contract before implementation. This phase generates and validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) before any implementation code.

**Activation:** this phase is ONLY executed when the story declares interfaces of type `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`. Stories without these interface types skip directly to Phase 1.

### Step 0.5.1 — Interface Detection

| Interface Type | Contract Format | Output Path |
|---------------|----------------|-------------|
| `rest` | OpenAPI 3.1 | `contracts/{STORY_ID}-openapi.yaml` |
| `grpc` | Protobuf 3 | `contracts/{STORY_ID}.proto` |
| `event-consumer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `event-producer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `websocket` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |

### Step 0.5.2 — Contract Generation

Generate a draft contract in the appropriate format using data contracts from the story:

- **REST (OpenAPI 3.1):** extract endpoints, DTOs, status codes from story data contracts. Generate `openapi: "3.1.0"` spec with `info`, `paths`, `components/schemas`, RFC 7807 errors.
- **gRPC (Protobuf 3):** extract service definitions, request/response messages. Generate `.proto` file with `syntax = "proto3"`, service, and message definitions.
- **Event (AsyncAPI 2.6):** extract event names, channels, payload schemas. Generate `asyncapi: "2.6.0"` spec with `channels`, `components/messages`, `components/schemas`.

Ensure directory exists: `mkdir -p contracts/`.

### Step 0.5.3 — Contract Validation

Perform inline contract validation using the appropriate linter for `{CONTRACT_FORMAT}`:

- **OpenAPI 3.1:** `npx @redocly/cli lint {CONTRACT_PATH}` or `spectral lint {CONTRACT_PATH}`.
- **Protobuf 3:** `protoc --lint_out=. {CONTRACT_PATH}` or `buf lint {CONTRACT_PATH}`.
- **AsyncAPI 2.6:** `npx @asyncapi/cli validate {CONTRACT_PATH}` or `spectral lint {CONTRACT_PATH}`.

If validation errors are found: fix errors in the generated contract and re-run validation until it passes.

> **Note:** a dedicated `x-test-contract-lint` skill does not exist in `core/` at the time of writing (the reference was an orphan removed in EPIC-0033 / STORY-0033-0001). If `x-test-contract-lint` is added in the future, convert this step to `Skill(skill: "x-test-contract-lint", args: "{CONTRACT_PATH}")` following Rule 13 — Skill Invocation Protocol (INLINE-SKILL pattern).

### Step 0.5.4 — Approval Gate (EPIC-0043)

**Deprecated flags:** `--manual-contract-approval` is a no-op (EPIC-0043). Emit one-time warning `"[DEPRECATED] --manual-contract-approval is no longer needed; the gate menu is now the default."` and continue.

**`--non-interactive` mode:** when present AND Step 0.5.3 validation passes, auto-approve and proceed to Phase 1 without pausing. Log: `"Contract auto-approved (--non-interactive, validation passed): {CONTRACT_PATH}"`. Set `contractStatus = APPROVED`.

**Default behavior (interactive menu, Rule 20 canonical option menu, no-PR variant):**

```markdown
AskUserQuestion(
  question: "Contract generated at {CONTRACT_PATH}. Format: {CONTRACT_FORMAT}. Review the contract and choose an action.",
  options: [
    {
      header: "Proceed",
      label: "Continue (Recommended)",
      description: "Approve the contract and proceed to Phase 1 (Architecture Planning)."
    },
    {
      header: "Reject",
      label: "Regenerate and retry",
      description: "Returns to Step 0.5.2 with feedback to regenerate the contract; reapresents this menu on return. Use when the generated contract has errors or missing endpoints."
    },
    {
      header: "Abort",
      label: "Cancel the operation",
      description: "Terminates the skill. The contract file is preserved for inspection."
    }
  ]
)
```

- **PROCEED:** set `contractStatus = APPROVED` and proceed to Phase 1.
- **REJECT:** prompt for feedback; return to Step 0.5.2 with feedback; on regen return, represent this gate menu. Apply Rule 20 §FIX-PR Loop-Back guard-rail: track each REJECT in `fixAttempts[]`; at 3 rejects, emit `GATE_FIX_LOOP_EXCEEDED` and terminate.
- **ABORT:** log `"Contract gate aborted by operator."` and terminate. Contract file preserved at `{CONTRACT_PATH}`.

## 2. Phase 1 — Parallel Planning Detail

### 2.1 Phase 1A — Architecture Plan

Evaluate change scope:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

**If Full or Simplified:**

    TaskCreate(description: "Planning: Architecture Plan — Story {storyId}")
    Skill(skill: "x-arch-plan", args: "{STORY_PATH}")
    TaskUpdate(id: archPlanTaskId, status: "completed")

Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`. On failure: emit `WARNING: Architecture plan generation failed. Continuing without architecture plan.`, still close tracking, proceed to 1B.

### 2.2 Parallel Planning — Batch Protocol

**Batch A (first assistant message — all TaskCreate + invocations together):**

- Emit every active planner's `TaskCreate(description: "Planning: {artifact} — Story {storyId}")`.
- Emit every active planner's `Skill(skill: "...", args: "...")` or `Agent(subagent_type:"general-purpose", ...)` launch.
- Record returned task IDs in an in-memory map (e.g., `planningTasks["testPlan"] = <id>`).
- All tool calls MUST be siblings in ONE assistant message (the runtime dispatches them in parallel). Splitting across messages serializes execution and defeats the Phase 1B-1F parallelism.

**Wait for all planners** (runtime holds subsequent messages until all siblings complete).

**Batch B (second assistant message — all TaskUpdate together):**

- For each planner launched in Batch A, emit `TaskUpdate(id: planningTasks[...], status: "completed")` as siblings in ONE message.
- Subagent-managed planners (1B Impl Plan, 1D Event Schema, 1E fallback, 1F Compliance) close their OWN tracking tasks from inside their prompts — the orchestrator does NOT emit TaskUpdate for those (would double-close).

Summary of who emits what:

| Planner | Strategy | Batch A | Batch B |
|---|---|---|---|
| 1B Implementation Plan | Subagent | Agent launch (subagent self-tracks FIRST/LAST ACTION) | — |
| 1B Test Plan | Skill-invoked | `TaskCreate` + `Skill(x-test-plan)` | `TaskUpdate` |
| 1C Task Decomposition | Skill-invoked | `TaskCreate` + `Skill(x-lib-task-decomposer)` | `TaskUpdate` |
| 1D Event Schema | Subagent | Agent launch (subagent self-tracks) | — |
| 1E Security (primary) | Skill-invoked | `TaskCreate` + `Skill(x-threat-model)` | `TaskUpdate` on success; pre-fallback close + fallback subagent launch on unavailability |
| 1F Compliance | Subagent | Agent launch (subagent self-tracks) | — |

### 2.3 Per-Planner Subagent Prompts

**1B Implementation Plan (Senior Architect):**

    Agent(
      subagent_type: "general-purpose",
      description: "Plan implementation for story {storyId}",
      prompt: "<Senior Architect prompt below>"
    )

Prompt content:

> **FIRST ACTION:** `TaskCreate(description: "Planning: Implementation Plan — Story {storyId}")` → record as `implPlanTaskId`.
>
> You are a Senior Architect planning feature implementation for {{PROJECT_NAME}}. CONTEXT ISOLATION: you receive only metadata.
>
> **Step 1 — Read context:**
> - Template `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` (RULE-007); on missing, log WARNING and use inline format (RULE-012).
> - Story file `{STORY_PATH}`.
> - `skills/architecture/references/architecture-principles.md` (layer structure, dependency direction).
> - `skills/layer-templates/SKILL.md` (per-layer code templates).
> - Relevant ADRs in `adr/`.
> - Architecture plan at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` (if exists).
>
> **Step 2 — Produce implementation plan** following the template. Inline fallback sections: (1) Affected layers and components, (2) New classes/interfaces, (3) Existing classes to modify, (4) Class diagram (Mermaid), (5) Method signatures, (6) Dependency direction validation, (7) Integration points, (8) DB changes, (9) API changes, (10) Event changes, (11) Configuration changes, (12) TDD strategy — map classes to UT-N/AT-N/IT-N, (13) Mini-ADRs, (14) Risk assessment.
>
> Save to `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
>
> **LAST ACTION:** `TaskUpdate(id: implPlanTaskId, status: "completed")`.

**1D Event Schema (Event Engineer):**

    Agent(
      subagent_type: "general-purpose",
      description: "Design event schemas for story {storyId}",
      prompt: "<Event Engineer prompt>"
    )

Prompt content:

> **FIRST ACTION:** `TaskCreate(description: "Planning: Event Schema — Story {storyId}")` → record as `eventSchemaTaskId`.
>
> You are an Event Engineer. Read `skills/protocols/references/event-driven-conventions.md` and the implementation plan. Produce event schema: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts. Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.
>
> **LAST ACTION:** `TaskUpdate(id: eventSchemaTaskId, status: "completed")`.

**1E Security fallback (Security Engineer):**

The orchestrator closes the skill-level `securityTaskId` explicitly (the TaskCreate already fired) before launching the fallback. The fallback subagent emits its own independent TaskCreate/TaskUpdate pair.

    Agent(
      subagent_type: "general-purpose",
      description: "Security assessment (fallback) for story {storyId}",
      prompt: "<Security Engineer prompt>"
    )

Prompt content:

> **FIRST ACTION:** `TaskCreate(description: "Planning: Security Assessment (fallback) — Story {storyId}")` → record as `securityFallbackTaskId`.
>
> You are a Security Engineer. Read template `.claude/templates/_TEMPLATE-SECURITY-ASSESSMENT.md`, `skills/security/references/application-security.md` (OWASP Top 10 + headers + dependency security), `skills/security/references/security-principles.md` (data classification, input validation, secure error handling), and the implementation plan. Produce threat model, OWASP mapping, authN/authZ review, input validation, data protection, secrets management. Save to `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`.
>
> **LAST ACTION:** `TaskUpdate(id: securityFallbackTaskId, status: "completed")`.

**1F Compliance (Security Engineer):**

Activation: project compliance field is not `"none"`. Skip with log `"Compliance assessment skipped (compliance not active)"` otherwise.

    Agent(
      subagent_type: "general-purpose",
      description: "Compliance assessment for story {storyId}",
      prompt: "<Security Engineer prompt>"
    )

Prompt content:

> **FIRST ACTION:** `TaskCreate(description: "Planning: Compliance Assessment — Story {storyId}")` → record as `complianceTaskId`.
>
> You are a Security Engineer. Read template `.claude/templates/_TEMPLATE-COMPLIANCE-ASSESSMENT.md`, the active compliance reference under `skills/compliance/references/` (the one matching project config: `gdpr.md` / `lgpd.md` / `pci-dss.md` / `hipaa.md` / `sox.md`), `skills/security/references/security-principles.md`, and the implementation plan. Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations. Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.
>
> **LAST ACTION:** `TaskUpdate(id: complianceTaskId, status: "completed")`.

## 3. Phase 1.5 — Parallelism Gate (EPIC-0041 / story-0041-0006)

Invoke `Skill(skill: "x-parallel-eval", args: "--scope=story --story={STORY_ID}")`. The skill consumes per-task/per-story File Footprints and returns a collision matrix classifying interactions as HARD (same file write), REGEN (same golden regen target), or SOFT. On any HARD or REGEN collision between tasks declared parallel, downgrade the affected wave to serial and record `ExecutionState.parallelismDowngrades` with fields `{taskA, taskB, collisionType, hotspot}`. RULE-004 hotspots (`SettingsAssembler.java`, `HooksAssembler.java`, `CLAUDE.md`, `CHANGELOG.md`, `pom.xml`, `.gitignore`, `src/test/resources/golden/**`) are pre-classified. Plans predating EPIC-0041 lack footprint data — warn, do not block (RULE-006).

## 4. Phase 2 — Task Execution Loop (Full)

### 4.1 Approval Gate Detail (EPIC-0042)

**Default behavior:** auto-approve and auto-merge task PRs. Log `"Task PR auto-approved: #{prNumber} — TASK-XXXX-YYYY-NNN (EPIC-0042)"`.

**When `--manual-task-approval`** (deprecated but preserved as no-op; the gate menu is now default). Emit:

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

### 4.2 Auto-Approve Mode (`--auto-approve-pr`, RULE-004)

1. Parent branch `feat/story-XXXX-YYYY-desc` created from `develop` in Phase 0.
2. Each task PR targets the parent branch (NOT `develop`).
3. Task PRs are auto-merged into the parent branch without approval gate.
4. After ALL tasks complete, the parent branch contains all task changes.
5. The parent branch NEVER auto-merges into `develop` or `main` — it requires a human review PR.
6. Phase 3 creates a story-level PR from the parent branch to `develop`.

### 4.3 G1–G7 Fallback (No Test Plan / No Tasks)

If no test plan with TPP markers was produced by Phase 1B and no formal tasks exist, use legacy group-based implementation as a single implicit task:

> Step 2 (Fallback) — Implement groups G1–G7 following the task breakdown:
> - For each group: implement all tasks, then compile: `{{COMPILE_COMMAND}}`.
> - If compilation fails: fix errors before proceeding.
> - After G7: run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`.
> - Coverage targets: line ≥ 95%, branch ≥ 90%.

Emit warning: `"WARNING: No TDD test plan available. Using G1-G7 group-based implementation as single implicit task. Consider running /x-test-plan for future implementations."`

## 5. Phase 3 — Story-Level Verification (Full)

### 5.1 Review Phase Progress Tracker

    TodoWrite(todos: [
      { content: "Run full test suite", status: "in_progress", activeForm: "Running full test suite" },
      { content: "Run coverage analysis", status: "pending", activeForm: "Running coverage analysis" },
      { content: "Run smoke tests", status: "pending", activeForm: "Running smoke tests" },
      { content: "Execute specialist reviews (8 specialists)", status: "pending", activeForm: "Running specialist reviews" },
      { content: "Apply remediation fixes", status: "pending", activeForm: "Applying remediation fixes" },
      { content: "Execute Tech Lead review", status: "pending", activeForm: "Running Tech Lead review" },
      { content: "Update dashboard and remediation files", status: "pending", activeForm: "Updating review artifacts" }
    ])

Update each item to "completed" as the corresponding step finishes. Not created when `--skip-verification` is active.

### 5.2 Step 3.1 — Coverage Consolidation

Run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}` across all story files. Validate thresholds line ≥ 95%, branch ≥ 90%. Emit WARNING with specific files/methods when below thresholds.

### 5.3 Step 3.2 — Cross-File Consistency

Verify uniform error handling patterns across classes of the same role. Verify constructor patterns, return types, and internal type definitions follow the same shape within a module. Inconsistency across files of the same role is a MEDIUM-severity violation. Report findings with file paths and specific inconsistencies.

### 5.4 Step 3.3 — Documentation Update

| Interface | Generator | Output |
|-----------|-----------|--------|
| `rest` | OpenAPI/Swagger generator | `contracts/api/openapi.yaml` |
| `grpc` | gRPC/Proto doc generator | `contracts/api/grpc-reference.md` |
| `cli` | CLI doc generator | `contracts/api/cli-reference.md` |
| `graphql` | GraphQL schema doc generator | `contracts/api/graphql-reference.md` |
| `websocket` / `kafka` / `event-consumer` / `event-producer` | Event doc generator | `contracts/api/event-reference.md` |

No documentable interfaces ⇒ skip with `"No documentable interfaces configured"`. Always generate CHANGELOG entry from Conventional Commits. If `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` exists, invoke `Skill(skill: "x-arch-update", args: "plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md")` to update `steering/service-architecture.md`.

### 5.5 Step 3.4 — Specialist Reviews

    Skill(skill: "x-review", args: "{STORY_ID}")

The review skill launches its own 8 parallel specialist subagents (Security, QA, Performance, Database, Observability, DevOps, API, Event). Instruct each specialist to read `.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md`. On missing template, WARN and fall back to inline format.

Consolidated dashboard (RULE-006): read `.claude/templates/_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`, aggregate specialist scores, save to `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`.

### 5.6 Step 3.5 — Fixes + Remediation

1. Read `.claude/templates/_TEMPLATE-REVIEW-REMEDIATION.md`. Map open findings from dashboard to remediation items. Save to `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`.
2. Fix ALL failed items (every specialist must reach STATUS: Approved).
3. TDD discipline: write/update test FIRST, then apply fix.
4. Atomic commits via `Skill(skill: "x-git-commit", ...)`.
5. Run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}`. Update remediation tracking with commit references.

**Agent-Assisted Remediation (EPIC-0042):** on CRITICAL/HIGH findings, auto-dispatch per-finding agents unless `--no-auto-remediation`:

    Agent(
      subagent_type: "general-purpose",
      description: "Fix review finding FIND-NNN",
      prompt: "Read finding FIND-NNN from plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md. Apply TDD discipline: write/update test FIRST for the finding, then fix implementation. Run {{TEST_COMMAND}} + {{COVERAGE_COMMAND}}. Commit via Skill(skill: 'x-git-commit', args: '--type fix --subject \"fix FIND-NNN: [description]\"'). Update remediation tracking: mark FIND-NNN as Fixed with commit SHA."
    )

After all finding agents complete, re-run test + coverage. Step 3.6 re-validates (max 2 cycles).

### 5.7 Step 3.6 — Tech Lead Review

`Skill(skill: "x-review-pr", args: "{STORY_ID}")` for holistic review. Requires all items passing for GO. If NO-GO, fix all failed items and re-review (max 2 cycles). Update dashboard with Tech Lead findings.

**Step 3.6.5 — PR fix on GO:** invoke `Skill(skill: "x-pr-fix", args: "--pr {prNumber}")` to apply reviewer comments automatically. On compile regression, ABORT with `PR_FIX_COMPILE_REGRESSION`; do NOT proceed to 3.7.

### 5.8 Step 3.7 — Story-Level PR (Auto-Approve Mode Only)

If `--auto-approve-pr` is active:

1. Push parent branch: `git push -u origin feat/story-XXXX-YYYY-desc`.
2. Create story-level PR via `gh pr create --base develop` with title `feat(story-XXXX-YYYY): {story title}`, body = consolidated review summary + task list with PR links + coverage report. This PR requires human review — NEVER auto-merged (RULE-004).

### 5.9 Step 3.8 — Final Verification + Cleanup

1. Update README if needed.
2. Update IMPLEMENTATION-MAP: find the story row, change Status column to `Concluida`, write file.
3. Update story file Status: change `**Status:**` from `Pendente` to `Concluida`. In Section 8, change completed `- [ ]` to `- [x]`. Write file.
4. Jira sync (conditional): if `**Chave Jira:**` is real (not `-` / `<CHAVE-JIRA>`), call `mcp__atlassian__getTransitionsForJiraIssue`, find "Done" transition, call `mcp__atlassian__transitionJiraIssue`. Non-blocking on failure.
5. Update `execution-state.json`: all completed tasks → DONE; `storyStatus → COMPLETE`.
6. **DoD checklist:**
   - [ ] All task PRs approved/merged
   - [ ] Coverage ≥ 95% line, ≥ 90% branch
   - [ ] Zero compiler/linter warnings
   - [ ] Commits show test-first pattern
   - [ ] AT-N GREEN
   - [ ] Tests follow TPP ordering
   - [ ] No test-after commits
   - [ ] Story markdown updated with `Status: Concluida`
   - [ ] IMPLEMENTATION-MAP Status updated
   - [ ] At least 1 automated test validates primary acceptance criterion
   - [ ] Smoke test PASSES (hard gate if `testing.smoke_tests == true`; `--skip-smoke` bypasses)
7. **Conditional DoD:** contract tests pass (if enabled), event schemas registered (if event_driven), compliance met (if active), gateway updated (if `api_gateway != none`), gRPC/GraphQL schema backward compatible, threat model updated if security findings ≥ MEDIUM, post-deploy verification passed.
8. **Post-Deploy Verification — SMOKE GATE (HARD, EPIC-0042):**
   - `testing.smoke_tests == false` ⇒ SKIP with `"Post-deploy verification skipped (testing.smoke_tests=false)"`.
   - `--skip-smoke` present ⇒ SKIP with `"Smoke gate bypassed via --skip-smoke flag"`.
   - Otherwise execute (invoke `/x-test-e2e` or configured smoke):
     - Health Check: GET `/health` → 200 OK.
     - Critical Path: primary request flow → valid response.
     - Response Time: p95 < configured SLO (advisory — WARNING only).
     - Error Rate: < 1% threshold (advisory — WARNING only).
   - Health Check OR Critical Path failure → FAIL Phase 3:
     ```
     SMOKE GATE FAILED (EPIC-0042):
       Health Check: FAIL / PASS
       Critical Path: FAIL / PASS
     Phase 3 is FAILED. Fix the failing smoke tests and re-run.
     Override: pass --skip-smoke to bypass the smoke gate.
     ```
9. Report PASS / FAIL / SKIP with task-level summary.

### 5.10 Step 3.8b — Mode-Aware Worktree Cleanup (Rule 14 §5)

| `STORY_OWNS_WORKTREE` | Phase 3 Verification | Cleanup Action |
| :--- | :--- | :--- |
| `false` | pass or fail | Skip worktree removal. Log `"[CLEANUP] Skipping worktree removal (STORY_OWNS_WORKTREE=false — not the creator, Rule 14 §5)"`. Mode 1 (REUSE) and Mode 3 (LEGACY) fall here. |
| `true` | pass | Remove worktree, then switch back to `mainRepoPath` and sync `develop`. |
| `true` | fail | Preserve worktree for diagnosis (Rule 14 §4). Log `"[PRESERVED] Worktree story-XXXX-YYYY kept due to verification failure"` and emit manual recovery instructions. |

Concrete per-mode actions:

- **Mode 1 (REUSE, `STORY_OWNS_WORKTREE=false`):** do NOT remove. Do NOT run `git checkout develop && git pull origin develop` here (violates Rule 14 §2).
- **Mode 2 (CREATE, `STORY_OWNS_WORKTREE=true`) + DoD passed:**
  1. Remove: `Skill(skill: "x-git-worktree", args: "remove --id story-XXXX-YYYY")`.
  2. Switch context back to `mainRepoPath` and run `git checkout develop && git pull origin develop`.
- **Mode 2 (CREATE) + story FAILED:** preserve for diagnosis (Rule 14 §4). Log the preserved path and instruct the operator to run `Skill(skill: "x-git-worktree", args: "remove --force --id story-XXXX-YYYY")` after triage.
- **Mode 3 (LEGACY, `STORY_OWNS_WORKTREE=false`):** no worktree created; run `git checkout develop && git pull origin develop` in the main checkout.

> **Anti-pattern (Rule 14 §5).** `x-story-implement` MUST NEVER call `/x-git-worktree remove` when `STORY_OWNS_WORKTREE=false`. Removal ownership belongs to the outer orchestrator (Mode 1) or is not applicable (Mode 3).

## 6. Error Classification, Retry, and Reporting

### 6.1 Error Classification

| Category | Detection Patterns (case-insensitive) | Action |
|----------|---------------------------------------|--------|
| **TRANSIENT** | `"overloaded"`, `"rate limit"`, `"429"`, `"503"`, `"504"`, `"timeout"`, `"ETIMEDOUT"`, `"capacity"`, `"502"` | Retry with exponential backoff |
| **CONTEXT** | `"context"`, `"token limit"`, `"too long"`, `"exceeded"`, `"output too large"`, `"truncated"` | Graceful degradation |
| **PERMANENT** | All errors not matching TRANSIENT or CONTEXT | Fail immediately with contextual error |

Default (no pattern match): PERMANENT.

### 6.2 Retry with Exponential Backoff (Tool Calls)

| Retry | Delay |
|-------|-------|
| 1 | 2 seconds |
| 2 | 4 seconds |
| 3 | 8 seconds |
| After 3 failures | Mark task as FAILED |

PERMANENT errors MUST NOT be retried.

### 6.3 SubagentResult Error Reporting

When `x-story-implement` is invoked as a subagent by `x-epic-implement`, errors MUST be reported back via `SubagentResult` JSON fields:

- `errorType` — TRANSIENT | CONTEXT | PERMANENT
- `errorMessage` — human-readable description
- `errorCode` — machine-readable code (e.g., `PR_FIX_COMPILE_REGRESSION`, `WAVE_VERIFICATION_FAILED`)
- `contextPressureDetected` — boolean (set true when context pressure detected locally)

## 7. Graceful Degradation

When invoked by `x-epic-implement`, the lifecycle skill respects context-pressure levels communicated via the subagent prompt. The epic orchestrator manages pressure detection and level advancement (see `x-epic-implement` Section 1.7).

| Level | Lifecycle Behavior |
|-------|--------------------|
| Level 0 (Normal) | Full execution — all phases, full logging, reviews enabled |
| Level 1 (Warning) | Reduce log output to status lines; skip non-essential doc generation; prefer slim-mode skill invocations |
| Level 2 (Critical) | Skip Phase 3 reviews (specialist + tech lead); minimize output in all tool calls; include `"CONTEXT PRESSURE: minimize output"` in delegated prompts |
| Level 3 (Emergency) | Not applicable — epic orchestrator saves state and exits before dispatching at Level 3 |

**Detection within lifecycle:** if tool calls return "output too large" or truncated responses, log `"CONTEXT PRESSURE signal detected in x-story-implement for {storyId}"`, set `contextPressureDetected: true` in SubagentResult, apply Level 1 actions locally, continue execution.

## 8. Template Fallback (RULE-012)

Templates referenced by this skill follow RULE-012. When a template does not exist, degrade with logged warning:

| Template | Fallback |
|----------|----------|
| `_TEMPLATE-IMPLEMENTATION-PLAN.md` | Inline section list used in Step 1B subagent |
| `_TEMPLATE-TEST-PLAN.md` | `x-test-plan` handles its own fallback |
| `_TEMPLATE-TASK-BREAKDOWN.md` | Task decomposer handles its own fallback |
| `_TEMPLATE-SECURITY-ASSESSMENT.md` | Inline format for security assessment |
| `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` | Inline format for compliance assessment |
| `_TEMPLATE-SPECIALIST-REVIEW.md` | Inline format for specialist reports |
| `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md` | Inline format for dashboard |
| `_TEMPLATE-REVIEW-REMEDIATION.md` | Inline format for remediation tracking |
| `_TEMPLATE-TECH-LEAD-REVIEW.md` | Inline format for Tech Lead review |

## 9. Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer (via x-test-tdd) | Phase 2 | Adaptive (per task) |
| Specialist Reviews | Phase 3.4 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 3.6 | Adaptive (story max tier) |

## 10. v2 Extensions (EPIC-0038 — Wave-Based Task Orchestration)

This appendix documents the schema-aware orchestration introduced by story-0038-0006. The legacy monolithic flow (coalesce tasks ad-hoc, run Double-Loop TDD inside `x-story-implement` itself) is preserved for v1. In v2, `x-story-implement` becomes a **wave dispatcher** that reads `task-implementation-map-STORY-*.md` and invokes `x-task-implement` per task, honouring declared parallelism.

### 10.1 Phase 0f — Schema Version Detection

Same `SchemaVersionResolver` as `x-story-plan` (story-0038-0004). When `planningSchemaVersion == "2.0"`, activate the wave dispatcher. Otherwise fall through to the legacy task-centric flow documented above.

### 10.2 Phase 1 (v2) — Read Task Implementation Map

1. Resolve `plans/epic-XXXX/plans/task-implementation-map-STORY-XXXX-YYYY.md`.
2. If missing, abort with `MAP_NOT_FOUND {path}` (the map should already exist from `x-story-plan` v2 Phase 4c; if not, the epic orchestrator or operator should re-run planning).
3. Parse the Execution Order table to recover wave structure (Wave N → list of TASK-IDs). Coalesced super-nodes appear as `(TASK-A, TASK-B)` and map to a single `x-task-implement` invocation (the child skill's Phase 0e COALESCED check handles partner presence).

### 10.3 Phase 2 (v2) — Wave Dispatch Loop

For each wave in topological order:

1. **Dispatch:** for every TASK-ID in the wave, invoke `x-task-implement <TASK-ID>` via the Skill tool. Emit all invocations as sibling tool calls in a SINGLE assistant message (Rule 13 — INLINE-SKILL, parallel dispatch).
2. **Await:** wait for every invocation in the wave to return.
3. **Verify wave:** assert every TASK result has `status == "DONE"` and a valid `commitSha`. Any FAILED task aborts the wave and the story.
4. **Integration verification:** run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}` on the aggregated state of develop + wave commits. Regression at this boundary reports which TASK-ID introduced the failure (last-writer per failing file).
5. **Advance:** move to the next wave only after current wave's integration verification passes.

### 10.4 Phase 3 (v2) — Coalesced Wave Handling

A wave row containing a coalesced super-node `(TASK-A, TASK-B)`:

- Dispatch a SINGLE `x-task-implement` invocation specifying both task IDs in the `--coalesce <ID1,ID2>` flag.
- `x-task-implement` Phase 4 emits one commit with `Coalesces-with: TASK-B` footer (RULE-TF-04). The other task's status is updated transitively — both IDs move to DONE with the same `commitSha`.

### 10.5 Phase 4 (v2) — Story-Level Aggregation

After the final wave:

1. Aggregate per-task results into a story-level commit summary `plans/epic-XXXX/reports/story-implementation-report-STORY-XXXX-YYYY.md`:
   - Table of TASK-ID → commit SHA → wallclock → coverage delta.
   - Wave execution timing.
   - Any rebases / conflict-resolution events.
2. Run the existing story-level verification phases (review, tech lead, story PR creation) exactly as in v1 — the new work is entirely in Phase 2's dispatch loop.

### 10.6 v2 Benefits vs v1

| Aspect | v1 (legacy) | v2 (wave dispatch) |
|--------|-------------|--------------------|
| Task coalescing | Ad-hoc; often silent | Declarative (only COALESCED-with-COALESCED pairs) |
| Parallelism | Serial | Declared per wave; independent tasks run concurrently |
| Review granularity | Story diff (hundreds of lines) | Per-task commit (tens of lines) |
| Bisect-ability | Story-level checkpoint | Per-task atomic commits |
| Failure attribution | Manual diff walk | Automatic (wave verification pinpoints TASK-ID) |
| TDD honesty | Best-effort | Enforced per-task (RED_NOT_OBSERVED aborts) |

### 10.7 Error Codes (v2)

| Code | Condition |
|------|-----------|
| `MAP_NOT_FOUND` | task-implementation-map-STORY-*.md missing for the story |
| `WAVE_VERIFICATION_FAILED` | post-wave compile/test failure; includes offending TASK-ID |
| `COALESCED_PARTNER_MISSING` | wave contains coalesced super-node but one partner isn't dispatchable |
| `CROSS_WAVE_REGRESSION` | earlier wave's work broken by a later wave (rare; aborts story) |

## 11. Rationale

The slim `SKILL.md` intentionally omits the step-by-step phase prose, per-planner subagent prompts, decision tables, template-fallback list, error-retry schedule, and the v2 wave-dispatch appendix. Runtime orchestration needs only the phase skeleton with canonical delegation snippets (those remain in SKILL.md) to execute the lifecycle correctly. This full-protocol file is the diagnostic/authoring reference consulted when:

- a new planner subagent is added (prompts live here);
- an operator investigates a failed phase (decision tables live here);
- the v2 wave dispatcher is activated (appendix lives here);
- an error needs classifying (retry-backoff schedule lives here).

See [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md) for the architectural rationale and [ADR-0011 — Shared Snippets Inclusion Strategy](../../../../../../../../../adr/ADR-0011-shared-snippets-inclusion-strategy.md) for how this composes with `_shared/` snippets.
