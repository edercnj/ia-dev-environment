---
name: architecture-patterns
model: haiku
description: "Architecture pattern references: microservice, resilience, data, integration, and architectural patterns (saga, outbox, circuit breaker, CQRS, event sourcing, and more)."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Architecture Patterns

## Purpose

Provides detailed reference documentation for architecture patterns. Complements the core architecture knowledge pack with in-depth implementation guidance for microservice, resilience, data access, integration, and architectural patterns.

### Architectural Patterns

- `references/cqrs.md` — Command/query separation, sync strategies, consistency boundaries
- `references/event-sourcing.md` — Append-only event log, snapshots, projections, event versioning
- `references/modular-monolith.md` — Module boundaries, inter-module communication, decomposition

### Microservice Patterns

- `references/api-gateway.md` — Edge routing, aggregation, auth at the edge
- `references/bulkhead.md` — Resource isolation, semaphore vs thread pool, partitioning
- `references/idempotency.md` — Deduplication strategies, exactly-once semantics
- `references/outbox-pattern.md` — Transactional event publishing, relay strategies
- `references/saga-pattern.md` — Orchestration vs choreography, compensation, timeouts
- `references/service-discovery.md` — Registration, health checks, DNS vs registry
- `references/strangler-fig.md` — Incremental migration, routing strategies, parallel running

### Resilience Patterns

- `references/circuit-breaker.md` — States, configuration, fallback strategies, per-dependency circuits
- `references/dead-letter-queue.md` — Poison pill detection, replay, monitoring
- `references/retry-with-backoff.md` — Backoff strategies, jitter, retry budgets
- `references/timeout-patterns.md` — Timeout types, deadline propagation, hierarchy

### Data Patterns

- `references/cache-aside.md` — Cache population, invalidation, TTL, thundering herd prevention
- `references/event-store.md` — Event record structure, snapshots, projections, versioning
- `references/repository-pattern.md` — Interface design, pagination, specification pattern
- `references/unit-of-work.md` — Transaction boundaries, commit/rollback, concurrency control

### Integration Patterns

- `references/anti-corruption-layer.md` — Translation boundary, error handling, testing
- `references/backend-for-frontend.md` — Per-client optimization, aggregation, versioning
- `references/adapter-pattern.md` — Protocol bridging, resilience wrapping, auth handling

## Related Knowledge Packs

| Pack | Relationship |
|------|-------------|
| `architecture` | Core hexagonal architecture principles |
| `resilience` | Resilience principles and patterns |
| `api-design` | API design principles |
| `database-patterns` | Database conventions and principles |
