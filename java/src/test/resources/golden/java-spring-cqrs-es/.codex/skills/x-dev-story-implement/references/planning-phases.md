# Planning Phases Reference

> **Context:** This reference details Phase 1A-1F planning subagents.
> Part of x-dev-story-implement skill.

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

**Orchestrator tracking (Story 0033-0003, planning subagent visibility):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Architecture Plan — Story {storyId}")

Record the returned integer task ID as `archPlanTaskId` for the closing TaskUpdate.

Invoke `x-dev-architecture-plan` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-dev-architecture-plan", args: "{STORY_PATH}")

After the skill returns (success, failure, or WARNING fallback), close the tracking task:

    TaskUpdate(id: archPlanTaskId, status: "completed")

Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
If the skill invocation fails: emit WARNING, still call `TaskUpdate(id: archPlanTaskId, status: "completed")`, and proceed to Step 1B.

**If Skip:** Log `"Architecture plan not needed for this change scope"` and proceed to Step 1B.

### Step 1B: Implementation Plan (Subagent via Task)

**Skip condition:** If Phase 0 pre-check marked the implementation plan as "Reuse", skip (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

Launch a single `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern). The subagent itself emits the TaskCreate/TaskUpdate for its own tracking task (per Story 0033-0003) and uses `model: opus` per RULE-009.

**Canonical Agent invocation:**

    Agent(
      subagent_type: "general-purpose",
      description: "Plan implementation for story {storyId}",
      prompt: "<prompt content from the Senior Architect quote block below>"
    )

**Prompt content to pass as the `prompt` argument above:**

> **FIRST ACTION (Story 0033-0003):** Create a tracking task to report progress to the parent orchestrator's task list:
>
>     TaskCreate(description: "Planning: Implementation Plan — Story {storyId}")
>
> Record the returned integer ID as `implPlanTaskId` for the LAST ACTION below.
>
> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
>
> **Step 1 -- Read context:**
> - Read template at `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` for required output format (RULE-007). If not found, use inline format (RULE-012).
> - Read story file: `{STORY_PATH}`
> - Read `skills/architecture/references/architecture-principles.md` -- layer structure, dependency direction
> - Read `skills/layer-templates/SKILL.md` -- code templates per architecture layer
> - Read any relevant ADRs in `adr/`
> - If architecture plan exists, read it for architectural decisions and constraints
>
> **Step 2 -- Produce implementation plan** following the template structure with sections:
> 1. Affected layers and components
> 2. New classes/interfaces to create (with package locations)
> 3. Existing classes to modify
> 4. Class diagram (Mermaid classDiagram)
> 5. Method signatures per new class
> 6. Dependency direction validation
> 7. Integration points
> 8. Database/API/Event/Configuration changes
> 9. TDD strategy -- map classes to test plan scenarios
> 10. Architecture decisions -- mini-ADRs
> 11. Risk assessment
>
> Save to `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task created in the FIRST ACTION:
>
>     TaskUpdate(id: implPlanTaskId, status: "completed")

### Fallback: Inline Architecture Planning

If skill `x-dev-architecture-plan` is not available, expand Step 1B subagent prompt to also read protocols, security, observability, and resilience references.

## Phases 1B-1F -- Parallel Planning (Subagents via Task -- SINGLE message)

**CRITICAL: ALL planning subagents (except those skipped) MUST be launched in a SINGLE message.**

**Parallelism + tracking batching (Story 0033-0003):** The per-planner instructions below LOOK sequential (TaskCreate → Skill/Agent → TaskUpdate), but to preserve SINGLE-message parallelism AND per-planner tracking, execute them in this 3-step batched pattern:

1. **Batch A — First assistant message:** all active planners' `TaskCreate` + their `Skill(...)`/`Agent(...)` invocations as sibling tool calls. Record returned task IDs in an in-memory map.
2. **Wait for all planners to return** (runtime handles this).
3. **Batch B — Second assistant message:** all orchestrator-managed `TaskUpdate` calls as sibling tool calls. Subagent-managed planners (1B Impl Plan, 1D Event Schema, 1E fallback, 1F Compliance) close their OWN tracking tasks from inside their prompts — the orchestrator does NOT emit TaskUpdate for those.

Read the per-planner sections below as "what goes into Batch A / Batch B for this planner", NOT as "execute sequentially one planner at a time". See the corresponding section in `x-dev-story-implement/SKILL.md` for the full table of orchestrator-managed vs subagent-managed planners.

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)

**Skip condition:** If Phase 0 pre-check marked the test plan as "Reuse", skip (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Orchestrator tracking (Story 0033-0003):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Test Plan — Story {storyId}")

Record the returned integer task ID as `testPlanTaskId` for the closing TaskUpdate.

Invoke `x-test-plan` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-test-plan", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: testPlanTaskId, status: "completed")

Produces `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.

The test plan produces: Acceptance tests (AT-N) as outer loop, Unit tests (UT-N) in TPP order as inner loop, Integration tests (IT-N), `Depends On: TASK-N` and `Parallel` markers.

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition

**Skip condition:** If Phase 0 pre-check marked the task breakdown as "Reuse", skip (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Orchestrator tracking (Story 0033-0003):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Task Decomposition — Story {storyId}")

Record the returned integer task ID as `taskDecompTaskId` for the closing TaskUpdate.

Invoke `x-lib-task-decomposer` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-lib-task-decomposer", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: taskDecompTaskId, status: "completed")

Produces `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`.

Auto-detects mode: test-driven tasks (if test plan exists) or G1-G7 layer-based decomposition.

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
> Read `skills/protocols/references/event-driven-conventions.md`.
> Read implementation plan. Produce event schema design: event names, CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task:
>
>     TaskUpdate(id: eventSchemaTaskId, status: "completed")

### 1E: Security Assessment (MANDATORY)

**Skip condition:** If Phase 0 pre-check marked the security assessment as "Reuse", skip (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Orchestrator tracking (Story 0033-0003):** Create a tracking task before invoking the skill:

    TaskCreate(description: "Planning: Security Assessment — Story {storyId}")

Record the returned integer task ID as `securityTaskId` for the closing TaskUpdate.

Invoke `x-threat-model` via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-threat-model", args: "{STORY_PATH}")

After the skill returns, close the tracking task:

    TaskUpdate(id: securityTaskId, status: "completed")

Output: `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`

If `x-threat-model` is unavailable, fall back to a `general-purpose` subagent. The orchestrator's `TaskCreate(description: "Planning: Security Assessment — ...")` above **already fired**, so the orchestrator MUST close `securityTaskId` explicitly before launching the fallback (otherwise the original tracking task stays open forever). The fallback subagent then emits its OWN independent TaskCreate/TaskUpdate pair:

**Orchestrator action BEFORE launching the fallback subagent:**

    TaskUpdate(id: securityTaskId, status: "completed")

Then launch the fallback `general-purpose` subagent via the `Agent` tool (Rule 13 — SUBAGENT-GENERAL pattern):

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
> Read template `_TEMPLATE-SECURITY-ASSESSMENT.md` (RULE-007, fallback RULE-012).
> Read `skills/security/SKILL.md` -> then read its references.
> Read implementation plan. Produce security assessment: threat model, OWASP Top 10 mapping, auth review, input validation, data protection, secrets management.
> Save to `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003 fallback):** Close the tracking task:
>
>     TaskUpdate(id: securityFallbackTaskId, status: "completed")

### 1F: Compliance Assessment (CONDITIONAL -- if compliance active)

**Activation:** Only when compliance is not "none". Otherwise skip (do NOT emit TaskCreate for skipped planners, per AC-4 of Story 0033-0003).

**Skip condition:** If Phase 0 pre-check marked the compliance assessment as "Reuse", skip (same rule).

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
> Read template `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` (RULE-007, fallback RULE-012).
> Read `skills/compliance/SKILL.md` -> then read its references.
> Read implementation plan. Produce compliance impact assessment.
> Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.
>
> **LAST ACTION (Story 0033-0003):** Close the tracking task:
>
>     TaskUpdate(id: complianceTaskId, status: "completed")
