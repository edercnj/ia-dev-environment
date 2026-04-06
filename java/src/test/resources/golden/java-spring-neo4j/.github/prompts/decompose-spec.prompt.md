---
name: decompose-spec
description: >
  Decomposes a system specification into an Epic, individual Story files,
  and an Implementation Map with dependency graph and phased execution plan.
---

# Decompose Specification

Use this prompt to break down a system specification into implementable work items
for **my-spring-neo4j**.

## Prerequisites

- A specification file (e.g., `specs/my-feature-spec.md`)
- The spec should follow the system specification template format

## Workflow

### Step 1 — Full Decomposition

Use the **x-story-epic-full** skill for complete decomposition:

```
/x-story-epic-full @specs/my-feature-spec.md
```

This produces three deliverables:
1. **Epic** — Scope, cross-cutting rules, quality gates, story index
2. **Stories** — One file per story with data contracts, Gherkin, diagrams, sub-tasks
3. **Implementation Map** — Phases, critical path, dependency graph

### Step 2 — Review Output

Verify the generated artifacts inside `plans/epic-XXXX/`:
- Epic file: `plans/epic-XXXX/epic-XXXX.md`
- Story files: `plans/epic-XXXX/story-XXXX-YYYY.md`
- Implementation map: `plans/epic-XXXX/implementation-map-XXXX.md`

### Step 3 — Validate Dependencies

Check that the dependency DAG has no cycles and the critical path is realistic.

## Agents Involved

- **product-owner** — Story prioritization and acceptance criteria
- **architect** — Technical decomposition and dependency analysis

## Tips

- Provide a well-structured spec for better decomposition quality
- Review generated stories for completeness before starting implementation
- Use the implementation map to plan sprint capacity
