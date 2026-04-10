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

**If Full or Simplified:** Invoke `x-dev-architecture-plan` via the Skill tool (Rule 10 — INLINE-SKILL pattern):

    Skill(skill: "x-dev-architecture-plan", args: "{STORY_PATH}")

Output: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
If the skill invocation fails: emit WARNING and proceed to Step 1B.

**If Skip:** Log `"Architecture plan not needed for this change scope"` and proceed to Step 1B.

### Step 1B: Implementation Plan (Subagent via Task)

**Skip condition:** If Phase 0 pre-check marked the implementation plan as "Reuse", skip.

Launch a **single** `general-purpose` subagent with `model: opus` (RULE-009):

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

### Fallback: Inline Architecture Planning

If skill `x-dev-architecture-plan` is not available, expand Step 1B subagent prompt to also read protocols, security, observability, and resilience references.

## Phases 1B-1F -- Parallel Planning (Subagents via Task -- SINGLE message)

**CRITICAL: ALL planning subagents (except those skipped) MUST be launched in a SINGLE message.**

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)

**Skip condition:** If Phase 0 pre-check marked the test plan as "Reuse", skip.

Invoke skill `x-test-plan` -> produces `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

The test plan produces: Acceptance tests (AT-N) as outer loop, Unit tests (UT-N) in TPP order as inner loop, Integration tests (IT-N), `Depends On: TASK-N` and `Parallel` markers.

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition

**Skip condition:** If Phase 0 pre-check marked the task breakdown as "Reuse", skip.

Invoke skill `x-lib-task-decomposer` -> produces `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

Auto-detects mode: test-driven tasks (if test plan exists) or G1-G7 layer-based decomposition.

### 1D: Event Schema Design (if event_driven)

Launch `general-purpose` subagent:
> Read `skills/protocols/references/event-driven-conventions.md`.
> Read implementation plan. Produce event schema design: event names, CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `plans/epic-XXXX/plans/events-story-XXXX-YYYY.md`.

### 1E: Security Assessment (MANDATORY)

**Skip condition:** If Phase 0 pre-check marked the security assessment as "Reuse", skip.

Invoke skill `/x-threat-model` via the Skill tool:
  Skill(skill: "x-threat-model", args: "{STORY_PATH}")

Output: `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`

If `/x-threat-model` is unavailable, fall back to `general-purpose` subagent:
> Read template `_TEMPLATE-SECURITY-ASSESSMENT.md` (RULE-007, fallback RULE-012).
> Read `skills/security/SKILL.md` -> then read its references.
> Read implementation plan. Produce security assessment: threat model, OWASP Top 10 mapping, auth review, input validation, data protection, secrets management.
> Save to `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`.

### 1F: Compliance Assessment (CONDITIONAL -- if compliance active)

**Activation:** Only when compliance is not "none". Otherwise skip.

**Skip condition:** If Phase 0 pre-check marked the compliance assessment as "Reuse", skip.

Launch `general-purpose` subagent:
> Read template `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` (RULE-007, fallback RULE-012).
> Read `skills/compliance/SKILL.md` -> then read its references.
> Read implementation plan. Produce compliance impact assessment.
> Save to `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.
