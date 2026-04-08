# architecture

> Full architecture reference: hexagonal/clean architecture principles, package structure, dependency rules, thread-safety, mapper patterns, persistence rules, and architecture variants.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-lifecycle, x-dev-architecture-plan, x-review, x-review-pr, x-codebase-audit, architect agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Hexagonal/clean architecture principles and dependency direction
- Package structure with layer responsibilities
- Dependency rules and layer isolation matrix
- Thread-safety patterns and concurrency considerations
- Mapper patterns between layers (DTOs, entities, domain models)
- Persistence rules and adapter conventions

## Key Concepts

This pack defines the canonical architecture for the project, establishing the inward dependency rule where adapters depend on application which depends on domain, never the reverse. It provides the full dependency matrix specifying which layers can depend on which, enforcing domain purity with zero external library imports. The mapper pattern defines how data transforms between layers (inbound DTOs to domain models, domain models to persistence entities). Architecture variants are documented with their selection criteria.

## See Also

- [architecture-hexagonal](../architecture-hexagonal/) — Hexagonal-specific package structure, Port/Adapter patterns, and ArchUnit validation
- [architecture-patterns](../architecture-patterns/) — Microservice, resilience, data, and integration patterns
- [layer-templates](../layer-templates/) — Code templates per architecture layer
