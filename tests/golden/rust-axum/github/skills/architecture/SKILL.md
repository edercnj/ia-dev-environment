---
name: architecture
description: >
  Knowledge Pack: Architecture -- Hexagonal architecture principles, package
  structure, dependency direction rules, layer boundaries, mapper patterns,
  persistence rules, and architecture variants for my-rust-service.
---

# Knowledge Pack: Architecture

## Summary

This project follows **hexagonal architecture** (ports and adapters) with strict dependency direction: adapters depend inward toward the domain. The domain layer has zero external dependencies.

### Package Structure

- `domain/model/` -- Entities, value objects, enums (pure, no framework imports)
- `domain/engine/` -- Business logic and decision rules
- `domain/port/` -- Inbound and outbound interfaces
- `application/` -- Use case orchestration (calls ports, never adapters)
- `adapter/inbound/` -- REST, gRPC, event handlers, DTOs, mappers
- `adapter/outbound/` -- Database, external clients, entities, mappers
- `config/` -- Framework configuration

### Dependency Rules

| Layer | Can Depend On | Cannot Depend On |
|-------|--------------|-----------------|
| domain | Standard library only | adapter, application, framework |
| application | domain | adapter, framework |
| adapter.inbound | application, domain.port | adapter.outbound |
| adapter.outbound | domain.port, domain.model | adapter.inbound |

### Implementation Order

Inner layers first: domain → ports → application → adapters → inbound → tests.

## References

- `.claude/skills/architecture/SKILL.md` -- Full architecture reference
- `.claude/skills/architecture/references/` -- Detailed documentation
