---
name: x-lib-task-decomposer
description: >
  Decomposes an implementation plan into tasks. Primary mode: derives tasks
  from test scenarios (x-test-plan output) using TDD structure (RED/GREEN/REFACTOR).
  Fallback mode: uses Layer Task Catalog (G1-G7) when no test plan exists.
  Reference: `.github/skills/lib/x-lib-task-decomposer/SKILL.md`
---

# Skill: Task Decomposer (Test-Driven + Layer Fallback)

## Purpose

Decomposes an implementation plan into granular tasks. When a test plan exists (from `x-test-plan`), derives tasks from test scenarios using TDD structure (RED/GREEN/REFACTOR) with a per-task `Parallel` flag. Falls back to the Layer Task Catalog (G1-G7) when no test plan is available, where tasks are additionally assigned to parallelism groups. Each task is assigned a model tier (Junior/Mid/Senior) and context budget.

## When to Use

- **Feature Lifecycle Phase 1C**: After the Architect produces the plan, BEFORE implementation
- **Standalone**: When you need to break down a plan into implementable tasks

## Inputs Required

1. `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` -- Architect's design
2. Story requirements file
3. `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` -- Test plan (from x-test-plan) [OPTIONAL]

## Procedure

### STEP 0 -- Idempotency Pre-Check (RULE-002)

Before any decomposition work, verify whether a valid task breakdown already exists:

1. **Derive artifact path**: `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID)
2. **Check existence**: Verify if the file exists
3. **Staleness check** (if file exists):
   - Compare modification times: `mtime(story file)` vs `mtime(tasks file)`
   - **Fresh** (`mtime(story) <= mtime(tasks)`): Log `"Reusing existing task breakdown from {date}"` and **RETURN the existing file without regeneration**. No subagent invocation needed.
   - **Stale** (`mtime(story) > mtime(tasks)`): Log `"Regenerating stale task breakdown for {story-id}"` and proceed to STEP 0.5
4. **File absent**: Log `"Generating task breakdown for {story-id}"` and proceed to STEP 0.5

> **Rationale**: Task breakdowns are expensive to generate (2-5 min, significant token cost). When a session is resumed after interruption, the existing breakdown should be reused if the story has not changed. This saves tokens and ensures continuity.

### STEP 0.5 -- Read Architecture Context

Before decomposing, read the project's architecture and layer definitions:
- `.github/skills/architecture/SKILL.md` -- layer structure, dependency direction, package organization
- `.github/skills/layer-templates/SKILL.md` -- complete layer catalog with package locations, code templates, and checklist per layer

### STEP 1 -- Read Story Context and Template (RULE-007, RULE-012)

Read these files:
- `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (Architect's plan)
- Story requirements file

**Template Reference (RULE-007):**

Read template at `.github/templates/_TEMPLATE-TASK-BREAKDOWN.md` for required output format.

- **Template found**: Use the template's sections as the mandatory output structure. The template defines: Header (Story ID, Date, Author), Summary, Dependency Graph, Tasks Table, and Escalation Notes.
- **Template NOT found (RULE-012 fallback)**: Log `"Template not found, using inline format"` and use the inline format defined in STEP 2A (test-driven) or STEP 2B (layer-based) of this skill. Execution continues normally without interruption.

> **Note**: The template contains `{{PLACEHOLDER}}` markers. These are NOT rendered by the engine -- they are filled by the LLM at runtime based on the decomposition results.

### STEP 1.5 -- Detect Decomposition Mode

Check if test plan exists at `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`:

1. **File exists AND has structured TPP scenario markers** (scenario IDs like `UT-01:`, `AT-01:`, `IT-01:` at start of line under a dedicated scenarios section):
   - Use **TEST-DRIVEN MODE** (proceed to STEP 2A)
   - Derive tasks from test scenarios with RED/GREEN/REFACTOR structure

2. **File exists but NO structured TPP markers** (may mention TPP/UT/AT in prose but lacks scenario ID lines):
   - Use **LAYER-BASED MODE** (proceed to STEP 2B)
   - Emit warning: "Test plan found but lacks structured TPP markers. Falling back to layer-based decomposition (G1-G7)."

3. **File absent**:
   - Use **LAYER-BASED MODE** (proceed to STEP 2B)
   - Emit warning: "No test plan found. Falling back to layer-based decomposition (G1-G7)."

### STEP 2A -- Test-Driven Decomposition (Primary Mode)

> Used when a test plan with TPP markers is available at
> `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.

For each test scenario in the test plan (ordered by TPP level):

1. **Map scenario to task**: One task per test scenario (UT/IT/AT)
2. **Identify layer components**: Which layers does this scenario touch?
3. **Determine dependencies**: Which tasks must complete before this one?
4. **Assess parallelism**: Can this task run concurrently with others?
5. **Assign tier**: Based on the highest-complexity layer touched

#### TDD Task Structure

Each generated task MUST contain:

| Field | Required | Description |
|-------|----------|-------------|
| Test Scenario | M | Reference to UT-N, IT-N, or AT-N from test plan |
| TPP Level | M | 1-7 from TPP ordering |
| RED | M | What the test asserts (expected behavior) |
| GREEN | M | Minimum implementation to make test pass |
| REFACTOR | O | Refactoring opportunities after green |
| Layer Components | M | Which layers/classes are touched |
| Parallel | M | yes/no -- can run concurrently with other tasks |
| Depends On | M | List of prerequisite TASK-N references |
| Tier | M | Junior/Mid/Senior based on complexity |
| Budget | M | S/M/L context budget |

#### Parallelism Detection

A task is parallelizable when ALL of:
1. Its test scenario operates on different layers than concurrent tasks
2. No shared state with concurrent tasks
3. No dependency on output of concurrent tasks

#### Ordering Rules

1. Unit test tasks ordered by TPP level (degenerate first, complex last)
2. Within same TPP level, inner layers before outer layers
3. Acceptance test tasks ALWAYS come after ALL related unit test tasks
4. Tasks with dependencies come after their prerequisites

#### Task Type Classification

| Scenario Type | Task Marker | Dependency Rule |
|--------------|-------------|-----------------|
| UT (Unit Test) | `[UT]` | Depends on earlier TPP-level tasks that create shared components |
| AT (Acceptance Test) | `[AT]` | Depends on ALL related UT tasks |
| IT (Integration Test) | `[IT]` | Depends on UT tasks for involved components |

#### TDD Task Output Format

When using test-driven mode, generate tasks in this format:

```
### TASK-N: [UT-X/AT-X] scenario-name
- **TPP Level:** N
- **Type:** UT | AT | IT
- **Tier:** Junior | Mid | Senior
- **Budget:** S | M | L
- **Parallel:** yes | no
- **Depends On:** TASK-N, TASK-M (or "none")

**RED:** [What the test asserts -- expected behavior]

**GREEN:** [Minimum implementation steps]
- Layer: component to create/modify
- Layer: component to create/modify

**REFACTOR:** [Optional refactoring opportunities]

**Layer Components:**
- domain.model: EntityName
- domain.port: PortName
- adapter.outbound: RepositoryName
```

After generating all TDD tasks, proceed to STEP 5 (Generate Output).

### STEP 2B -- Identify Affected Layers (Layer-Based Fallback)

> Used when no test plan with TPP markers is available.

For each section in the Architect's plan, check which architectural layers are involved. Mark each layer as active or inactive.

### STEP 3B -- Apply the Layer Task Catalog (Layer-Based Fallback)

> Used when no test plan with TPP markers is available.

For each active layer, create ONE task using the fixed catalog below.

### STEP 4B -- Variable Tier Decision (Layer-Based Fallback)

> Used when no test plan with TPP markers is available.

For complex domain logic tasks, read the Architect's plan carefully:
- **Simple mapping/lookup** (1 decision, no state) -> Mid tier
- **Multi-branch logic** (type hierarchies with 3+ implementations, resilience patterns) -> Senior tier

### STEP 5 -- Generate Output

Save to: `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p plans/epic-XXXX/plans`.

**Output format selection:**
- If the template was loaded in STEP 1, structure the output following the template's sections (Header, Summary, Dependency Graph, Tasks Table, Escalation Notes)
- If the template was not found (fallback), use the inline TDD Task Output Format (STEP 2A) or Layer Task Catalog format (STEP 2B) as defined in this skill

## Fallback: Layer Task Catalog (G1-G7)

> **When to use:** Only when no test plan with TPP markers exists at
> `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.
> Prefer test-driven decomposition when test plan is available.

| Task Type | Architecture Layer | Tier | Budget | Group |
| --- | --- | --- | --- | --- |
| Database Migration | migration | Junior | S | G1 |
| Domain Models | domain model | Junior | S | G1 |
| Ports (Inbound Interfaces) | domain port inbound | Junior | S | G2 |
| Ports (Outbound Interfaces) | domain port outbound | Junior | S | G2 |
| DTOs (Request/Response) | inbound adapter dto | Junior | S | G2 |
| Domain Engine/Rules (simple) | domain engine | Mid | M | G2 |
| Domain Engine/Rules (complex) | domain engine | Senior | L | G2 |
| Persistence Entity | outbound adapter entity | Junior | S | G3 |
| Entity Mapper | outbound adapter mapper | Junior | S | G3 |
| DTO Mapper (Inbound) | inbound adapter mapper | Junior | S | G3 |
| Repository | outbound adapter repo | Mid | M | G3 |
| Use Case (Application) | application | Mid | M | G4 |
| REST Resource/Controller | inbound adapter rest | Mid | M | G5 |
| Exception Mapper | inbound adapter rest | Mid | M | G5 |
| Protocol Handler | inbound adapter protocol | Senior | L | G5 |
| Configuration | config | Junior | S | G5 |
| Observability (Spans/Metrics) | cross-cutting | Mid | M | G6 |
| Unit Tests | test | Follows tested layer | | G7 |
| Integration Tests | test | Mid | M | G7 |
| API Tests | test | Mid | M | G7 |
| E2E Tests | test | Mid | M | G7 |

## Fallback: Layer Dependency Graph (G1-G7)

> **When to use:** Only when no test plan with TPP markers exists at
> `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`.
> Prefer test-driven decomposition when test plan is available.

```
G1: FOUNDATION (Migration + Domain Models) -- PARALLEL
G2: CONTRACTS (Ports + DTOs + Engine) -- PARALLEL, depends on G1
G3: OUTBOUND ADAPTERS (Entity + Mapper + Repository) -- PARALLEL, depends on G1, G2
G4: ORCHESTRATION (Use Case) -- SEQUENTIAL, depends on G2, G3
G5: INBOUND ADAPTERS (Controllers + Protocol Handlers + Config) -- PARALLEL, depends on G4
G6: OBSERVABILITY -- SEQUENTIAL, depends on G4, G5
G7: TESTS -- PARALLEL (max 4 concurrent), depends on ALL previous
```

## Integration Notes

- Invoked by `x-story-implement` during Phase 1C
- Consumes test plan from `x-test-plan` (Phase 1B output) when available
- Output consumed by Phase 2 (group-based or TDD-based implementation)
- Works with any layered architecture (hexagonal, clean, onion) — layer names derived from project rules
- When test plan present: generates TDD tasks with RED/GREEN/REFACTOR structure
- When test plan absent: generates layer-based tasks using G1-G7 catalog (backward compatible)
- **Idempotency (RULE-002)**: Checks `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` existence and staleness before regenerating. Second invocation with unchanged story reuses existing breakdown
- **Template reference (RULE-007)**: Reads `.github/templates/_TEMPLATE-TASK-BREAKDOWN.md` for standardized output format
- **Graceful fallback (RULE-012)**: Functions without template (pre-EPIC-0024 projects). Logs warning and uses inline format when template absent
- Reference: `.github/skills/lib/x-lib-task-decomposer/SKILL.md`
