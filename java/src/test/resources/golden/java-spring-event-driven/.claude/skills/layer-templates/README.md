# layer-templates

> Reference code templates for each hexagonal architecture layer. Provides consistent patterns for domain model, ports, DTOs, mappers, entities, repositories, use cases, REST resources, exception mappers, migrations, and configuration.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-dev-implement`, `x-dev-lifecycle`, `x-dev-epic-implement`, `typescript-developer` agent, `architect` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Domain model (records, sealed interfaces, enums)
- Inbound and outbound port interfaces
- Request/Response DTOs with validation
- DTO and entity mappers (static utility classes)
- ORM-managed persistence entities (JPA, MongoDB, Cassandra)
- Repository patterns per database type
- Use case orchestration (application layer)
- REST resource endpoints and exception mappers
- Database migrations and typed configuration

## Key Concepts

This pack provides copy-and-adapt code templates for all 18 components of a hexagonal architecture implementation. Each template enforces strict layer boundaries: domain models have zero framework dependencies, ports use only domain types, mappers are static utility classes (not DI beans), and entities never leak outside the persistence adapter. The included checklist ensures no component is missed when implementing a new aggregate.

## See Also

- [architecture](../architecture/) — Hexagonal architecture principles, dependency rules, and package structure
- [coding-standards](../coding-standards/) — Clean Code rules, SOLID principles, and naming conventions
- [testing](../testing/) — Test patterns per architecture layer
