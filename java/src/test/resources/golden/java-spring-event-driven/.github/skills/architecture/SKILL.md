---
name: architecture
description: >
  Knowledge Pack: Architecture -- Hexagonal architecture principles, package
  structure, dependency direction rules, layer boundaries, mapper patterns,
  persistence rules, and architecture variants for my-spring-event-driven.
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

- [Hexagonal Architecture — Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/) -- Original ports and adapters pattern
- [Clean Architecture — Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) -- Dependency rule and layer boundaries
- [Domain-Driven Design Reference — Eric Evans](https://www.domainlanguage.com/ddd/reference/) -- DDD building blocks and patterns
