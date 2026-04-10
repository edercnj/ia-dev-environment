# architecture-patterns

> Architecture pattern references: microservice patterns (saga, outbox, bulkhead, idempotency, service discovery, strangler fig, API gateway), resilience patterns (circuit breaker, retry, timeout, dead letter queue), data patterns (cache-aside, event store, repository, unit of work), and integration patterns (ACL, BFF, adapter).

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-architecture-plan, x-review, x-code-audit, architect agent, tech-lead agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Architectural patterns: CQRS, event sourcing, modular monolith
- Microservice patterns: saga, outbox, bulkhead, idempotency, service discovery, strangler fig, API gateway
- Resilience patterns: circuit breaker, dead letter queue, retry with backoff, timeout patterns
- Data patterns: cache-aside, event store, repository, unit of work
- Integration patterns: anti-corruption layer, backend-for-frontend, adapter pattern

## Key Concepts

This pack provides in-depth implementation guidance for 20+ architecture patterns organized into four categories. Each pattern file covers intent, when to use, when NOT to use, structure, and implementation guidelines. The patterns complement the core hexagonal architecture with specific solutions for microservice integration (saga orchestration vs choreography), resilience (circuit breaker states and fallback strategies), data access (cache population and invalidation), and system integration (anti-corruption layer for domain purity). Pattern selection depends on architecture style, event-driven configuration, and DDD enablement.

## See Also

- [architecture](../architecture/) — Core architecture principles and hexagonal structure
- [architecture-cqrs](../architecture-cqrs/) — CQRS/Event Sourcing detailed implementation
- [database-patterns](../database-patterns/) — Database-specific conventions and optimization
