# story-planning

> Story decomposition and planning: layer-by-layer decomposition (foundation, core domain, extensions, compositions, cross-cutting), story self-containment (data contracts, acceptance criteria), dependency DAG, sizing rules, and phase computation.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-story-create`, `x-story-epic`, `x-story-epic-full`, `x-story-map`, `x-dev-lifecycle`, `product-owner` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Layer decomposition (Layer 0-4: foundation, core domain, extensions, compositions, cross-cutting)
- Story self-containment (data contracts, Gherkin acceptance criteria, sequence diagrams, sub-tasks)
- Dependency DAG construction and circular dependency resolution
- Cross-cutting rules extraction with unique sequential IDs
- Story sizing metrics (endpoints, protocol flows, Gherkin scenarios, sub-tasks)
- Phase computation from DAG (critical path, parallelization, schedule variance)
- Template generation from runtime templates

## Key Concepts

This pack enables systematic decomposition of system specifications into independently implementable stories organized in five layers from foundation infrastructure through cross-cutting quality concerns. Each story must be self-contained with complete data contracts, concrete Gherkin acceptance criteria, and individually estimable sub-tasks. The dependency DAG ensures correct implementation ordering with bidirectional consistency, while automatic phase computation identifies the critical path and parallelization opportunities for optimal delivery scheduling.

## See Also

- [architecture](../architecture/) — Hexagonal architecture principles that inform layer decomposition
- [testing](../testing/) — Test categories and coverage thresholds that stories must satisfy
- [coding-standards](../coding-standards/) — Coding rules applied during story implementation
