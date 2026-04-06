# Command Query Responsibility Segregation (CQRS)

## Intent

CQRS addresses the fundamental tension between optimizing data models for writing (consistency, normalization, validation) and optimizing them for reading (denormalization, aggregation, speed). By separating the command (write) path from the query (read) path into distinct models -- and optionally distinct data stores -- each side can be independently optimized, scaled, and evolved without compromising the other.

## When to Use

- Read and write workloads have significantly different performance characteristics or scaling needs
- The domain model is complex and read-optimized views diverge substantially from the write model
- Multiple read representations of the same data are needed (dashboards, reports, search)
- Most relevant for `architecture.style=microservice` and `architecture.style=modular-monolith`
- Systems where eventual consistency on reads is acceptable
- High-throughput systems where read/write ratios exceed 10:1

## When NOT to Use

- Simple CRUD applications with no meaningful difference between read and write models
- Systems requiring strong consistency on every read immediately after a write
- Small teams without experience managing dual models and synchronization
- `architecture.style=monolith` with a simple domain and low traffic
- When the added complexity of maintaining two models outweighs the scaling benefit

## Structure

```
    ┌─────────────────┐                    ┌─────────────────┐
    │   Command Side   │                    │   Query Side     │
    │                  │                    │                  │
    │  Command DTO     │                    │  Query Request   │
    │       │          │                    │       │          │
    │       ▼          │                    │       ▼          │
    │  Command Handler │                    │  Query Handler   │
    │       │          │                    │       │          │
    │       ▼          │                    │       ▼          │
    │  Domain Model    │                    │  Read Model      │
    │  (Aggregates)    │                    │  (Projections)   │
    │       │          │                    │       │          │
    │       ▼          │    Sync/Async      │       ▼          │
    │  Write Store     │───────────────────►│  Read Store      │
    │  (Normalized)    │   (Events/CDC)     │  (Denormalized)  │
    └─────────────────┘                    └─────────────────┘
```

### Separation Levels

| Level | Write Store | Read Store | Sync Mechanism | Complexity |
|-------|------------|------------|----------------|------------|
| Logical | Same DB, same schema | Same DB, views/materialized views | Database-level | Low |
| Schema | Same DB, separate tables | Same DB, read-optimized tables | Triggers or app-level | Medium |
| Physical | Separate database | Separate database (or search engine) | Events, CDC, or messaging | High |

## Implementation Guidelines

### Command Side Principles

- Commands represent intent: they describe what the user wants to happen, not how
- Each command has exactly one handler; commands are never broadcast
- Command handlers validate, enforce invariants, and persist through the write model
- The write model is normalized, optimized for consistency and integrity
- Command handlers return only success/failure and identifiers -- never full read models
- Commands MUST be idempotent or carry idempotency keys

### Query Side Principles

- Queries are inherently idempotent and side-effect free
- Read models are denormalized, pre-computed, and optimized for specific query patterns
- Multiple read models can exist for the same data (one per consumer need)
- Read handlers MUST NOT modify state
- Query results may be eventually consistent with the write model

### Synchronization Guidelines

| Mechanism | Consistency | Latency | Reliability |
|-----------|------------|---------|-------------|
| Synchronous (same transaction) | Strong | Low | High (but coupled) |
| Domain events (async in-process) | Eventual (milliseconds) | Low | Medium |
| Message broker (async cross-process) | Eventual (seconds) | Medium | High (with outbox) |
| Change Data Capture (CDC) | Eventual (seconds) | Medium | High |

### Consistency Boundaries

- Define explicit SLAs for read model staleness (e.g., "read model reflects writes within 2 seconds")
- Implement version tracking so consumers can detect stale reads
- For operations requiring read-after-write consistency, route reads to the write store
- Design the UI to accommodate eventual consistency (optimistic updates, pending states)

### Model Evolution

- Command and query models evolve independently; a change to the write schema does not mandate an immediate change to every read model
- Projection rebuilding MUST be supported: the ability to reconstruct any read model from the event or change log
- Version read models so consumers can migrate gradually

## Relationship to Other Patterns

- **Event Sourcing**: Frequently paired with CQRS; the event log becomes the write model, and projections become the read models
- **Hexagonal Architecture**: Commands and queries map naturally to distinct inbound ports
- **Repository Pattern**: The write side uses traditional repositories; the read side uses specialized query repositories or direct read-optimized access
- **Saga Pattern**: Long-running business processes coordinate multiple commands across services
- **Outbox Pattern**: Ensures reliable synchronization between the write store and read store via transactional event publishing
- **Event Store**: When combined with event sourcing, the event store replaces the traditional write database


---

# Event Sourcing

## Intent

Event Sourcing replaces traditional state-based persistence with an append-only log of domain events. Instead of storing the current state of an entity, the system stores the complete sequence of state-changing events. Current state is derived by replaying events from the log. This provides a complete audit trail, enables temporal queries, supports rebuilding state at any point in time, and naturally decouples write operations from read projections.

## When to Use

- Systems requiring a complete, immutable audit trail (financial, regulatory, compliance)
- When `architecture.event_driven=true` and the domain naturally expresses state changes as events
- Domains where understanding "how we got here" is as important as "where we are now"
- Systems that benefit from temporal queries (state at any point in time)
- When multiple read models (projections) are needed from the same data
- Complex domains with high concurrency where conflict resolution benefits from event granularity

## When NOT to Use

- Simple CRUD applications where current state is the only concern
- Systems where every read requires strong consistency with the latest write
- Teams without experience in eventual consistency and event-driven thinking
- Domains with high-frequency state changes on the same entity (thousands per second per aggregate)
- When regulatory requirements mandate the ability to delete data (right to be forgotten) without crypto-shredding capability

## Structure

```
    Command ──► Aggregate ──► Events ──► Event Store
                                │              │
                                │              │ (append-only)
                                │              │
                                ▼              ▼
                          Event Bus      Event Log
                              │         ┌──────────┐
                    ┌─────────┼─────┐   │ Event 1  │
                    │         │     │   │ Event 2  │
                    ▼         ▼     ▼   │ Event 3  │
               Projection Projection   │   ...    │
                  A         B     C     │ Event N  │
                  │         │     │     └──────────┘
                  ▼         ▼     ▼          │
              Read DB    Search  Cache   Snapshots
                                        (periodic)
```

### Event Lifecycle

```
  Command Received
        │
        ▼
  Load Aggregate (replay events or load snapshot + subsequent events)
        │
        ▼
  Validate Business Rules
        │
        ▼
  Produce New Event(s)
        │
        ▼
  Append to Event Store (with optimistic concurrency check)
        │
        ▼
  Publish to Event Bus (for projections and downstream consumers)
```

## Implementation Guidelines

### Event Design Principles

| Principle | Guideline |
|-----------|-----------|
| Immutability | Events are facts; once stored, they MUST NEVER be modified or deleted |
| Self-description | Each event carries enough context to be understood independently |
| Granularity | One event per meaningful state change; avoid mega-events |
| Naming | Past tense verbs describing what happened: OrderPlaced, PaymentReceived |
| Versioning | Include a schema version; support upcasting from old versions to new |
| Ordering | Events within a stream are strictly ordered; global ordering is optional |

### Event Store Requirements

| Requirement | Description |
|-------------|-------------|
| Append-only | No updates, no deletes on event records |
| Stream identity | Events belong to a stream (typically per aggregate instance) |
| Optimistic concurrency | Reject appends if expected stream version mismatches |
| Ordering guarantee | Events within a stream maintain insertion order |
| Global position | A monotonically increasing position across all streams (for projections) |
| Retention | Events are retained indefinitely unless crypto-shredded |

### Snapshot Strategy

Snapshots accelerate aggregate loading by capturing state at a point in time. Only events after the snapshot need replaying.

| Aspect | Guideline |
|--------|-----------|
| Frequency | Every N events (e.g., every 100) or time-based |
| Storage | Alongside or separate from the event store |
| Invalidation | Snapshots are disposable; always rebuildable from events |
| Versioning | Snapshot schema version must be tracked; stale snapshots trigger full replay |

### Projection Guidelines

- Projections are disposable read models rebuilt entirely from the event log
- Each projection consumes specific event types and maintains its own read-optimized store
- Projections MUST track their last processed event position for resumability
- Projection rebuilds MUST be idempotent
- Design projections for specific query patterns; multiple projections for the same data are normal

### Event Versioning and Evolution

| Strategy | When to Use |
|----------|-------------|
| Upcasting | Transform old event formats to new on read; original events unchanged |
| Weak schema | Add optional fields with defaults; consumers ignore unknown fields |
| New event type | When the semantic meaning changes fundamentally |
| Copy-and-replace | Last resort: create a new stream with transformed events |

### Concurrency and Conflict Resolution

- Use optimistic concurrency on aggregate stream version for writes
- On conflict, reload the aggregate and retry the command
- For high-contention aggregates, consider functional conflict resolution: merge non-conflicting events automatically

## Relationship to Other Patterns

- **CQRS**: The natural companion; event sourcing provides the write model, projections provide read models
- **Event Store**: The persistence mechanism specifically designed for event sourcing (see `patterns/data/event-store.md`)
- **Saga Pattern**: Sagas coordinate multi-aggregate or multi-service operations using the events produced by event-sourced aggregates
- **Outbox Pattern**: Ensures events are reliably published to external consumers alongside event store persistence
- **Idempotency**: Event handlers and projections MUST be idempotent since events may be replayed during recovery or projection rebuilds


---

# Hexagonal Architecture (Ports & Adapters)

## Intent

Hexagonal Architecture isolates the business domain from all external concerns -- frameworks, databases, messaging systems, and user interfaces -- by establishing strict dependency boundaries through ports (abstractions) and adapters (implementations). This ensures the core business logic remains testable, portable, and independent of infrastructure decisions, allowing teams to defer or replace technology choices without rewriting domain logic.

## When to Use

- Any project where long-term maintainability and testability are priorities
- Systems that must integrate with multiple external protocols (REST, gRPC, TCP, messaging)
- Applications expected to outlive their current infrastructure choices
- Applicable to all `architecture.style` values: monolith, modular-monolith, microservice
- When domain complexity justifies the structural overhead
- Projects where multiple teams contribute to the same codebase

## When NOT to Use

- Throwaway prototypes or proof-of-concept projects with a lifespan under three months
- CRUD-only applications with no meaningful business logic
- Scripts, CLI tools, or single-purpose utilities
- When the entire team is unfamiliar with the pattern and there is no time for onboarding

## Structure

```
                    ┌──────────────────────────────────────────┐
                    │            Inbound Adapters               │
                    │  REST  │  gRPC  │  TCP  │  CLI  │  MQ    │
                    └────┬───┴────┬───┴───┬───┴───┬───┴────┬───┘
                         │        │       │       │        │
                    ═════╪════════╪═══════╪═══════╪════════╪═════
                         │   Inbound Ports (Interfaces)    │
                    ┌────▼────────▼───────▼───────▼────────▼───┐
                    │                                           │
                    │             APPLICATION LAYER             │
                    │         (Use Cases / Orchestration)       │
                    │                                           │
                    ├───────────────────────────────────────────┤
                    │                                           │
                    │              DOMAIN CORE                  │
                    │   Models │ Rules │ Engines │ Value Objs   │
                    │                                           │
                    └────┬────────┬───────┬───────┬────────┬───┘
                         │   Outbound Ports (Interfaces)   │
                    ═════╪════════╪═══════╪═══════╪════════╪═════
                         │        │       │       │        │
                    ┌────▼───┬────▼──┬────▼──┬────▼───┬────▼───┐
                    │   DB   │ Cache │ Queue │ Logger │ ExtAPI │
                    │            Outbound Adapters              │
                    └───────────────────────────────────────────┘
```

## Implementation Guidelines

### The Dependency Rule

All source-level dependencies MUST point inward toward the domain. The domain layer has zero knowledge of the adapter layer. The application layer orchestrates domain operations through ports but never references adapter implementations directly.

| Layer | Permitted Dependencies |
|-------|----------------------|
| Domain (models, rules, engines) | Standard library only; no framework, no infrastructure |
| Domain (ports) | Domain models only |
| Application (use cases) | Domain models, domain ports, domain engines |
| Inbound Adapters | Application layer, domain ports, framework libraries |
| Outbound Adapters | Domain ports, domain models, infrastructure libraries |
| Configuration | Framework configuration, dependency injection wiring |

### Port Design Principles

**Inbound Ports** define what the application offers to the outside world. They are interfaces that use cases implement. Each inbound port represents a distinct capability: processing a command, answering a query, or handling an event.

**Outbound Ports** define what the application needs from the outside world. They are interfaces the domain declares and outbound adapters implement. Each outbound port represents a dependency: persistence, messaging, external service calls, logging.

| Port Aspect | Guideline |
|-------------|-----------|
| Granularity | One port per bounded concern; avoid god-interfaces |
| Naming | Name after the capability, not the technology (PersistencePort, not DatabasePort) |
| Return types | Domain types only; never framework-specific types |
| Exceptions | Domain-defined exceptions only; adapters translate infrastructure exceptions |

### Adapter Design Principles

Adapters translate between the external world and the domain. Each adapter belongs to exactly one port. Adapters are replaceable; swapping an adapter MUST NOT require changes to the domain or application layer.

### The Mapper Pattern

Mappers are the translation mechanism between layers. Two distinct categories exist:

- **DTO Mappers** (inbound side): Convert external request/response formats into domain models and back. Masking, formatting, and protocol-specific concerns live here.
- **Entity Mappers** (outbound side): Convert domain models into persistence entities and back. ORM annotations, column mappings, and storage-specific concerns live here.

Mappers MUST be stateless. They MUST NOT contain business logic. They exist purely for structural translation.

### Thread-Safety Requirements

| Classification | Lifecycle | Constraint |
|---------------|-----------|------------|
| Stateless services | Singleton | No mutable instance fields |
| Request handlers | Request-scoped | Scoped to a single request lifecycle |
| Domain models / DTOs | Immutable | Thread-safe by construction |
| ORM entities | Transaction-scoped | Never shared across threads or transactions |

### Package Structure

Organize packages by architectural layer first, then by concern within each layer. The domain package has zero imports from adapter or framework packages. The adapter package is organized by direction (inbound/outbound) and then by technology.

## Relationship to Other Patterns

- **Summarized in**: `core/05-architecture-principles.md` provides the condensed reference for daily use
- **Modular Monolith**: Each module within a modular monolith applies hexagonal architecture internally, with module boundaries acting as additional isolation
- **CQRS**: Naturally fits within hexagonal architecture -- command and query ports become distinct inbound ports
- **Repository Pattern**: Repositories are the canonical outbound adapter for persistence, implementing outbound ports defined in the domain
- **Anti-Corruption Layer**: When integrating with external systems, the outbound adapter acts as an anti-corruption layer, preventing external models from leaking into the domain
- **Adapter Pattern**: Every outbound adapter is fundamentally an application of the adapter pattern, translating between domain interfaces and external protocols


---

# Modular Monolith

## Intent

The Modular Monolith structures a single deployable unit into well-defined, loosely coupled modules with explicit boundaries, enforced dependency rules, and clear internal APIs. It captures the architectural discipline of microservices -- bounded contexts, independent data ownership, explicit contracts -- without incurring the operational complexity of distributed systems. It also provides a natural migration path toward microservices when scaling demands justify the transition.

## When to Use

- `architecture.style=modular-monolith` in the project configuration
- Teams that need strong module boundaries but are not ready for distributed system overhead
- Projects in early stages where the domain boundaries are still being discovered
- Organizations that want a clear migration path to microservices in the future
- Systems where deployment simplicity and transactional consistency are priorities
- Teams smaller than five engineers, where operating multiple services is impractical

## When NOT to Use

- When independent scaling of components is an immediate, proven need
- When different modules require fundamentally different technology stacks
- When organizational structure already maps to independent service teams
- Trivial applications with no meaningful module boundaries
- When the team has strong microservice experience and infrastructure is already in place

## Structure

```
    ┌───────────────────── Single Deployable Unit ─────────────────────┐
    │                                                                   │
    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
    │  │  Module A    │  │  Module B    │  │  Module C    │             │
    │  │             │  │             │  │             │              │
    │  │ Public API   │  │ Public API   │  │ Public API   │             │
    │  │ (Interface)  │◄─┤ (Interface)  │◄─┤ (Interface)  │             │
    │  │             │  │             │  │             │              │
    │  │ Domain       │  │ Domain       │  │ Domain       │             │
    │  │ Application  │  │ Application  │  │ Application  │             │
    │  │ Adapters     │  │ Adapters     │  │ Adapters     │             │
    │  │             │  │             │  │             │              │
    │  │ [Schema A]   │  │ [Schema B]   │  │ [Schema C]   │             │
    │  └─────────────┘  └─────────────┘  └─────────────┘              │
    │         │                │                │                       │
    │         ▼                ▼                ▼                       │
    │  ┌──────────────────────────────────────────────┐                │
    │  │          Shared Kernel (minimal)              │                │
    │  │  Common types, cross-cutting concerns          │                │
    │  └──────────────────────────────────────────────┘                │
    │                                                                   │
    └───────────────────────────────────────────────────────────────────┘
```

## Implementation Guidelines

### Module Boundary Rules

| Rule | Constraint |
|------|-----------|
| Data ownership | Each module owns its database schema; no cross-module table access |
| API surface | Modules communicate ONLY through their public API interfaces |
| Direct references | Module internals (domain models, entities, repositories) are NEVER exposed |
| Package visibility | Use language-level access control to enforce boundaries (module systems, package-private) |
| Dependency direction | Modules may depend on the Shared Kernel; modules MUST NOT have circular dependencies |

### Module Internal Structure

Each module follows hexagonal architecture internally:

```
module-{name}/
├── api/                  # Public API (interfaces + DTOs exposed to other modules)
│   ├── {Module}Service   # Interface other modules call
│   └── dto/              # Shared DTOs for inter-module communication
├── domain/               # Domain core (private to module)
│   ├── model/
│   ├── port/
│   └── engine/
├── application/          # Use cases (private to module)
├── adapter/              # Infrastructure (private to module)
│   ├── inbound/
│   └── outbound/
└── config/               # Module-specific configuration
```

### Inter-Module Communication

| Mechanism | Consistency | Coupling | Use When |
|-----------|------------|---------|----------|
| Direct method call via public API | Strong (same transaction) | Medium | Operations requiring immediate consistency |
| In-process domain events | Eventual (async listener) | Low | Notifications, side effects, projections |
| Shared message bus (in-memory) | Eventual | Very low | Decoupled workflows, future microservice migration |

**Rules:**
- Prefer domain events over direct calls for cross-module side effects
- Direct calls are acceptable for queries and operations requiring transactional consistency
- NEVER pass internal domain models across module boundaries; use DTOs from the public API

### Shared Kernel Guidelines

The Shared Kernel contains ONLY code that genuinely belongs to multiple modules:

| Permitted | Forbidden |
|-----------|-----------|
| Common value types (Money, Email, Address) | Business logic |
| Cross-cutting concerns (logging, metrics interfaces) | Domain entities |
| Shared event base types | Module-specific DTOs |
| Common exception types | Repository interfaces |

**Rule:** The Shared Kernel MUST be minimal. If only two modules use a type, consider duplication over sharing. Shared Kernel changes affect all modules.

### Data Isolation

- Each module has its own database schema (same database, separate schemas)
- Cross-module data access goes through the owning module's public API, NEVER through direct SQL
- Each module manages its own migrations independently
- Foreign keys across module schemas are FORBIDDEN; use soft references (IDs only)

### Migration Path to Microservices

| Step | Action | Validation |
|------|--------|------------|
| 1. Identify candidate | High traffic module, independent scaling need | Metrics confirm bottleneck |
| 2. Verify boundaries | Module has no shared-transaction dependencies | All cross-module calls are async or API-based |
| 3. Extract data store | Move module schema to separate database | Module still functions with remote DB |
| 4. Replace in-process calls | Swap direct calls for network calls (HTTP/gRPC) | All integration tests pass |
| 5. Deploy independently | Module becomes a standalone service | Monitor latency, error rates |

**Rule:** A module is ready for extraction when it communicates with other modules exclusively through its public API and domain events, with no shared transactions.

## Relationship to Other Patterns

- **Hexagonal Architecture**: Each module applies hexagonal architecture internally; the module boundary adds another layer of isolation
- **CQRS**: Modules can independently adopt CQRS for their internal read/write separation
- **Event Sourcing**: Individual modules may use event sourcing while others use traditional persistence
- **Anti-Corruption Layer**: Module public APIs act as anti-corruption layers, preventing internal model leakage
- **Strangler Fig**: When migrating a legacy monolith, the modular monolith is often the intermediate step before full microservice extraction


---

# Cache-Aside Pattern

## Intent

The Cache-Aside pattern (also called Lazy Loading) places the application in control of cache population and invalidation. The application checks the cache before querying the database; on a cache miss, it loads from the database, populates the cache, and returns the result. On writes, the application updates the database and invalidates or updates the cache. This explicit control avoids the complexity of transparent caching while giving the application full authority over cache behavior, TTLs, and invalidation strategies.

## When to Use

- Read-heavy workloads where the same data is requested frequently
- When eventual consistency between cache and database is acceptable
- Applications where cache misses should transparently fall back to the database
- Systems with predictable hot data that benefits from caching
- Reference: `databases/cache/common/cache-principles.md` provides the cache strategy foundation

## When NOT to Use

- Write-heavy workloads where cached data would be invalidated faster than it is read
- When strong consistency between cache and database is required on every read
- Data that changes too frequently for any TTL to be useful (sub-second change frequency)
- When the working set is too large to fit in the cache (low hit ratio, wasted resources)
- Single-use data that is unlikely to be requested again within the TTL window

## Structure

```
    READ PATH (Cache Hit):
    Client ──► Application ──► Cache ──► Return cached data
                                  │
                              [HIT] ✓

    READ PATH (Cache Miss):
    Client ──► Application ──► Cache ──► [MISS]
                   │                        │
                   │               ┌────────┘
                   ▼               │
               Database ◄─────────┘
                   │
                   ├──► Return data to Application
                   │
                   └──► Application writes to Cache (populate)
                              │
                              ▼
                        Return data to Client

    WRITE PATH:
    Client ──► Application ──► Database (write)
                   │
                   └──► Cache (invalidate or update)
```

## Implementation Guidelines

### Cache Population Strategy

| Strategy | Mechanism | Consistency | Complexity |
|----------|-----------|-------------|------------|
| Cache-Aside (lazy) | Populate on miss; invalidate on write | Eventual | Low |
| Read-Through | Cache auto-loads from DB on miss | Eventual | Medium (requires cache-DB integration) |
| Write-Through | Write to cache and DB synchronously | Strong | Medium |
| Write-Behind | Write to cache; async flush to DB | Eventual (risk of data loss) | High |
| Refresh-Ahead | Proactively refresh before TTL expires | Eventual (fresher) | Medium |

**Default choice:** Cache-Aside. It is the simplest, most widely applicable, and gives the application full control.

### Cache Invalidation Strategies

| Strategy | Mechanism | When to Use |
|----------|-----------|-------------|
| TTL expiration | Cached entry expires after a fixed duration | Default for all cached data |
| Explicit invalidation | Application deletes cache key on write | When writes are infrequent and cache must reflect changes quickly |
| Event-driven invalidation | Listen for domain events and invalidate affected keys | Multi-instance deployments where local cache must sync |
| Version-based | Include a version in the cache key; increment on write | When partial invalidation is complex |

**Rule:** Every cache entry MUST have a TTL, even if explicit invalidation is also used. TTL is the safety net against stale data when invalidation fails.

### TTL Guidelines

| Data Type | TTL Range | Rationale |
|-----------|-----------|-----------|
| Session data | 30 min - 24h | Security and memory |
| API response cache | 1-5 min | Freshness for API consumers |
| User profiles | 15-60 min | Low change frequency |
| Configuration | 5-15 min | Infrequent updates |
| Rate limit counters | 1-60 sec | Rolling window accuracy |
| Static reference data | 1-24h | Rarely changes |
| Computed aggregates | 5-60 min | Expensive to recompute |

### Thundering Herd Prevention

When a popular cache key expires, many concurrent requests simultaneously miss and hit the database.

| Strategy | Mechanism | Trade-off |
|----------|-----------|-----------|
| Mutex/lock | First requester acquires a lock; others wait | Simple; adds latency for waiters |
| Stale-while-revalidate | Serve expired data while refreshing in background | Complex; requires background refresh capability |
| Probabilistic early expiration | Randomly refresh before TTL (beta * ln(random) approach) | Distributed; no coordination needed |
| Pre-warming | Populate cache before expected traffic | Requires knowledge of traffic patterns |

### Write-Path Consistency

| Approach | Operation Order | Risk |
|----------|----------------|------|
| Invalidate cache, then write DB | Cache miss between invalidate and DB commit | Brief window of DB query (low risk) |
| Write DB, then invalidate cache | Stale data served between DB commit and invalidation | Brief staleness window (usually acceptable) |
| Write DB, then update cache | Cache updated with latest data | Concurrent writes can leave cache with older value |

**Guideline:** Prefer "write DB, then invalidate cache." It is the simplest and safest for most applications. The staleness window is typically milliseconds. Avoid updating the cache with the new value on write, as concurrent writes can race.

### Cache Key Design

| Principle | Guideline |
|-----------|-----------|
| Namespace | Prefix with service and entity: `{service}:{entity}:{id}` |
| Determinism | Same input always produces the same key |
| Uniqueness | Include all parameters that affect the cached value |
| Size | Keep keys under 128 bytes |
| Versioning | Include a version prefix if the cached data format changes |

### Monitoring Requirements

| Metric | Type | Alert Threshold |
|--------|------|----------------|
| Hit ratio | Gauge | Below 80% (investigate cache sizing or TTL) |
| Miss count | Counter | Sudden spike indicates invalidation storm or new traffic pattern |
| Latency P99 | Histogram | Above 10ms (cache should be fast) |
| Evictions | Counter | Sustained high rate indicates undersized cache |
| Memory usage | Gauge | Above 80% of allocated maximum |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| No TTL | Memory leak; stale data indefinitely | Every entry MUST have a TTL |
| Cache as primary store | Data loss on cache restart | Database is always source of truth |
| Caching sensitive data | Security risk on cache compromise | Never cache PII, credentials, secrets |
| Large cached values (> 1 MB) | Latency, fragmentation | Split or compress large values |
| Hot key (single key, extreme traffic) | Single-node bottleneck | Replicate with randomized suffixes |
| Unbounded cache | Out of memory | Set max memory and eviction policy |

## Relationship to Other Patterns

- **Reference**: `databases/cache/common/cache-principles.md` defines cache strategies, key naming, TTL, and serialization guidelines
- **Repository Pattern**: The repository implementation may incorporate cache-aside internally, transparent to the domain
- **CQRS**: Read-side projections often use caching; cache-aside is the natural pattern for query-side caching
- **Circuit Breaker**: If the cache is unavailable, the application falls through to the database; a circuit breaker can protect against a failing cache layer
- **Bulkhead**: Separate cache connection pools from database connection pools to prevent cache failures from exhausting DB connections


---

# Event Store

## Intent

The Event Store is a specialized persistence mechanism designed for event-sourced systems. It stores an ordered, immutable sequence of domain events that represent every state change in the system. Unlike traditional databases that store current state, the event store captures the complete history of state transitions, enabling state reconstruction at any point in time, complete audit trails, and multiple read model projections from a single source of truth.

## When to Use

- Systems implementing event sourcing where events are the primary persistence model
- When a complete, immutable audit trail of all state changes is a business requirement
- Applications that need temporal queries (state at any point in time)
- Systems that build multiple read models (projections) from the same event stream
- Domains where regulatory compliance requires provable, tamper-evident history

## When NOT to Use

- Applications using traditional CRUD persistence where current state is sufficient
- Systems that do not benefit from historical state reconstruction
- When the team lacks experience with event sourcing and event-driven architecture
- Simple applications where the overhead of event storage and projection management is unjustified
- Systems with extreme write throughput (millions of events per second per aggregate) without partitioning capability

## Structure

```
    ┌─────────────────────────────────────────────────────────┐
    │                      Event Store                         │
    │                                                          │
    │  Stream: Order-12345                                     │
    │  ┌──────┬──────────────────┬─────────┬────────┬───────┐ │
    │  │ Pos  │ Event Type       │ Payload │ Version│ Time  │ │
    │  ├──────┼──────────────────┼─────────┼────────┼───────┤ │
    │  │  1   │ OrderCreated     │ {...}   │   1    │ T1    │ │
    │  │  2   │ ItemAdded        │ {...}   │   2    │ T2    │ │
    │  │  3   │ ItemAdded        │ {...}   │   3    │ T3    │ │
    │  │  4   │ PaymentReceived  │ {...}   │   4    │ T4    │ │
    │  │  5   │ OrderShipped     │ {...}   │   5    │ T5    │ │
    │  └──────┴──────────────────┴─────────┴────────┴───────┘ │
    │                                                          │
    │  Snapshot: Order-12345 (at version 100)                  │
    │  ┌──────────────────────────────────────────────┐       │
    │  │ Aggregate state as of event 100              │       │
    │  │ (only events 101+ need replaying)            │       │
    │  └──────────────────────────────────────────────┘       │
    │                                                          │
    │  Global Position Index (for projections):                │
    │  ┌──────┬──────────┬──────────────────┬────────┐        │
    │  │ GPos │ Stream   │ Event Type       │ Time   │        │
    │  ├──────┼──────────┼──────────────────┼────────┤        │
    │  │  1   │ Order-1  │ OrderCreated     │ T1     │        │
    │  │  2   │ User-5   │ UserRegistered   │ T2     │        │
    │  │  3   │ Order-1  │ ItemAdded        │ T3     │        │
    │  │  ...                                        │        │
    │  └─────────────────────────────────────────────┘        │
    └─────────────────────────────────────────────────────────┘
```

## Implementation Guidelines

### Event Record Structure

| Field | Purpose | Constraint |
|-------|---------|-----------|
| stream_id | Identifies the aggregate instance (e.g., Order-12345) | NOT NULL, indexed |
| stream_position | Ordinal position within the stream | NOT NULL, sequential per stream |
| global_position | Monotonically increasing position across all streams | NOT NULL, unique, sequential |
| event_type | Fully qualified event type name | NOT NULL |
| payload | Serialized event data (JSON or binary) | NOT NULL |
| metadata | Correlation ID, causation ID, user context | NOT NULL |
| schema_version | Version of the event payload schema | NOT NULL |
| created_at | Timestamp of event creation | NOT NULL |

### Storage Requirements

| Requirement | Description |
|-------------|-------------|
| Append-only | Events are NEVER updated or deleted once written |
| Stream ordering | Events within a stream maintain strict insertion order |
| Global ordering | A global position enables ordered consumption across all streams |
| Optimistic concurrency | Appending checks expected stream version; rejects on mismatch |
| Durability | Committed events MUST survive process and hardware failures |
| Immutability | No UPDATE or DELETE operations on event records (enforced at application and/or DB level) |

### Snapshot Strategy

Snapshots accelerate aggregate loading by capturing materialized state at a point in time.

| Aspect | Guideline |
|--------|-----------|
| Trigger | Create a snapshot every N events (e.g., every 50-200) |
| Storage | Store snapshots in a separate table or alongside event streams |
| Loading | Load snapshot + replay only events after the snapshot's version |
| Rebuilding | Snapshots are disposable; always rebuildable from the event stream |
| Versioning | Track snapshot schema version; stale versions trigger full replay |
| Cleanup | Keep only the latest snapshot per stream; purge older ones |

### Projection Design

| Principle | Guideline |
|-----------|-----------|
| Disposability | Projections can be deleted and rebuilt entirely from the event store |
| Position tracking | Each projection tracks the last global position it has processed |
| Idempotency | Processing the same event twice MUST produce the same result |
| Isolation | Each projection runs independently; one projection failure does not affect others |
| Multiple projections | Different read models can consume the same events for different purposes |
| Rebuild support | The system MUST support rebuilding any projection from position zero |

### Event Versioning and Evolution

| Strategy | Mechanism | When to Use |
|----------|-----------|-------------|
| Upcasting | Transform old event format to new format on read | Non-breaking changes; adding optional fields |
| Weak schema | Add fields with defaults; ignore unknown fields | Forward-compatible changes |
| New event type | Introduce a new event alongside the old one | Fundamentally different semantics |
| Stream migration | Create a new stream with transformed events | Last resort; expensive and complex |

**Rule:** NEVER modify stored events. All evolution strategies work at the read or projection layer, leaving the stored events immutable.

### Performance Considerations

| Concern | Mitigation |
|---------|------------|
| Long event streams | Snapshots every N events; limit replay to snapshot + tail |
| High write throughput | Batch appends; partition streams across storage nodes |
| Projection lag | Monitor lag metrics; scale projection consumers |
| Storage growth | Archive old streams to cold storage; keep event store lean |
| Query by event type | Maintain indexes on event_type and global_position |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Updating events | Destroys audit trail and breaks projections | Events are immutable; use new events or upcasting |
| Large events | Slow reads, high storage cost | Keep events focused; reference external data by ID |
| Events without metadata | Cannot correlate or trace | Always include correlation ID, causation ID, timestamp |
| No global position | Cannot build cross-stream projections | Include a monotonically increasing global position |
| Snapshot as source of truth | Snapshots can be stale or corrupt | Events are the source of truth; snapshots are optimization |
| Unbounded stream replay | Startup takes minutes or hours | Implement snapshots; monitor stream lengths |

## Relationship to Other Patterns

- **Event Sourcing**: The event store is the persistence layer for event sourcing; see `patterns/architectural/event-sourcing.md` for the complete pattern
- **CQRS**: The event store is the write store; projections built from events populate the read stores
- **Outbox Pattern**: In some designs, the event store itself serves as the outbox, with a relay process publishing events to external consumers
- **Saga Pattern**: Saga state changes can be stored as events in the event store, providing a complete history of saga execution
- **Repository Pattern**: In event-sourced systems, the repository loads aggregates by reading and replaying events from the event store, rather than querying a relational table


---

# Repository Pattern

## Intent

The Repository pattern provides a collection-like interface for accessing domain objects, encapsulating the logic required to retrieve, persist, and query data from the underlying storage mechanism. It mediates between the domain layer and the data mapping layer, enabling the domain to work with in-memory collections of objects without knowledge of how those objects are stored. This keeps the domain clean, the data access testable, and the storage technology replaceable.

## When to Use

- Any application following hexagonal or clean architecture where data access must be abstracted
- When domain logic must remain independent of the persistence technology (SQL, NoSQL, file, API)
- Systems where data access patterns need to be testable with in-memory implementations
- When multiple data sources exist and the domain should not know which source is used
- Reference: `core/11-database-principles.md` establishes the repository as the standard data access pattern

## When NOT to Use

- Trivial CRUD applications where the ORM's built-in data access is sufficient
- One-off scripts or batch jobs where a direct query is simpler and maintainability is not a concern
- When the application has no domain layer (pure data pipeline or ETL)
- Read-heavy analytics workloads where repositories add unnecessary abstraction over direct queries

## Structure

```
    Domain Layer                        Adapter Layer
    ┌─────────────────────┐             ┌─────────────────────────┐
    │                     │             │                         │
    │  Domain Model       │             │  ORM Entity             │
    │  (Immutable)        │             │  (Mutable, annotated)   │
    │                     │             │                         │
    │  Repository Port    │             │  Repository Impl        │
    │  (Interface)        │◄────────────┤  (Implements Port)      │
    │                     │             │                         │
    │                     │             │  Entity Mapper          │
    │                     │             │  (Entity <-> Domain)    │
    │                     │             │                         │
    └─────────────────────┘             └─────────────────────────┘

    Call Flow:
    Use Case ──► Repository Port ──► Repository Impl ──► ORM/DB
                 (domain layer)       (adapter layer)
                                            │
                                            ▼
                                      Entity Mapper
                                      (Entity ──► Domain Model)
```

## Implementation Guidelines

### Repository Interface Design

| Principle | Guideline |
|-----------|-----------|
| Location | Repository interfaces (ports) live in the domain layer |
| Parameters | Accept and return domain models only; never ORM entities or DTOs |
| Naming | Name after the aggregate root: OrderRepository, not OrderDAO |
| Granularity | One repository per aggregate root; not per table |
| Return types | Use Optional for single-result queries; never return null |
| Collections | Return immutable collections from query methods |

### Standard Query Methods

| Method Category | Purpose | Naming Convention |
|----------------|---------|-------------------|
| Find by ID | Retrieve single entity by primary key | findById(id) -> Optional |
| Find by criteria | Retrieve entities matching conditions | findByStatus(status) -> List |
| Exists check | Check if entity exists without loading | existsById(id) -> boolean |
| Count | Count entities matching criteria | countByStatus(status) -> long |
| Save | Persist a new or updated entity | save(entity) -> Entity |
| Delete | Remove an entity | deleteById(id) -> void |

### Pagination Guidelines

| Aspect | Guideline |
|--------|-----------|
| Default page size | Define a reasonable default (20-50); never return unbounded results |
| Maximum page size | Enforce a ceiling (100-500); reject requests exceeding it |
| Cursor vs offset | Prefer cursor-based pagination for large datasets; offset-based for small, stable datasets |
| Sort stability | Always include a unique field (ID) in the sort to ensure stable ordering across pages |
| Return metadata | Include total count (if feasible), page size, current page/cursor, has-next indicator |

### Specification Pattern (Advanced Queries)

For complex, composable queries, the Specification pattern separates query criteria from the repository:

| Aspect | Guideline |
|--------|-----------|
| Purpose | Encapsulate query criteria as first-class objects that can be combined |
| Composition | Support AND, OR, NOT composition of specifications |
| Location | Specifications live in the domain layer (they express domain-level criteria) |
| Translation | The repository implementation translates specifications into storage-native queries |
| Use when | Query criteria are dynamic, user-driven, or reused across multiple contexts |

### Implementation Layer Guidelines

| Principle | Detail |
|-----------|--------|
| Entity mapping | Convert between domain models and ORM entities at the repository boundary |
| Transaction awareness | Repositories participate in transactions but do not manage them (Unit of Work manages transactions) |
| Query optimization | Implement efficient queries (proper indexes, fetch strategies) inside the repository implementation |
| Caching | Repository implementations may cache results transparently; the domain is unaware of caching |
| Exception translation | Convert storage-specific exceptions into domain-defined exceptions |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Leaking ORM entities | Domain depends on persistence framework | Map to domain models at the boundary |
| God repository | One repository with methods for every query pattern | One repository per aggregate; use specifications for complex queries |
| Business logic in repository | Validation or rules inside query methods | Keep business logic in domain services |
| Returning mutable collections | Callers can modify the collection | Return unmodifiable or copied collections |
| SELECT * in queries | Fetches unnecessary data; performance waste | Select only required columns |
| No pagination on list queries | Unbounded result sets crash the application | Always paginate list queries |

## Relationship to Other Patterns

- **Reference**: `core/11-database-principles.md` establishes the repository as the standard data access abstraction
- **Hexagonal Architecture**: The repository interface is an outbound port; the implementation is an outbound adapter
- **Unit of Work**: Manages transaction boundaries that span multiple repository operations
- **CQRS**: Write-side repositories handle command persistence; read-side may use specialized query repositories or direct projections
- **Cache-Aside**: The repository implementation may incorporate cache-aside logic transparently
- **Specification Pattern**: Composes complex query criteria as domain objects that repositories can translate to storage queries


---

# Unit of Work

## Intent

The Unit of Work pattern maintains a list of objects affected by a business transaction and coordinates the writing of changes to the database in a single atomic operation. It tracks which objects have been created, modified, or deleted during a business operation and ensures all changes are committed together or rolled back together, preventing partial updates that would leave the system in an inconsistent state.

## When to Use

- Business operations that modify multiple entities or aggregates within the same bounded context
- When multiple repository operations must succeed or fail as a single atomic unit
- Applications using ORMs where transaction management should be explicit and controlled
- Use cases that span multiple domain operations but require a single commit point
- When the application layer needs to orchestrate multiple repository calls within one transaction

## When NOT to Use

- Single-entity operations where the repository's save method is sufficient
- Event-sourced systems where each event is appended independently (event store handles consistency)
- Microservice architectures where transactions span multiple services (use Sagas instead)
- Read-only operations that do not modify state
- When the ORM's built-in session/context already provides adequate unit of work behavior and explicit control is unnecessary

## Structure

```
    ┌─────────────────────────────────────────────────────┐
    │                    Use Case                          │
    │                                                      │
    │   1. Begin Unit of Work                              │
    │   2. Repository A: create entity                     │
    │   3. Repository B: update entity                     │
    │   4. Repository C: delete entity                     │
    │   5. Commit Unit of Work                             │
    │                                                      │
    └──────────────────────┬──────────────────────────────┘
                           │
                           ▼
    ┌─────────────────────────────────────────────────────┐
    │                 Unit of Work                          │
    │                                                      │
    │   ┌──────────────┐                                   │
    │   │ Change Track  │                                  │
    │   │               │                                  │
    │   │ New:    [A1]  │                                  │
    │   │ Dirty:  [B3]  │                                  │
    │   │ Deleted:[C7]  │                                  │
    │   └──────────────┘                                   │
    │                                                      │
    │   commit() ──► BEGIN                                 │
    │                INSERT A1                              │
    │                UPDATE B3                              │
    │                DELETE C7                              │
    │                COMMIT                                 │
    │                                                      │
    │   rollback() ──► ROLLBACK (discard all changes)      │
    └─────────────────────────────────────────────────────┘
```

## Implementation Guidelines

### Core Responsibilities

| Responsibility | Description |
|---------------|-------------|
| Change tracking | Track which entities are new, modified, or deleted |
| Transaction boundary | Define the start and end of the database transaction |
| Commit coordination | Persist all tracked changes in a single transaction |
| Rollback | Discard all tracked changes if any operation fails |
| Identity map | Ensure each entity is loaded only once per unit of work (prevent duplicate instances) |

### Transaction Boundary Placement

| Layer | Appropriate? | Rationale |
|-------|:------------:|-----------|
| Domain layer | No | Domain must not know about transactions |
| Application layer (Use Case) | Yes | Use cases define the business operation boundary |
| Adapter layer (Repository) | No | Too fine-grained; each repo call would be its own transaction |
| Controller/Handler | Sometimes | Acceptable for simple operations; prefer application layer |

**Rule:** The application layer (use case) is the correct place to begin and commit a unit of work. This aligns with the principle that use cases define the scope of a business operation.

### Commit and Rollback Rules

| Rule | Detail |
|------|--------|
| Explicit commit | The unit of work MUST be explicitly committed; no auto-commit on scope exit |
| Automatic rollback on failure | If an exception occurs before commit, the unit of work MUST roll back |
| Resource cleanup | Connections, cursors, and locks MUST be released in a finally block or equivalent |
| Single commit | Each unit of work commits exactly once; multiple commits within the same unit are forbidden |
| No business logic after commit | Once committed, no further domain operations should depend on the transaction |

### Nested Transaction Guidelines

| Approach | Behavior | Use When |
|----------|----------|----------|
| Flat (no nesting) | Inner "transactions" join the outer transaction | Default; simplest model |
| Savepoints | Inner scope creates a savepoint; can roll back to savepoint without aborting outer | Inner operations are independently retriable |
| Independent | Inner scope has its own transaction (separate connection) | Audit logging that must persist even if outer fails |

**Guideline:** Prefer flat transactions. Use savepoints sparingly for specific retry scenarios. Independent nested transactions are rare and should be justified explicitly.

### Ordering of Operations

| Phase | Actions | Rationale |
|-------|---------|-----------|
| 1. Inserts | Persist new entities first | Satisfy foreign key dependencies |
| 2. Updates | Apply modifications | Entities may reference newly inserted rows |
| 3. Deletes | Remove marked entities last | Avoid foreign key violations from premature deletion |

### Concurrency Control

| Strategy | Mechanism | Trade-off |
|----------|-----------|-----------|
| Optimistic locking | Version column checked at commit time; conflict = retry | Higher throughput; occasional retry on conflict |
| Pessimistic locking | SELECT FOR UPDATE at read time | Lower throughput; guaranteed no conflict |

**Guideline:** Prefer optimistic locking for most web applications. Use pessimistic locking only for high-contention scenarios where retry cost is prohibitive.

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Transaction per repository call | Each save is its own transaction; no atomicity across operations | One unit of work per use case |
| Long-running transactions | Holds database locks for extended periods | Keep transactions short; do expensive computation outside the transaction |
| Unit of work crossing service boundaries | Distributed transaction disguised as a unit of work | Use Saga pattern for cross-service coordination |
| Business logic inside commit hooks | Hard to test; hidden side effects | Keep business logic in domain layer |
| Forgetting to commit | Changes are silently lost | Enforce commit in the application layer; framework support helps |
| Committing in a loop | Multiple transactions where one is needed | Collect all changes; commit once |

## Relationship to Other Patterns

- **Repository Pattern**: Repositories perform operations within the unit of work's transaction context; they do not manage their own transactions
- **Hexagonal Architecture**: The unit of work is typically managed in the application layer (use case), orchestrating domain operations and persistence
- **CQRS**: The unit of work applies to the command side; the query side has no write transactions
- **Saga Pattern**: When business operations span multiple services, the saga replaces the unit of work as the consistency mechanism
- **Event Sourcing**: In event-sourced systems, appending events to the event store replaces the traditional unit of work; the event store provides its own consistency guarantees
