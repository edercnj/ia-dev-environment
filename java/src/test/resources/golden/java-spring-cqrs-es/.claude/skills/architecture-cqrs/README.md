# architecture-cqrs

> CQRS/Event Sourcing knowledge pack: write/read model separation, command bus, event store interface, aggregate with event sourcing, projections and rebuild, snapshot policy, dead letter and error handling.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-architecture-plan, x-review, architect agent |
| **Condition** | Included when `architecture.style` is `cqrs` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Write/read model separation with independent pipelines
- Command bus interface and routing implementation
- Event store interface with optimistic concurrency control
- Aggregate with event sourcing (rehydration and uncommitted events)
- Projections, projection handler, and rebuild mechanism
- Snapshot policy with configurable snapshot frequency
- Dead letter queue, retry policy, and idempotent event processing

## Key Concepts

This pack provides the complete CQRS/Event Sourcing lifecycle with compilable code examples. The write side uses a command bus to dispatch commands to handlers that operate on event-sourced aggregates, while the read side consumes domain events through projections optimized for query performance. Optimistic concurrency control via expected version prevents conflicting writes. Snapshots reduce rehydration cost for aggregates with many events, and a dead letter queue with retry policy handles projection failures gracefully. All projections must be idempotent to support safe reprocessing.

## See Also

- [architecture](../architecture/) — Core architecture principles and dependency rules
- [architecture-patterns](../architecture-patterns/) — CQRS and event sourcing pattern references
- [database-patterns](../database-patterns/) — Database conventions and persistence patterns
