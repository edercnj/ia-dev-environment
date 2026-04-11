# ddd-strategic

> DDD Strategic Design knowledge pack: bounded context identification, context map with 6 integration patterns, Anti-Corruption Layer template with compilable code, and conditional /x-ddd-context-map skill for Mermaid diagram generation.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-task-implement, x-arch-plan, x-epic-create, x-review, architect agent |
| **Condition** | Included when `architecture.style` is `hexagonal`/`ddd` or `ddd.enabled` is `true` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Bounded context definition, identification criteria, and decomposition heuristics
- Context map with 6 integration patterns: Shared Kernel, Customer/Supplier, Conformist, Anti-Corruption Layer, Open Host Service, Published Language
- Pattern selection decision tree for choosing integration patterns
- Anti-Corruption Layer template with compilable code and hexagonal port integration
- Automated context map generation via /x-ddd-context-map skill

## Key Concepts

This pack provides strategic DDD guidance for identifying bounded contexts and managing their relationships. Bounded contexts are identified through four criteria: distinct ubiquitous language, own data model, independent lifecycle, and team ownership. The context map documents relationships using six integration patterns, with a decision tree guiding selection based on team control, model influence, and schema evolution needs. The Anti-Corruption Layer pattern is fully implemented with a generic interface, concrete translation example, and integration with hexagonal ports in the outbound adapter layer. The /x-ddd-context-map skill auto-discovers contexts from package structure and generates Mermaid diagrams.

## See Also

- [architecture](../architecture/) — Hexagonal architecture principles and package structure
- [architecture-hexagonal](../architecture-hexagonal/) — Port/Adapter patterns and boundary validation
- [architecture-patterns](../architecture-patterns/) — Detailed pattern implementations including ACL
