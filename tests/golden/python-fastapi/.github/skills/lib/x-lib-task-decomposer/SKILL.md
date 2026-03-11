---
name: x-lib-task-decomposer
description: >
  Decomposes an architect's implementation plan into parallelizable tasks
  by layer. Uses the Layer Task Catalog to assign model tiers, context
  budgets, and parallelism groups. Produces a task breakdown document.
  Reference: `.github/skills/lib/x-lib-task-decomposer/SKILL.md`
---

# Skill: Task Decomposer (Layer-Based)

## Purpose

Decomposes an implementation plan into granular, single-layer tasks using a **fixed Layer Task Catalog**. Each task is assigned a model tier (Junior/Mid/Senior), context budget, and parallelism group based on its architectural layer.

## When to Use

- **Feature Lifecycle Phase 1C**: After the Architect produces the plan, BEFORE implementation
- **Standalone**: When you need to break down a plan into implementable tasks

## Inputs Required

1. `docs/plans/STORY-ID-plan.md` -- Architect's design
2. Story requirements file

## Procedure

### STEP 0 -- Read Architecture Context

Before decomposing, read the project's architecture and layer definitions:
- `.github/skills/architecture/SKILL.md` -- layer structure, dependency direction, package organization
- `.github/skills/layer-templates/SKILL.md` -- complete layer catalog with package locations, code templates, and checklist per layer

### STEP 1 -- Read Story Context

Read these files:
- `docs/plans/STORY-ID-plan.md` (Architect's plan)
- Story requirements file

### STEP 2 -- Identify Affected Layers

For each section in the Architect's plan, check which architectural layers are involved. Mark each layer as active or inactive.

### STEP 3 -- Apply the Layer Task Catalog

For each active layer, create ONE task using the fixed catalog below.

### STEP 4 -- Variable Tier Decision

For complex domain logic tasks, read the Architect's plan carefully:
- **Simple mapping/lookup** (1 decision, no state) -> Mid tier
- **Multi-branch logic** (type hierarchies with 3+ implementations, resilience patterns) -> Senior tier

### STEP 5 -- Generate Output

Save to: `docs/plans/STORY-ID-tasks.md`

## Layer Task Catalog

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

## Layer Dependency Graph

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

- Invoked by `x-dev-lifecycle` during Phase 1C
- Output consumed by Phase 2 (group-based implementation)
- Reference: `.github/skills/lib/x-lib-task-decomposer/SKILL.md`
