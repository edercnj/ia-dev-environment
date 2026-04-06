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


---

# Adapter Pattern (External Service Integration)

## Intent

The Adapter pattern provides a bridge between an application's internal interfaces and external services that have incompatible APIs, protocols, or data formats. In the context of service integration, the adapter encapsulates all communication details -- protocol handling, data format translation, error mapping, authentication, and resilience wrappers -- behind a clean interface that the application can consume without knowledge of the external system's peculiarities. This isolation makes external dependencies replaceable, testable, and maintainable.

## When to Use

- Integrating with any external service (third-party APIs, partner systems, payment processors, messaging platforms)
- When the external service's API contract differs from the internal domain model
- When the external service uses a different protocol than the application (REST vs gRPC, SOAP vs REST)
- When resilience patterns (circuit breaker, retry, timeout) must wrap external calls consistently
- Whenever the application should remain functional if the external service changes its API

## When NOT to Use

- Internal service-to-service calls within the same organization where contracts are aligned and co-owned
- When the external service's model matches the domain model exactly (though this is rare in practice)
- Trivial integrations that are unlikely to change (e.g., writing to standard output)
- When a higher-level pattern (Anti-Corruption Layer, BFF) already provides the necessary translation

## Structure

```
    ┌──────────────────────────────────────────────────────┐
    │                  Application                          │
    │                                                       │
    │  Domain Layer                                         │
    │  ┌─────────────────┐                                  │
    │  │  Outbound Port   │  (Interface defined by domain)  │
    │  │  e.g., Payment   │                                 │
    │  │  Gateway         │                                 │
    │  └────────┬─────────┘                                 │
    │           │                                           │
    │  Adapter Layer                                        │
    │  ┌────────▼──────────────────────────────────────┐    │
    │  │              Adapter Implementation             │   │
    │  │                                                │   │
    │  │  ┌──────────────┐  ┌──────────────────────┐   │   │
    │  │  │  Translator   │  │  Protocol Client     │   │   │
    │  │  │  (Format Map) │  │  (HTTP, gRPC, SOAP)  │   │   │
    │  │  └──────────────┘  └──────────────────────┘   │   │
    │  │  ┌──────────────┐  ┌──────────────────────┐   │   │
    │  │  │  Error Mapper │  │  Auth Handler        │   │   │
    │  │  └──────────────┘  └──────────────────────┘   │   │
    │  │  ┌──────────────┐  ┌──────────────────────┐   │   │
    │  │  │  Retry/CB    │  │  Request/Response Log │   │   │
    │  │  └──────────────┘  └──────────────────────┘   │   │
    │  └────────────────────────────────────────────────┘   │
    │           │                                           │
    └───────────┼───────────────────────────────────────────┘
                │
                ▼
    ┌──────────────────────┐
    │   External Service    │
    │   (Third-party API)   │
    └──────────────────────┘
```

## Implementation Guidelines

### Adapter Components

| Component | Responsibility |
|-----------|---------------|
| Port interface | Defined in the domain; describes what the application needs, not how it gets it |
| Protocol client | Manages HTTP connections, gRPC channels, SOAP clients, or TCP sockets |
| Request translator | Converts domain objects into the external service's request format |
| Response translator | Converts external responses into domain objects |
| Error mapper | Translates external errors (HTTP status codes, error payloads) into domain exceptions |
| Authentication handler | Manages API keys, OAuth tokens, mutual TLS, or other auth mechanisms |
| Resilience wrapper | Applies circuit breaker, retry, timeout, and bulkhead around external calls |

### Format Translation Principles

| Principle | Guideline |
|-----------|-----------|
| Domain-driven interface | The port interface uses domain types; the adapter handles all format conversion |
| No external types in domain | External DTOs, enums, and error codes MUST NOT leak past the adapter |
| Mapping completeness | Map all fields the domain needs; explicitly ignore irrelevant external fields |
| Default handling | Define sensible defaults for optional external fields that the domain requires |
| Validation | Validate external response data before translating to domain types |

### Protocol Bridging

| Scenario | Adapter Handles |
|----------|----------------|
| REST to gRPC | HTTP client on external side; gRPC service interface on internal side |
| REST to SOAP | HTTP/JSON externally; SOAP/XML translation in adapter |
| Async to Sync | Message queue consumer externally; synchronous port interface internally |
| Sync to Async | Synchronous port call; adapter publishes to external event system |
| Binary to structured | Raw binary protocol; adapter deserializes to domain structures |

### Resilience Wrapping

Every external service call through an adapter MUST be wrapped with resilience patterns:

| Pattern | Purpose | Configuration |
|---------|---------|---------------|
| Timeout | Prevent waiting indefinitely for external response | Connection: 3-5s, Read: 5-10s |
| Circuit breaker | Stop calling a failing external service | Per external service instance |
| Retry | Recover from transient external failures | Only for idempotent operations; with backoff and jitter |
| Bulkhead | Limit concurrent calls to external service | Based on external service's rate limits |
| Fallback | Provide degraded response when external service is unavailable | Cache, default, or error |

### Authentication Handling

| Auth Type | Adapter Responsibility |
|-----------|----------------------|
| API key | Attach key to headers; rotate keys without domain changes |
| OAuth2 client credentials | Manage token lifecycle (obtain, cache, refresh) transparently |
| Mutual TLS | Configure certificates; handle renewal |
| HMAC signing | Sign requests per the external service's specification |
| Custom auth | Implement the vendor's authentication flow |

**Rule:** Authentication details are entirely within the adapter. The domain MUST NOT know how authentication with the external service works.

### Error Handling Strategy

| External Error | Adapter Action | Domain Receives |
|---------------|---------------|-----------------|
| HTTP 400 (client error) | Log, translate | Domain-specific validation exception |
| HTTP 401/403 (auth) | Re-authenticate; retry once | Auth failure exception (if retry fails) |
| HTTP 404 (not found) | Translate | Domain-specific not-found exception |
| HTTP 429 (rate limit) | Respect Retry-After; queue or reject | Rate limit exception |
| HTTP 500/503 (server error) | Trigger retry/circuit breaker | Service unavailable exception |
| Network error | Trigger retry/circuit breaker | Connection failure exception |
| Unexpected response format | Log full response; translate to error | Integration exception |

### Testing Strategy

| Test Type | Purpose | Approach |
|-----------|---------|----------|
| Unit tests | Verify translation logic | Test translators with known inputs and expected outputs |
| Contract tests | Verify adapter against external API contract | Use consumer-driven contracts or recorded responses |
| Integration tests | Verify real communication | Call sandbox/staging external environments |
| Resilience tests | Verify circuit breaker, retry, timeout behavior | Simulate failures with mocks or chaos tools |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Exposing external DTOs to domain | Domain polluted with external concerns | Translate at the adapter boundary |
| No resilience wrapping | Single external failure cascades through the system | Wrap every external call with timeout, circuit breaker, retry |
| Hardcoded external URLs | Cannot switch environments or migrate | Externalize configuration |
| Auth credentials in adapter code | Security risk | Use secrets management; inject credentials |
| Single adapter for multiple services | Changes to one external service break integration with another | One adapter per external service |

## Relationship to Other Patterns

- **Hexagonal Architecture**: Adapters are the outer ring; they implement outbound ports defined in the domain layer
- **Anti-Corruption Layer**: The ACL is a specialized adapter focused on protecting the domain model from external model pollution
- **Circuit Breaker**: Wraps the adapter's external calls to detect and prevent cascading failures
- **Retry with Backoff**: Applied inside the adapter for transient external failures on idempotent operations
- **Backend for Frontend**: BFFs are client-facing adapters that aggregate and shape data from multiple backend services
- **Outbox Pattern**: When the adapter needs to reliably publish events after an external call, the outbox ensures consistency


---

# Anti-Corruption Layer (ACL)

## Intent

The Anti-Corruption Layer protects a bounded context's domain model from being polluted by the models, conventions, and assumptions of external systems or legacy contexts. It acts as a translation boundary that converts external concepts into the local domain's language and structures, preventing foreign models from leaking inward. Without an ACL, teams gradually compromise their domain model to accommodate external systems, leading to a tangled, inconsistent model that serves no context well.

## When to Use

- When integrating with legacy systems whose models differ significantly from the local domain
- When `architecture.domain_driven=true` and bounded context integrity must be preserved
- Integrating with third-party APIs, partner systems, or vendor platforms
- During migration (Strangler Fig) where old and new systems coexist
- When multiple teams own different bounded contexts with divergent models
- Whenever an external system's model would distort or complicate the local domain model

## When NOT to Use

- When the external system's model aligns closely with the local domain (shared kernel is sufficient)
- Internal communication within the same bounded context
- Trivial integrations where a simple mapping function suffices and there is no model divergence
- When the team owns both sides and can align the models directly

## Structure

```
    ┌──────────────────────────┐        ┌──────────────────────────┐
    │    Local Bounded Context  │        │   External System         │
    │                           │        │                           │
    │   Domain Model            │        │   Foreign Model           │
    │   (clean, consistent)     │        │   (legacy, different)     │
    │                           │        │                           │
    │   Domain Service          │        │   External API            │
    │        │                  │        │        │                  │
    │        ▼                  │        │        │                  │
    │   ACL Port (Interface)    │        │        │                  │
    │                           │        │        │                  │
    └───────────┬──────────────┘        └────────┼──────────────────┘
                │                                 │
                │         ┌───────────────┐       │
                └────────►│     ACL        │◄─────┘
                          │               │
                          │  Translator   │
                          │  Adapter      │
                          │  Facade       │
                          │               │
                          └───────────────┘

    Data Flow:
    External Model ──► ACL Translator ──► Domain Model
    Domain Model   ──► ACL Translator ──► External Model
```

## Implementation Guidelines

### ACL Components

| Component | Responsibility |
|-----------|---------------|
| Translator | Converts between external and domain models (structural mapping) |
| Adapter | Handles protocol and communication concerns (HTTP, gRPC, TCP) |
| Facade | Provides a simplified interface to complex external system interactions |
| Exception mapper | Converts external errors/exceptions into domain-defined exceptions |
| Validator | Validates external data meets domain invariants before translation |

### Translation Principles

| Principle | Guideline |
|-----------|-----------|
| Domain-first | The local domain model dictates the shape of the translation; never bend the domain to match the external model |
| Complete mapping | Every external concept used by the domain MUST be translated; no external types cross the boundary |
| Null safety | Handle missing or null fields from external systems gracefully; apply defaults or reject |
| Validation | Validate translated data against domain invariants; reject invalid external data at the boundary |
| Lossy translation | Not all external fields need mapping; translate only what the domain requires |

### Boundary Rules

| Rule | Detail |
|------|--------|
| No leakage inward | External DTOs, entities, and enums MUST NOT appear in the domain layer |
| No domain leakage outward | Domain models MUST NOT be sent to external systems; translate to external format |
| Interface in domain | The ACL interface (port) lives in the domain layer; the implementation lives in the adapter layer |
| One ACL per external system | Each external integration has its own ACL; do not share translators across systems |
| Version isolation | External API version changes are absorbed by the ACL; the domain remains stable |

### Handling External Model Changes

| Scenario | ACL Response | Domain Impact |
|----------|-------------|---------------|
| External field renamed | Update translator mapping | None |
| External field added | Ignore or map if domain-relevant | None or minimal |
| External field removed | Handle absence gracefully (default, error) | None |
| External API version change | Create new translator version; old one remains until deprecated | None |
| External model restructured | Rewrite translator; domain stays stable | None |

### Error Handling at the Boundary

| External Error | ACL Translation | Domain Receives |
|---------------|----------------|-----------------|
| HTTP 404 Not Found | Translate to domain "entity not found" | Domain-specific NotFoundException |
| HTTP 503 Unavailable | Translate to "service unavailable" | Domain-specific ServiceUnavailableException |
| Validation error from external | Translate to domain validation failure | Domain-specific ValidationException |
| Unexpected response format | Log, translate to system error | Domain-specific IntegrationException |
| Timeout | Translate to timeout domain error | Domain-specific TimeoutException |

### Testing Strategy

| Test Type | Scope | Purpose |
|-----------|-------|---------|
| Unit tests | Translator logic in isolation | Verify mapping correctness for all field combinations |
| Contract tests | ACL against external API contract | Detect external API changes early |
| Integration tests | ACL with real or simulated external system | Verify end-to-end translation |
| Null/edge case tests | Missing fields, unexpected values | Ensure robustness against external data quality |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Passing external DTOs into domain | Domain polluted with external concepts | Always translate at the ACL boundary |
| Shared translator across contexts | Changes to one integration break another | One ACL per external system |
| Business logic in the ACL | ACL becomes a hidden domain service | ACL only translates; logic stays in domain |
| No ACL ("just call the API") | External model gradually leaks into domain | Always use an ACL for external integrations |
| Bidirectional coupling | ACL depends on domain AND domain depends on ACL types | Domain depends only on the ACL port interface |

## Relationship to Other Patterns

- **Hexagonal Architecture**: The ACL is implemented as an outbound adapter, behind an outbound port defined in the domain
- **Adapter Pattern**: The ACL is a specialized application of the adapter pattern, focused on domain model protection
- **Modular Monolith**: Module public APIs act as lightweight ACLs between modules within the same deployment
- **Strangler Fig**: During migration, the ACL translates between legacy and new system models, enabling gradual replacement
- **Backend for Frontend**: BFFs may incorporate ACL-like translation to protect the frontend's model from backend complexity


---

# Backend for Frontend (BFF)

## Intent

The Backend for Frontend pattern creates a dedicated backend service tailored to the needs of a specific client type (web, mobile, CLI, third-party). Instead of forcing all clients to consume a single generic API -- which inevitably becomes a compromise that serves no client well -- each BFF aggregates, transforms, and shapes responses specifically for its client. This eliminates over-fetching, under-fetching, and client-side orchestration complexity while keeping backend services focused on domain logic rather than presentation concerns.

## When to Use

- `architecture.style=microservice` with multiple client types (web SPA, native mobile, CLI, partner API)
- When different clients need significantly different response shapes, fields, or aggregation from the same backend services
- When mobile clients need optimized, minimal payloads compared to feature-rich web responses
- When client teams want to iterate independently on their API contracts without coordinating with backend teams
- Systems where client-side orchestration of multiple microservices creates latency and complexity

## When NOT to Use

- Single-client applications where a single API is sufficient
- When all clients consume the same data in the same shape (a shared API gateway suffices)
- Small systems with one or two backend services where aggregation is trivial
- When the team cannot maintain multiple BFF services (operational overhead)
- When GraphQL or similar query languages solve the response-shaping problem adequately

## Structure

```
    ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
    │  Web    │   │ Mobile  │   │   CLI   │   │ Partner │
    │  SPA    │   │  App    │   │  Tool   │   │   API   │
    └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
    ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
    │ Web BFF │   │Mobile   │   │ CLI BFF │   │Partner  │
    │         │   │  BFF    │   │         │   │  BFF    │
    │ Full    │   │Minimal  │   │Structured│  │Stable   │
    │ payload │   │ payload │   │  output  │   │contract │
    └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘
         │              │              │              │
         └──────────────┼──────────────┼──────────────┘
                        │              │
                        ▼              ▼
              ┌──────────────────────────────────┐
              │        Backend Microservices       │
              │                                    │
              │  ┌────────┐ ┌────────┐ ┌────────┐│
              │  │Order   │ │User    │ │Catalog ││
              │  │Service │ │Service │ │Service ││
              │  └────────┘ └────────┘ └────────┘│
              └──────────────────────────────────┘
```

## Implementation Guidelines

### BFF Responsibilities

| Responsibility | Description |
|---------------|-------------|
| Response shaping | Select and transform fields for the specific client |
| Aggregation | Compose data from multiple backend services into a single response |
| Protocol translation | Adapt between client protocol (REST, GraphQL) and backend protocols (gRPC, events) |
| Pagination adaptation | Translate between client pagination expectations and backend pagination models |
| Error translation | Map backend errors into client-appropriate error formats and messages |
| Client-specific caching | Cache responses tailored to the client's access patterns |

### What a BFF Must NOT Do

| Forbidden | Rationale |
|-----------|-----------|
| Domain business logic | Business rules belong in backend services; BFF is presentation-layer only |
| Direct database access | BFF calls backend services; it does not own data |
| Data mutation logic | Write operations are forwarded to backend services; BFF does not process them |
| Shared state between BFFs | Each BFF is independent; sharing state re-creates the monolithic API problem |
| Authentication decisions | BFF validates tokens and forwards identity; authorization is in backend services |

### Per-Client Optimization

| Client Type | BFF Optimization |
|-------------|-----------------|
| Web SPA | Rich payloads, full entity details, pagination with counts, hypermedia links |
| Mobile native | Minimal payloads, compressed images, offline-friendly structures, delta updates |
| CLI tool | Structured output (JSON/YAML), machine-parseable errors, progress indicators |
| Partner/Third-party | Stable versioned API, backward-compatible, comprehensive documentation |

### Aggregation Guidelines

| Principle | Guideline |
|-----------|-----------|
| Parallel calls | Call independent backend services in parallel; aggregate results |
| Timeout per service | Each backend call has its own timeout; return partial data if non-critical services are slow |
| Fallback for non-critical data | If a secondary service fails, return the response without that data (with a flag indicating incompleteness) |
| Circuit breakers | Wrap each backend call in a circuit breaker to prevent cascading failures |
| No deep orchestration | BFFs aggregate (fan-out/fan-in); they do not orchestrate multi-step workflows |

### Versioning and Evolution

| Aspect | Guideline |
|--------|-----------|
| Owned by client team | The BFF is maintained by (or closely with) the team that owns the client |
| Independent deployment | Each BFF deploys independently from backend services and other BFFs |
| Contract evolution | BFF API contract evolves at the client's pace, not the backend's pace |
| Backend changes | Backend API changes are absorbed by the BFF; the client contract remains stable |
| Deprecation | BFF versions are retired when the corresponding client version is sunset |

### Operational Considerations

| Concern | Guideline |
|---------|-----------|
| Performance | BFFs add one network hop; minimize overhead with caching and parallel calls |
| Monitoring | Monitor BFF-specific metrics: aggregation latency, per-backend-call latency, error rates |
| Scaling | Scale each BFF independently based on its client's traffic patterns |
| Security | BFF validates authentication tokens; passes identity to backends; terminates client-facing TLS |
| Logging | Correlate BFF requests with backend service calls using a shared trace/correlation ID |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Shared BFF for all clients | Becomes a monolithic API; compromises for every client | One BFF per client type |
| Business logic in BFF | BFF becomes a hidden domain service | Keep BFF as a thin translation and aggregation layer |
| BFF calling other BFFs | Circular dependencies, layered complexity | BFFs call backend services only |
| No BFF ownership | BFF drifts without clear responsibility | Client team owns and maintains their BFF |
| Over-aggregation | BFF composes 10+ backend calls per request | Rethink backend API boundaries; consider backend aggregation service |

## Relationship to Other Patterns

- **API Gateway**: The gateway routes to BFFs; BFFs route to backend services. Gateway handles cross-cutting concerns (auth, rate limiting); BFFs handle client-specific shaping
- **Anti-Corruption Layer**: BFFs act as ACLs between the client's expected model and the backend services' domain models
- **Circuit Breaker**: Each BFF-to-backend call should be wrapped in a circuit breaker for resilience
- **Adapter Pattern**: BFFs are adapters that translate between client expectations and backend service contracts
- **CQRS**: BFFs naturally align with the query side; they aggregate read models from multiple services for client consumption


---

# API Gateway

## Intent

The API Gateway provides a single entry point for all client requests in a microservice architecture. It handles cross-cutting concerns -- routing, authentication, rate limiting, load balancing, and response aggregation -- at the edge, shielding internal services from direct client exposure. This decouples clients from the internal service topology, enabling services to be refactored, split, or merged without impacting consumers.

## When to Use

- `architecture.style=microservice` where clients need a unified API surface
- Systems with multiple client types (web, mobile, CLI) that require different response shapes
- When cross-cutting concerns (auth, rate limiting, logging) must be applied consistently at the edge
- APIs exposed to external consumers who should not know the internal service topology
- When request aggregation is needed to reduce client-side round trips

## When NOT to Use

- Monolithic applications with a single API surface
- Internal service-to-service communication (use service mesh or direct calls instead)
- When a simple reverse proxy or load balancer is sufficient
- Systems with only one or two services where the gateway adds unnecessary indirection
- When the team cannot maintain the gateway as a critical infrastructure component

## Structure

```
    Clients
    ┌──────┐  ┌──────┐  ┌──────┐
    │ Web  │  │Mobile│  │ CLI  │
    └──┬───┘  └──┬───┘  └──┬───┘
       │         │         │
       ▼         ▼         ▼
    ┌─────────────────────────────────────────┐
    │              API Gateway                 │
    │                                          │
    │  ┌──────────┐  ┌───────────┐  ┌───────┐│
    │  │  Auth     │  │Rate Limit │  │Logging││
    │  └──────────┘  └───────────┘  └───────┘│
    │  ┌──────────┐  ┌───────────┐  ┌───────┐│
    │  │  Routing  │  │Aggregation│  │ Cache ││
    │  └──────────┘  └───────────┘  └───────┘│
    │  ┌──────────────────────────────────────┐│
    │  │  Protocol Translation (REST/gRPC)    ││
    │  └──────────────────────────────────────┘│
    └──────┬──────────┬──────────┬─────────────┘
           │          │          │
           ▼          ▼          ▼
      ┌────────┐ ┌────────┐ ┌────────┐
      │Service │ │Service │ │Service │
      │   A    │ │   B    │ │   C    │
      └────────┘ └────────┘ └────────┘
```

## Implementation Guidelines

### Core Responsibilities

| Responsibility | Description |
|---------------|-------------|
| Request routing | Route requests to the appropriate backend service based on path, headers, or method |
| Authentication | Verify identity tokens (JWT, OAuth2) at the edge before forwarding |
| Authorization | Enforce coarse-grained access control (fine-grained stays in services) |
| Rate limiting | Protect backend services with per-client, per-endpoint, and global limits |
| Request/response transformation | Translate protocols, reshape payloads, add/remove headers |
| Aggregation | Compose responses from multiple backend services into a single client response |
| Load balancing | Distribute traffic across service instances |
| Caching | Cache responses for idempotent GET requests at the edge |

### Routing Principles

- Route based on URL path prefix, HTTP method, and headers
- Version APIs at the gateway level (path-based: /v1/, /v2/ or header-based)
- Support canary routing for gradual rollouts (route a percentage of traffic to new versions)
- Maintain a routing table that maps external paths to internal service endpoints
- Gateway routing configuration MUST be version-controlled and reviewed

### Authentication at the Edge

| Aspect | Guideline |
|--------|-----------|
| Token validation | Validate JWT/OAuth2 tokens at the gateway; reject invalid tokens before they reach services |
| Token propagation | Forward validated identity context (user ID, roles, tenant) to backend services via headers |
| Service identity | Backend services trust the gateway's forwarded identity; mutual TLS between gateway and services |
| Public endpoints | Explicitly whitelist unauthenticated endpoints; deny by default |

### Rate Limiting at the Edge

| Scope | Strategy | Purpose |
|-------|----------|---------|
| Per API key | Token bucket | Prevent single consumer from monopolizing resources |
| Per endpoint | Fixed window | Protect expensive operations |
| Global | Sliding window | Overall system protection |

### Aggregation Guidelines

- Aggregation requests to multiple backend services should execute in parallel
- Define timeouts per backend call; return partial results if non-critical services are slow
- Implement circuit breakers for each backend service the gateway calls
- Avoid deep business logic in the aggregation layer; it should be structural composition only

### Gateway Anti-Patterns

| Anti-Pattern | Problem | Alternative |
|-------------|---------|-------------|
| Business logic in gateway | Gateway becomes a monolith | Keep logic in services |
| Single gateway for all concerns | Single point of failure and bottleneck | Use BFF pattern for client-specific needs |
| No health checks for backends | Gateway routes to dead services | Integrate with service discovery and health checks |
| Unbounded aggregation | Gateway timeouts on complex compositions | Limit aggregation depth; use async patterns |
| Gateway as service mesh | Conflating edge and internal concerns | Use service mesh for service-to-service |

### Operational Guidelines

- The gateway is a critical path component; it MUST be highly available (multi-instance, multi-zone)
- Gateway configuration changes MUST go through the same review process as code changes
- Monitor gateway latency independently from backend latency to detect gateway-introduced overhead
- Implement circuit breakers within the gateway for each backend service
- Support graceful degradation: if a non-critical backend is down, serve partial responses

## Relationship to Other Patterns

- **Backend for Frontend (BFF)**: Specialized gateways per client type; the API gateway may route to BFFs rather than directly to services
- **Service Discovery**: The gateway must resolve service locations dynamically; integrate with service registry or DNS
- **Circuit Breaker**: Each backend call from the gateway should be wrapped in a circuit breaker
- **Rate Limiting**: The gateway is the primary enforcement point for rate limiting (see `core/09-resilience-principles.md`)
- **Bulkhead**: Isolate gateway resources per backend service to prevent one slow service from affecting all routes


---

# Bulkhead Pattern

## Intent

The Bulkhead pattern isolates system resources into independent compartments so that a failure or resource exhaustion in one compartment does not cascade to others. Named after the watertight compartments in ship hulls, this pattern ensures that a slow or failing dependency, tenant, or operation type consumes only its allocated resources, preserving the health of the rest of the system.

## When to Use

- `architecture.style=microservice` where services call multiple downstream dependencies
- Multi-tenant systems where one tenant's load must not impact others
- Services that handle multiple operation types with different latency profiles
- Systems where a single slow dependency has historically caused cascading failures
- Reference: `core/09-resilience-principles.md` for the resilience context

## When NOT to Use

- Single-purpose services with only one downstream dependency and one operation type
- Systems where all operations share identical latency and resource profiles
- When resource isolation overhead exceeds the benefit (very low-traffic systems)
- Applications where a single failure should halt all processing by design (fail-stop semantics)

## Structure

```
    Incoming Requests
    ┌────────────────────────────────────────────────┐
    │                                                │
    │   ┌──────────────┐  ┌──────────────┐          │
    │   │  Bulkhead A   │  │  Bulkhead B   │         │
    │   │  (Service X)  │  │  (Service Y)  │         │
    │   │               │  │               │         │
    │   │  Capacity: 10 │  │  Capacity: 20 │         │
    │   │  Active: 7    │  │  Active: 3    │         │
    │   │  Waiting: 2   │  │  Waiting: 0   │         │
    │   └───────┬───────┘  └───────┬───────┘         │
    │           │                  │                  │
    │           ▼                  ▼                  │
    │      Service X          Service Y              │
    │      (slow)             (healthy)              │
    │                                                │
    │   Service X slowness does NOT affect           │
    │   Service Y requests                           │
    └────────────────────────────────────────────────┘
```

## Implementation Guidelines

### Isolation Strategies

| Strategy | Mechanism | Overhead | Isolation Level | Best For |
|----------|-----------|----------|----------------|----------|
| Thread pool | Dedicated thread pool per resource | High (thread creation) | Strong | Long-running operations, blocking I/O |
| Semaphore | Counter limiting concurrent access | Low | Medium | Fast operations, non-blocking I/O |
| Connection pool | Separate pool per dependency | Medium | Strong | Database and HTTP connections |
| Process-level | Separate process or container | Very high | Very strong | Complete fault isolation |

### Thread Pool Isolation

| Parameter | Guideline |
|-----------|-----------|
| Pool size | Based on expected concurrency and downstream latency (requests/sec * avg_latency_sec) |
| Queue size | Small or zero; prefer rejection over unbounded queueing |
| Rejection policy | Return error immediately; never block the calling thread |
| Thread naming | Include bulkhead identity for debugging (e.g., bulkhead-serviceX-1) |
| Timeout | Each thread pool operation MUST have a timeout; never wait indefinitely |

### Semaphore Isolation

| Parameter | Guideline |
|-----------|-----------|
| Permits | Maximum concurrent executions allowed |
| Acquire timeout | Short (milliseconds); fail fast if permits unavailable |
| Fairness | Use fair ordering only if strict request ordering matters (adds overhead) |
| Release guarantee | ALWAYS release in a finally block; leaked permits cause resource starvation |

### Partitioning Strategies

| Partition By | Example | Rationale |
|-------------|---------|-----------|
| Downstream service | Bulkhead per external API | Slow API X does not exhaust resources for API Y |
| Operation type | Read vs Write bulkheads | Writes are slower; reads should not wait |
| Tenant | Bulkhead per tenant tier | Premium tenants isolated from noisy neighbors |
| Priority | Critical vs background tasks | Background processing does not starve critical path |
| Protocol | REST vs TCP vs gRPC | Traffic spikes in one protocol do not affect others |

### Sizing Guidelines

| Factor | Consideration |
|--------|--------------|
| Expected throughput | Requests per second for the protected operation |
| Downstream latency | Average and P99 latency of the dependency |
| Acceptable queue time | How long a request can wait before timing out |
| System capacity | Total threads or connections available to the service |
| Formula (thread pool) | pool_size = target_rps * p99_latency_seconds * safety_factor |

### Rejection Handling

When a bulkhead is full, requests MUST be rejected immediately with clear feedback:

| Channel | Rejection Response |
|---------|-------------------|
| REST API | HTTP 503 Service Unavailable with Retry-After header |
| gRPC | RESOURCE_EXHAUSTED status code |
| Async messaging | Negative acknowledgment; message returns to queue |
| Internal call | Throw specific BulkheadFullException |

### Monitoring Requirements

| Metric | Type | Alert Condition |
|--------|------|----------------|
| Active executions | Gauge (per bulkhead) | Sustained at or near capacity |
| Queue depth | Gauge (per bulkhead) | Growing over time |
| Rejected requests | Counter (per bulkhead) | Any rejections (investigate) |
| Wait time | Histogram (per bulkhead) | P99 approaching timeout |
| Execution time | Histogram (per bulkhead) | Drift from baseline |

## Relationship to Other Patterns

- **Reference**: `core/09-resilience-principles.md` defines bulkhead as a core resilience primitive with partitioning strategy
- **Circuit Breaker**: Bulkheads limit concurrent access; circuit breakers detect failure rates. Use both: bulkhead prevents resource exhaustion while circuit breaker detects downstream failure
- **Timeout Patterns**: Every bulkhead-protected operation MUST have a timeout; without it, slow requests occupy permits indefinitely
- **Retry with Backoff**: Retries against a bulkhead-protected resource must respect the bulkhead capacity; excessive retries can fill the bulkhead
- **API Gateway**: The gateway can enforce bulkhead isolation per route or per backend service, protecting the gateway's own resources


---

# Idempotency

## Intent

Idempotency ensures that performing the same operation multiple times produces the same result as performing it once. In distributed systems where network failures, retries, and message redelivery are inevitable, idempotency is the fundamental mechanism that prevents duplicate processing, double charges, duplicate records, and data corruption. Without idempotency, every retry is a risk.

## When to Use

- `architecture.style=microservice` and `event_driven=true` systems where retries are expected
- Any API endpoint that modifies state (POST, PUT, PATCH, DELETE)
- Message consumers in at-least-once delivery systems (Kafka, RabbitMQ, SQS)
- Saga steps and compensation actions that may be retried
- Payment processing, order creation, or any operation where duplication has business impact
- Webhook receivers that may receive the same event multiple times

## When NOT to Use

- Read-only (GET) operations that are naturally idempotent
- Operations where duplication is harmless (e.g., updating a record to the same value)
- Internal function calls within a single process with no retry mechanism
- When the operation is already idempotent by nature (e.g., PUT replacing the entire resource)

## Structure

```
    Client                    Service                    Store
      │                         │                          │
      │── Request + Key ──────►│                          │
      │                         │── Check Key Exists? ───►│
      │                         │◄── No (first time) ─────│
      │                         │                          │
      │                         │── Process Operation ────►│
      │                         │── Store Key + Result ───►│
      │                         │◄── Confirm ──────────────│
      │◄── Response ────────────│                          │
      │                         │                          │
      │── Same Request + Key ──►│                          │
      │                         │── Check Key Exists? ───►│
      │                         │◄── Yes (duplicate) ─────│
      │                         │── Load Stored Result ───►│
      │◄── Same Response ──────│                          │
```

## Implementation Guidelines

### Idempotency Key Design

| Aspect | Guideline |
|--------|-----------|
| Generation | Client generates a unique key per logical operation (UUID, ULID) |
| Transmission | Passed via a dedicated header (e.g., Idempotency-Key) or message attribute |
| Scope | One key per distinct business intent; retries use the same key |
| Format | UUID v4, ULID, or a deterministic hash of operation parameters |
| Uniqueness | Keys MUST be globally unique within the retention window |

### Deduplication Strategies

| Strategy | Mechanism | Best For |
|----------|-----------|----------|
| **Key-based lookup** | Store idempotency key in a dedicated table; check before processing | API requests |
| **Natural key** | Use business-meaningful identifiers (order ID + operation) | Domain operations |
| **Message ID** | Use the message broker's delivery ID | Event consumers |
| **Content hash** | Hash the request body; treat identical content as duplicate | Webhook receivers |
| **Database constraint** | Unique constraint on business key prevents duplicate inserts | Write operations |

### Storage for Idempotency State

| Storage | TTL | Consistency | Use Case |
|---------|-----|-------------|----------|
| Relational DB (same as business data) | Days to weeks | Strong (same transaction) | Critical operations (payments) |
| Redis / Cache | Hours to days | Eventual | High-throughput API deduplication |
| In-memory (local) | Minutes | None (single instance) | Development or single-instance services |

**Rule:** For critical operations, store the idempotency key in the SAME transaction as the business data change. This ensures atomicity: the operation is recorded as processed if and only if the business change commits.

### Request Lifecycle with Idempotency

| State | Meaning | Response to Duplicate |
|-------|---------|----------------------|
| Not found | First request with this key | Process normally |
| In progress | Another request with this key is currently being processed | Return 409 Conflict or wait |
| Completed | Request was already processed successfully | Return stored response |
| Failed | Previous attempt with this key failed | Allow retry (reprocess) |

### Exactly-Once Semantics

True exactly-once processing is achieved by combining:

| Component | Role |
|-----------|------|
| At-least-once delivery | Message broker guarantees the message is delivered (possibly more than once) |
| Consumer idempotency | Consumer deduplicates based on message ID or idempotency key |
| Result: Exactly-once processing | Each message is processed exactly once, despite possible redelivery |

### Retry Safety Guidelines

| Principle | Detail |
|-----------|--------|
| Same key on retry | Retries MUST use the same idempotency key as the original request |
| Fingerprint validation | On duplicate key, optionally verify the request body matches the original |
| Mismatch handling | If key matches but body differs, return 422 Unprocessable Entity |
| TTL on keys | Idempotency records MUST expire; retention period matches the business retry window |
| Cleanup | Purge expired idempotency records periodically |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Server-generated idempotency keys | Client cannot retry with the same key | Client generates keys |
| No TTL on idempotency records | Unbounded storage growth | Set expiration aligned with retry window |
| Check-then-act without locking | Race condition: two requests pass the check | Atomic check-and-insert (INSERT IF NOT EXISTS) |
| Idempotency only at the API layer | Internal event consumers still process duplicates | Apply at every boundary |
| Ignoring partial failures | Operation partially completed; retry completes the rest differently | Use transactions or compensation |

## Relationship to Other Patterns

- **Outbox Pattern**: The outbox guarantees at-least-once event delivery; consumers MUST be idempotent to handle redelivery
- **Saga Pattern**: Every saga step and compensation action MUST be idempotent to handle retries during orchestration
- **Retry with Backoff**: Retries are safe only when the target operation is idempotent
- **Event Sourcing**: Event handlers and projections MUST be idempotent since events may be replayed
- **Dead Letter Queue**: Messages that fail even with idempotent retry go to the DLQ for investigation


---

# Outbox Pattern

## Intent

The Outbox pattern solves the dual-write problem: when a service must both update its database and publish an event, these two operations cannot be performed atomically without a distributed transaction. The outbox pattern writes the event to an "outbox" table within the same local database transaction as the business data change, then a separate process reads from the outbox table and publishes to the message broker. This guarantees that events are published if and only if the business transaction commits.

## When to Use

- `architecture.style=microservice` with `event_driven=true`
- Any service that must reliably publish events after a state change
- When at-least-once event delivery is required (with consumer-side idempotency)
- Systems where losing an event means data inconsistency across services
- Saga step execution where event publication failure would corrupt the saga
- Whenever a database write and a message publish must be logically atomic

## When NOT to Use

- Single-service applications with no event consumers
- When the messaging system supports transactional producers natively (and you accept the coupling)
- Fire-and-forget notifications where occasional loss is acceptable
- Systems using event sourcing, where the event store IS the source of truth and can be tailed directly

## Structure

```
    ┌─────────────────────────────────────────────────┐
    │                   Service                        │
    │                                                  │
    │   Business Logic                                 │
    │        │                                         │
    │        ▼                                         │
    │   ┌──────────── Single Transaction ────────────┐ │
    │   │                                            │ │
    │   │   1. Write to Business Tables              │ │
    │   │   2. Write to Outbox Table                 │ │
    │   │                                            │ │
    │   └────────────────────────────────────────────┘ │
    │                                                  │
    │   Outbox Table                                   │
    │   ┌────────┬───────┬─────────┬────────┬───────┐ │
    │   │   id   │ type  │ payload │ status │ time  │ │
    │   ├────────┼───────┼─────────┼────────┼───────┤ │
    │   │  uuid  │ event │  json   │ PENDING│  ts   │ │
    │   └────────┴───────┴─────────┴────────┴───────┘ │
    │                    │                             │
    └────────────────────┼─────────────────────────────┘
                         │
              ┌──────────▼──────────┐
              │   Relay Process     │
              │  (Polling or CDC)   │
              └──────────┬──────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │   Message Broker    │
              │  (Kafka, RabbitMQ)  │
              └─────────────────────┘
```

## Implementation Guidelines

### Outbox Table Design

| Column | Purpose | Constraint |
|--------|---------|-----------|
| id | Unique event identifier | Primary key, UUID or ULID |
| aggregate_type | Type of the entity that changed | NOT NULL, indexed |
| aggregate_id | ID of the specific entity instance | NOT NULL, indexed |
| event_type | Name of the domain event | NOT NULL |
| payload | Serialized event data | NOT NULL, JSON |
| created_at | Timestamp of the event creation | NOT NULL, indexed for ordering |
| status | Processing status (PENDING, PUBLISHED, FAILED) | NOT NULL, indexed |
| retry_count | Number of publication attempts | NOT NULL, default 0 |
| published_at | Timestamp when successfully published | Nullable |

### Relay Strategies

| Strategy | Mechanism | Latency | Complexity | Reliability |
|----------|-----------|---------|------------|-------------|
| **Polling** | Periodic query on outbox table | Seconds (poll interval) | Low | High |
| **CDC (Change Data Capture)** | Database transaction log tailing | Milliseconds | Medium | Very high |
| **Transaction log mining** | Read DB WAL/binlog directly | Milliseconds | High | Very high |

### Polling Relay Guidelines

- Poll at a fixed interval (e.g., every 1-5 seconds) for PENDING records
- Process in batches ordered by created_at to maintain ordering
- Use SELECT FOR UPDATE SKIP LOCKED (or equivalent) to allow multiple relay instances
- Mark records as PUBLISHED after successful broker acknowledgment
- Delete or archive published records periodically to prevent table growth

### CDC Relay Guidelines

- Tail the database transaction log for changes to the outbox table
- Maintains strict ordering as events are captured in commit order
- No polling overhead; near-real-time event publishing
- Requires infrastructure support (Debezium, DynamoDB Streams, etc.)
- The relay process MUST track its position in the transaction log for crash recovery

### Delivery Guarantees

| Guarantee | Mechanism |
|-----------|-----------|
| At-least-once delivery | Outbox + relay ensures events are published; consumers MUST be idempotent |
| Ordering per aggregate | Events for the same aggregate_id are published in created_at order |
| No data loss | Event persisted in same transaction as business data; relay retries on failure |
| Duplicate detection | Consumers use the event id for deduplication |

### Housekeeping

| Task | Frequency | Action |
|------|-----------|--------|
| Purge published events | Daily or weekly | Delete or archive records where status = PUBLISHED and age > retention period |
| Retry failed events | Continuous (with backoff) | Increment retry_count; move to dead letter after max retries |
| Monitor outbox lag | Continuous | Alert when PENDING records exceed age threshold |
| Table size monitoring | Daily | Alert when outbox table exceeds expected size |

## Relationship to Other Patterns

- **Saga Pattern**: Each saga step uses the outbox pattern to reliably publish its completion or failure events
- **Event Sourcing**: In event-sourced systems, the event store can serve as the outbox, eliminating the need for a separate outbox table
- **Idempotency**: Since the outbox guarantees at-least-once delivery, all consumers MUST implement idempotent event handling
- **Dead Letter Queue**: Events that fail to publish after maximum retries are moved to a dead letter queue for manual intervention
- **CQRS**: The outbox ensures read model projections receive every event, maintaining consistency between command and query sides


---

# Saga Pattern

## Intent

The Saga pattern manages distributed transactions across multiple services where traditional ACID transactions are impossible. It decomposes a long-running business process into a sequence of local transactions, each within a single service, coordinated through events or a central orchestrator. When a step fails, previously completed steps are compensated (undone) through compensating transactions, maintaining eventual consistency across the system.

## When to Use

- `architecture.style=microservice` with `event_driven=true`
- Business processes that span multiple services and require all-or-nothing semantics
- Operations where distributed two-phase commit (2PC) is impractical or too slow
- Workflows with well-defined compensation logic for each step
- Long-running business processes (minutes to days) that cannot hold locks

## When NOT to Use

- Operations that fit within a single service's transactional boundary
- When strong consistency is required and a single database can serve the need
- Simple request-response interactions with no multi-step coordination
- When the team lacks experience with eventual consistency patterns
- Processes where compensation is impossible or undefined (e.g., sending a physical shipment)

## Structure

### Orchestration Approach

```
                         ┌──────────────┐
                         │    Saga       │
                         │ Orchestrator  │
                         └──────┬───────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                  │
              ▼                 ▼                  ▼
        ┌──────────┐    ┌──────────┐       ┌──────────┐
        │ Service A │    │ Service B │       │ Service C │
        │ Step 1    │    │ Step 2    │       │ Step 3    │
        │ Comp. 1   │    │ Comp. 2   │       │ Comp. 3   │
        └──────────┘    └──────────┘       └──────────┘
```

### Choreography Approach

```
   Service A          Service B          Service C
   ┌────────┐         ┌────────┐         ┌────────┐
   │ Step 1  │──Event──►│ Step 2  │──Event──►│ Step 3  │
   │         │         │         │         │         │
   │ Comp. 1 │◄─Event──┤ Comp. 2 │◄─Event──┤ Comp. 3 │
   └────────┘         └────────┘         └────────┘
```

### Comparison

| Aspect | Orchestration | Choreography |
|--------|--------------|--------------|
| Control flow | Centralized in orchestrator | Distributed across services |
| Visibility | Single place to see full workflow | Requires tracing across services |
| Coupling | Services coupled to orchestrator | Services coupled to event contracts |
| Complexity | Orchestrator can become complex | Hard to reason about with many steps |
| Scalability | Orchestrator can be bottleneck | Naturally distributed |
| Best for | Complex workflows (5+ steps) | Simple workflows (2-4 steps) |

## Implementation Guidelines

### Saga Design Principles

| Principle | Guideline |
|-----------|-----------|
| Atomicity per step | Each step is a local transaction within one service |
| Compensation | Every step (except the last) MUST have a defined compensating action |
| Idempotency | Every step and every compensation MUST be idempotent |
| Ordering | Compensation executes in reverse order of the original steps |
| State tracking | The saga's current state (which steps completed) MUST be persisted |

### Compensation Design

- Compensating actions undo the semantic effect, not necessarily the technical operation
- Compensation MUST be idempotent: running it multiple times produces the same result
- Compensation MAY fail; design for retry of compensating actions
- Some compensations are logical: instead of deleting a record, mark it as "cancelled"
- Track compensation status separately from forward step status

### Failure Handling

| Failure Type | Response |
|-------------|----------|
| Step fails (business error) | Begin compensation from the last successful step |
| Step fails (transient error) | Retry with backoff; compensate only after max retries exhausted |
| Compensation fails | Retry compensation with backoff; alert on repeated failure |
| Orchestrator crashes | Recover saga state from persistent store; resume from last known state |
| Timeout | Treat as failure after deadline; begin compensation |

### Timeout Management

| Aspect | Guideline |
|--------|-----------|
| Step timeout | Each step has an individual timeout proportional to its expected duration |
| Saga timeout | Overall saga has a maximum duration; exceeded = compensation |
| Timeout detection | Orchestrator polls or uses scheduled timers |
| Timeout action | Log, alert, and initiate compensation |
| Deadline propagation | Pass remaining saga deadline to each step so steps can fail fast |

### State Machine

```
  STARTED ──► STEP_1_PENDING ──► STEP_1_COMPLETE ──► STEP_2_PENDING ──► ...
                    │                                       │
                    ▼                                       ▼
              STEP_1_FAILED                           STEP_2_FAILED
                    │                                       │
                    ▼                                       ▼
              COMPENSATING ◄────────────────────────── COMPENSATING
                    │
                    ▼
              COMPENSATED (or COMPENSATION_FAILED)
```

### Observability Requirements

- Every saga instance MUST have a unique correlation ID propagated to all steps
- Log saga state transitions with step name, status, duration, and correlation ID
- Emit metrics: saga success/failure rate, average duration, compensation frequency
- Alert on sagas stuck in COMPENSATING for longer than expected

## Relationship to Other Patterns

- **Outbox Pattern**: Ensures reliable event publishing between saga steps; without it, events can be lost between committing a local transaction and publishing
- **Idempotency**: Every saga step and compensation MUST be idempotent to handle retries safely
- **Circuit Breaker**: Wrap external service calls within saga steps with circuit breakers to fail fast
- **Event Sourcing**: Saga state can be event-sourced, providing a complete history of the saga's progression
- **Dead Letter Queue**: Failed saga events that cannot be processed go to a DLQ for manual investigation


---

# Service Discovery

## Intent

Service Discovery solves the problem of locating service instances in a dynamic environment where instances are created, destroyed, and relocated continuously. Instead of hardcoding network addresses, services register themselves in a registry and discover each other at runtime. This enables elastic scaling, rolling deployments, and failure recovery without manual configuration changes.

## When to Use

- `architecture.style=microservice` where services need to locate each other dynamically
- Environments with auto-scaling where instance counts and addresses change frequently
- Kubernetes and container orchestration platforms (often built-in)
- When services are deployed across multiple hosts, zones, or regions
- Systems requiring zero-downtime deployments with rolling updates

## When NOT to Use

- Monolithic applications or modular monoliths deployed as a single unit
- Small systems with a fixed number of service instances behind a static load balancer
- When infrastructure-level service discovery (Kubernetes DNS) is sufficient and no application-level control is needed
- Environments where all services run on the same host

## Structure

### Server-Side Discovery

```
    Client ──► Load Balancer / Router ──► Service Registry
                      │                        │
                      │    ┌───────────────────┘
                      │    │ (resolve instances)
                      ▼    ▼
                ┌──────────────┐
                │  Instance A   │ ◄── registers
                │  Instance B   │ ◄── registers
                │  Instance C   │ ◄── registers
                └──────────────┘
```

### Client-Side Discovery

```
    Client ──► Service Registry ──► returns [Instance A, B, C]
       │
       │  (client-side load balancing)
       │
       ├──► Instance A
       ├──► Instance B
       └──► Instance C
```

### Comparison

| Aspect | Client-Side | Server-Side |
|--------|------------|-------------|
| Load balancing | Client decides | Router/LB decides |
| Client complexity | Higher (needs discovery logic) | Lower (just calls the router) |
| Network hops | Fewer (direct to instance) | One extra hop (through LB) |
| Language coupling | Client needs discovery library | Language-agnostic |
| Example tools | Eureka client, Consul client | Kubernetes Service, AWS ALB |
| Best for | Polyglot-tolerant teams | Platform-managed infrastructure |

## Implementation Guidelines

### Registration Approaches

| Approach | Mechanism | Responsibility |
|----------|-----------|---------------|
| Self-registration | Service registers itself on startup, heartbeats periodically | Service |
| Third-party registration | Platform agent or sidecar registers services | Infrastructure |
| Platform-native | Container orchestrator manages registration automatically | Platform (K8s, ECS) |

**Guideline:** Prefer platform-native registration when available. Self-registration is appropriate when running outside orchestrated environments.

### Health Check Integration

| Check Type | Purpose | Frequency | Failure Action |
|-----------|---------|-----------|----------------|
| Liveness | Is the process running? | Every 10-30s | Restart instance |
| Readiness | Can the instance serve traffic? | Every 5-15s | Remove from registry |
| Startup | Has the instance finished initializing? | During boot only | Wait before routing |
| Deep health | Are downstream dependencies accessible? | Every 30-60s | Mark degraded |

**Rules:**
- Liveness probes MUST be lightweight; they should never call external dependencies
- Readiness probes MAY check critical dependencies (database, cache)
- A failed readiness check MUST remove the instance from the discovery registry
- Recovery from readiness failure MUST re-register the instance automatically

### DNS-Based vs Registry-Based

| Aspect | DNS-Based | Registry-Based |
|--------|-----------|---------------|
| Resolution | DNS lookup (A/SRV records) | API call to registry |
| TTL sensitivity | Subject to DNS caching/TTL delays | Real-time updates |
| Metadata | Limited (SRV records for port/weight) | Rich (version, tags, health, zone) |
| Simplicity | Very simple; works everywhere | Requires registry infrastructure |
| Examples | Kubernetes DNS, Consul DNS | Eureka, Consul HTTP, etcd |

**Guideline:** Use DNS-based discovery as the baseline. Add registry-based discovery when you need metadata-aware routing, canary deployments, or real-time instance changes faster than DNS TTL allows.

### Instance Metadata

Services SHOULD register with metadata that enables intelligent routing:

| Metadata | Purpose |
|----------|---------|
| Service name | Logical service identifier |
| Instance ID | Unique instance identifier |
| Host and port | Network location |
| Version | Service version for canary routing |
| Zone / Region | Locality-aware routing |
| Weight | Traffic distribution preference |
| Health status | Current health state |
| Start time | Instance age for debugging |

### Failure Modes and Mitigations

| Failure | Impact | Mitigation |
|---------|--------|------------|
| Registry unavailable | No new discoveries | Cache last-known instance list; retry with backoff |
| Stale registration | Traffic routed to dead instance | Short TTL + health checks; client-side retry |
| Split brain | Inconsistent views across clients | Consensus-based registry (etcd, Consul) |
| Thundering herd on recovery | All instances re-register simultaneously | Jittered registration delay |

## Relationship to Other Patterns

- **API Gateway**: The gateway uses service discovery to resolve backend service locations dynamically
- **Circuit Breaker**: When an instance is consistently failing, the circuit breaker opens; combine with discovery to route away from unhealthy instances
- **Bulkhead**: Discovery metadata (zone, version) enables routing decisions that support bulkhead isolation
- **Retry with Backoff**: On connection failure to a discovered instance, retry with a different instance from the registry
- **Strangler Fig**: Service discovery enables gradual migration by routing traffic percentages to old vs new implementations


---

# Strangler Fig Pattern

## Intent

The Strangler Fig pattern enables incremental migration from a legacy monolith to a new architecture (typically microservices) by gradually replacing specific functionalities. Rather than a risky big-bang rewrite, new functionality is built in the new system while a routing layer progressively diverts traffic from legacy to new implementations. Over time, the legacy system is "strangled" as more features are migrated, until it can be decommissioned entirely.

## When to Use

- Migrating from monolith to microservices incrementally
- Legacy systems that cannot be rewritten all at once due to business continuity requirements
- When the legacy system is too large, too risky, or too poorly understood for a complete rewrite
- Organizations that need to deliver business value continuously during the migration
- When the migration timeline spans months or years

## When NOT to Use

- The legacy system is small enough for a clean rewrite in a few weeks
- The legacy system has no users and can be taken offline during migration
- When the routing and dual-running overhead exceeds the risk of a direct cutover
- When the target architecture is not meaningfully different from the source (refactor in place instead)
- Greenfield projects with no legacy to migrate from

## Structure

```
    ┌──────────────────────────────────────────────────────┐
    │                   Routing Layer                       │
    │              (Proxy / API Gateway)                    │
    │                                                      │
    │   Route A ──────────────────────► New Service A      │
    │   Route B ──────────────────────► New Service B      │
    │   Route C ──┐                                        │
    │   Route D ──┤                                        │
    │   Route E ──┴──────────────────► Legacy Monolith     │
    │                                                      │
    └──────────────────────────────────────────────────────┘

    Migration Timeline:
    ──────────────────────────────────────────────────────►

    Phase 1: [████░░░░░░] 20% migrated — Routes A, B in new system
    Phase 2: [██████░░░░] 60% migrated — Routes C, D extracted
    Phase 3: [██████████] 100% migrated — Legacy decommissioned
```

## Implementation Guidelines

### Migration Sequence

| Step | Action | Validation |
|------|--------|------------|
| 1. Introduce routing layer | Place a proxy/gateway in front of the monolith | All existing traffic flows through unchanged |
| 2. Identify migration candidate | Choose a bounded context with clear boundaries | Low coupling to other monolith components |
| 3. Build new implementation | Develop the feature in the new architecture | Passes all functional tests |
| 4. Shadow traffic | Route copies of production traffic to new system (results discarded) | Compare outputs for correctness |
| 5. Canary release | Route a small percentage of real traffic to new system | Monitor error rates, latency, correctness |
| 6. Gradual cutover | Increase traffic percentage to new system | Metrics stable at each increment |
| 7. Full cutover | Route 100% to new system | Legacy route disabled but reversible |
| 8. Decommission legacy route | Remove legacy code for this feature | After sufficient bake period |

### Candidate Selection Criteria

| Factor | Preferred | Risky |
|--------|-----------|-------|
| Coupling | Low coupling to other monolith components | Deeply entangled with shared state |
| Complexity | Well-understood business logic | Poorly documented, tribal knowledge only |
| Change frequency | Frequently changing (high ROI) | Stable, rarely modified (low ROI) |
| Data ownership | Clean data boundaries | Shared database tables across features |
| Team readiness | Team experienced with the domain | Domain expertise concentrated in one person |

### Routing Strategies

| Strategy | Mechanism | Granularity |
|----------|-----------|-------------|
| Path-based | Route by URL path prefix | Per feature/endpoint |
| Header-based | Route by custom header or user attribute | Per user segment |
| Percentage-based | Route N% to new, (100-N)% to legacy | Per request (random) |
| Feature toggle | Route based on feature flag per user/tenant | Per user or tenant |
| Geographic | Route based on client region | Per region |

### Data Migration Considerations

| Approach | Description | Risk |
|----------|-------------|------|
| Shared database | Both systems read/write the same database during transition | Schema coupling; migration blocked |
| Database per service | New service has its own database; data synchronized | Data consistency during transition |
| Event-driven sync | Legacy publishes events; new system consumes | Requires legacy modification |
| ETL/batch sync | Periodic data synchronization between stores | Staleness window |

**Guideline:** Start with a shared database if legacy modification is difficult. Migrate to separate databases per service as the boundary solidifies. The shared database is a transitional state, not the target.

### Parallel Running

Running both legacy and new implementations simultaneously to validate correctness:

| Aspect | Guideline |
|--------|-----------|
| Shadow mode | Send copies of requests to both; compare responses; only return legacy response |
| Verification | Automated comparison of legacy vs new responses; log discrepancies |
| Duration | Run parallel for at least one full business cycle (week, month) |
| Performance | Shadow traffic must not slow down the primary (legacy) response path |

### Rollback Strategy

- Every migration step MUST be reversible by routing traffic back to the legacy system
- Maintain the legacy implementation in a functional state until the new implementation is proven
- Feature toggles enable instant rollback without deployment
- Data written by the new system during canary MUST be reconcilable with legacy if rolled back

### Observability During Migration

| Metric | Purpose |
|--------|---------|
| Traffic split ratio | Percentage of requests going to legacy vs new |
| Error rate comparison | New system error rate vs legacy error rate per route |
| Latency comparison | P50, P95, P99 latency of new vs legacy per route |
| Data consistency | Discrepancies between legacy and new system outputs |
| Migration progress | Number of routes migrated vs remaining |

## Relationship to Other Patterns

- **API Gateway**: The gateway is the natural location for the routing layer that directs traffic between legacy and new systems
- **Anti-Corruption Layer**: New services MUST implement an ACL when consuming legacy APIs or data to prevent legacy models from leaking in
- **Service Discovery**: New services register with the discovery mechanism; the routing layer resolves their locations
- **Modular Monolith**: Often the intermediate step -- decompose the monolith into modules first, then extract modules into services
- **Feature Toggles**: Enable fine-grained control over which users or tenants are routed to the new system


---

# Circuit Breaker

## Intent

The Circuit Breaker pattern prevents an application from repeatedly attempting an operation that is likely to fail, protecting both the calling service and the failing dependency. By detecting sustained failure rates and short-circuiting requests, it enables fast failure responses, gives the failing system time to recover, and prevents cascading failures across the system. When the dependency recovers, the circuit breaker gradually resumes normal traffic.

## When to Use

- Any call to an external dependency (database, API, message broker, cache) that can fail or become slow
- `architecture.style=microservice` where service-to-service calls are common
- Systems where a failing dependency would otherwise cause thread exhaustion or timeout accumulation
- Operations where waiting for a timeout on every request is unacceptable (fail-fast is preferred)
- Reference: `core/09-resilience-principles.md` provides the condensed circuit breaker summary

## When NOT to Use

- In-process method calls with no external dependency
- Operations that are expected to fail frequently by design (e.g., cache lookups where misses are normal)
- Fire-and-forget operations where the caller does not wait for a response
- When the dependency has its own robust retry and recovery mechanisms that should not be bypassed

## Structure

```
    ┌────────────────────────────────────────────────────────┐
    │                  Circuit Breaker States                  │
    │                                                         │
    │   ┌─────────┐    failure ratio    ┌─────────┐          │
    │   │         │    >= threshold     │         │          │
    │   │ CLOSED  │───────────────────►│  OPEN   │          │
    │   │ (normal)│                     │ (reject)│          │
    │   │         │◄────┐              │         │          │
    │   └─────────┘     │              └────┬────┘          │
    │                    │                   │                │
    │              success              delay expires        │
    │              >= N                      │                │
    │                    │              ┌────▼────┐          │
    │                    │              │         │          │
    │                    └──────────────┤HALF-OPEN│          │
    │                                   │ (test)  ├──────┐  │
    │                                   │         │      │  │
    │                                   └─────────┘  failure │
    │                                                    │  │
    │                                        ┌───────────┘  │
    │                                        ▼              │
    │                                     OPEN              │
    └────────────────────────────────────────────────────────┘
```

## Implementation Guidelines

### State Behavior

| State | Behavior | Transitions |
|-------|----------|-------------|
| CLOSED | All requests pass through; failures are counted in a sliding window | Moves to OPEN when failure ratio exceeds threshold within the measurement window |
| OPEN | All requests are rejected immediately without calling the dependency | Moves to HALF-OPEN after a configured delay period |
| HALF-OPEN | A limited number of test requests are allowed through | Moves to CLOSED if test requests succeed; returns to OPEN if any test request fails |

### Configuration Guidelines

| Parameter | Guideline | Rationale |
|-----------|-----------|-----------|
| Sliding window size | 10-100 requests or 10-60 seconds | Must be large enough to smooth out normal fluctuations |
| Failure ratio threshold | 30-50% | Below 30% may be too sensitive; above 50% too lenient |
| Minimum call threshold | 10+ requests in window | Prevents opening on insufficient data |
| Wait duration (open state) | 15-30 seconds | Long enough for transient issues to resolve; short enough to recover quickly |
| Permitted calls in half-open | 3-5 requests | Enough to gain confidence without risking a flood |
| Success threshold (half-open) | 3-5 consecutive successes | Confirms the dependency has genuinely recovered |
| Slow call duration threshold | Based on P99 of healthy operation | Slow calls counted as failures when ratio is exceeded |
| Slow call ratio threshold | 50-80% | When this percentage of calls exceed the duration threshold |

### What Counts as a Failure

| Counts as Failure | Does NOT Count as Failure |
|------------------|--------------------------|
| Connection refused | Client-side validation error (4xx) |
| Connection timeout | Business logic rejection |
| Read timeout | Successful response with error payload |
| HTTP 5xx responses | HTTP 4xx responses (client error) |
| Exceptions from the dependency | Cancellation by the caller |
| Slow responses exceeding threshold | Responses within acceptable latency |

### Fallback Strategies

| Strategy | When to Use | Constraint |
|----------|-------------|-----------|
| Return cached data | Read operations where stale data is acceptable | Track staleness; alert on prolonged fallback |
| Return default value | Operations with a safe default (empty list, default config) | Default must be semantically safe |
| Call alternative dependency | Backup service or replica available | Alternative must be independent (not same failure) |
| Queue for later processing | Write operations that can be deferred | Queue must be bounded; process when circuit closes |
| Fail immediately with clear error | Operations where no degradation is acceptable | Return appropriate error code (503, UNAVAILABLE) |

**Golden Rule (from core/09):** When the circuit is OPEN for a critical dependency, ALWAYS fail secure. NEVER approve, allow, or return success when the dependency required for that decision is unavailable.

### Per-Dependency Circuits

Each external dependency MUST have its own circuit breaker instance. Never share a single circuit breaker across multiple dependencies.

| Dependency | Separate Circuit | Rationale |
|-----------|-----------------|-----------|
| Database | Yes | DB failure should not open the cache circuit |
| Cache | Yes | Cache failure should not block DB operations |
| External API A | Yes | API A failure is independent of API B |
| External API B | Yes | Each dependency fails independently |

### Monitoring and Alerting

| Metric | Type | Alert Condition |
|--------|------|-----------------|
| Circuit state | Gauge | Transition to OPEN |
| Calls total | Counter (success/failure/rejected/ignored) | Rejected count increasing |
| Failure ratio | Gauge | Approaching threshold |
| Not permitted calls | Counter | Any (means circuit is open) |
| State transition events | Counter (per transition type) | OPEN transitions exceeding baseline |

### Logging Requirements

| Event | Level | Required Context |
|-------|-------|-----------------|
| Circuit opened | ERROR | Circuit name, failure count, failure ratio, window duration |
| Circuit half-opened | INFO | Circuit name, wait duration elapsed |
| Circuit closed | INFO | Circuit name, success count in half-open |
| Request rejected (circuit open) | WARN | Circuit name, operation attempted |

## Relationship to Other Patterns

- **Reference**: `core/09-resilience-principles.md` defines the circuit breaker as a core resilience primitive
- **Retry with Backoff**: Retries happen INSIDE the circuit breaker; when the circuit opens, retries stop immediately
- **Timeout Patterns**: Timeouts trigger failures that the circuit breaker counts; without timeouts, slow calls accumulate without opening the circuit
- **Bulkhead**: Bulkheads limit concurrent access; circuit breakers detect failure rates. Use both together for comprehensive protection
- **Fallback/Graceful Degradation**: The circuit breaker triggers the fallback; the fallback defines what happens when the circuit is open
- **Service Discovery**: When a circuit opens for a specific instance, discovery can route to healthy instances


---

# Dead Letter Queue (DLQ)

## Intent

The Dead Letter Queue captures messages that cannot be processed successfully after all retry attempts are exhausted. Rather than losing failed messages or blocking the processing pipeline, the DLQ provides a safe holding area where problematic messages can be inspected, diagnosed, and manually replayed once the underlying issue is resolved. It is the last line of defense in an event-driven system's error handling chain.

## When to Use

- Event-driven systems with asynchronous message processing
- `architecture.event_driven=true` where at-least-once delivery is configured
- Any message consumer that may encounter unprocessable messages (poison pills)
- Systems where message loss is unacceptable and failed messages must be preserved
- Saga orchestration where failed steps need manual investigation
- Webhook receivers where incoming events may contain unexpected formats

## When NOT to Use

- Synchronous request-response systems where errors are returned directly to the caller
- Messages that are intentionally discarded on failure (telemetry, non-critical metrics)
- When the processing system has no mechanism for manual intervention or replay
- Log-based systems where the original event log is preserved and can be replayed from source

## Structure

```
    Producer ──► Main Queue ──► Consumer
                                   │
                              [Process Message]
                                   │
                          ┌────────┼────────┐
                          │                  │
                       Success            Failure
                          │                  │
                          ▼                  ▼
                       Commit            Retry (N times)
                                             │
                                        ┌────┼────┐
                                        │         │
                                    Recovered  Exhausted
                                        │         │
                                     Commit       ▼
                                          ┌──────────────┐
                                          │  Dead Letter  │
                                          │    Queue      │
                                          │               │
                                          │ [Failed Msg]  │
                                          │ [Error Info]  │
                                          │ [Timestamp]   │
                                          │ [Attempt #]   │
                                          └──────┬───────┘
                                                 │
                                          ┌──────▼───────┐
                                          │  DLQ Monitor  │
                                          │  (Alert +     │
                                          │   Dashboard)  │
                                          └──────────────┘
```

## Implementation Guidelines

### DLQ Message Enrichment

When a message is moved to the DLQ, it MUST carry diagnostic metadata:

| Field | Purpose |
|-------|---------|
| Original message | The complete original message payload |
| Original queue/topic | Where the message was originally consumed from |
| Error message | The exception or error that caused the final failure |
| Stack trace | Full stack trace from the last processing attempt |
| Retry count | Number of attempts made before moving to DLQ |
| First failure time | Timestamp of the first processing attempt |
| Last failure time | Timestamp of the final processing attempt |
| Consumer instance | Identifier of the consumer that last attempted processing |
| Correlation ID | Business correlation ID for tracing |

### Poison Pill Detection

A poison pill is a message that will never be processed successfully regardless of how many times it is retried. Detecting these early prevents wasted retry cycles.

| Detection Strategy | Mechanism |
|-------------------|-----------|
| Schema validation | Validate message format before processing; reject invalid immediately |
| Error classification | Distinguish permanent errors (deserialization, validation) from transient errors |
| Fast-fail on known patterns | If the error type is non-retryable, move to DLQ immediately without retrying |
| Retry threshold | If a message has been redelivered N times (broker-tracked), move to DLQ |

### DLQ Processing Workflow

| Step | Action | Responsibility |
|------|--------|---------------|
| 1. Alert | Notify operations team when DLQ message count exceeds threshold | Automated monitoring |
| 2. Triage | Classify messages by error type; identify root cause | Operations / Development |
| 3. Fix | Deploy fix for the processing logic or data issue | Development |
| 4. Replay | Replay messages from DLQ back to the original queue | Operations (manual or tooling) |
| 5. Verify | Confirm replayed messages are processed successfully | Automated monitoring |
| 6. Purge | Remove successfully replayed messages from DLQ | Automated or manual |

### Replay Guidelines

| Principle | Guideline |
|-----------|-----------|
| Ordering | Replay messages in the order they were added to the DLQ |
| Rate limiting | Replay at a controlled rate to avoid overwhelming the consumer |
| Idempotency | Consumers MUST be idempotent; replayed messages may be duplicates |
| Selective replay | Support replaying by error type, time range, or correlation ID |
| Dry run | Support a validation mode that checks if messages would process successfully without committing |
| Audit trail | Log every replay action with who initiated it and the result |

### Monitoring and Alerting

| Metric | Type | Alert Condition |
|--------|------|-----------------|
| DLQ message count | Gauge | Any messages present (investigate) |
| DLQ ingestion rate | Counter | Messages entering DLQ faster than being resolved |
| DLQ age (oldest message) | Gauge | Oldest message exceeds SLA (e.g., > 1 hour) |
| Replay success rate | Gauge | Replayed messages failing again |
| DLQ by error type | Counter (per type) | New error type appearing |

**Rule:** DLQ messages MUST trigger alerts. A growing DLQ is a symptom of a systemic issue that requires attention. DLQs should normally be empty.

### Retention and Lifecycle

| Aspect | Guideline |
|--------|-----------|
| Retention period | Based on business SLA; typically 7-30 days |
| Archival | After retention period, archive to cold storage before deletion |
| Size limits | Set maximum DLQ size; alert well before the limit |
| Cleanup | Automated purge of messages older than retention period |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Ignoring the DLQ | Failed messages accumulate unnoticed | Alert on any DLQ message; review daily |
| DLQ without metadata | Cannot diagnose why the message failed | Enrich with error, stack trace, context |
| Automatic replay without fix | Replayed messages fail again, re-entering DLQ | Fix the root cause before replaying |
| No replay tooling | Manual, error-prone replay process | Build or adopt replay tooling with rate limiting |
| DLQ for every error | Transient errors bypass retry | Only DLQ after retries exhausted or on permanent errors |
| No retention policy | DLQ grows unbounded | Set TTL and archival policies |

## Relationship to Other Patterns

- **Retry with Backoff**: Retries are the first line of defense for transient failures; the DLQ is the last resort after retries are exhausted
- **Circuit Breaker**: When a circuit opens, messages may fail processing; these failures contribute to DLQ volume
- **Outbox Pattern**: If the outbox relay cannot publish a message after max retries, it moves the message to a DLQ
- **Saga Pattern**: Failed saga steps that cannot be compensated or retried produce DLQ entries for manual resolution
- **Idempotency**: Replayed messages from the DLQ must be handled idempotently by consumers
- **Event Sourcing**: In event-sourced systems, the original event stream provides an alternative replay mechanism; the DLQ captures projection failures


---

# Retry with Backoff

## Intent

The Retry with Backoff pattern handles transient failures by automatically re-attempting failed operations with progressively increasing delays between attempts. Transient failures -- network glitches, temporary resource exhaustion, brief service unavailability -- are common in distributed systems and often resolve on their own. Retry with backoff provides automatic recovery from these failures while preventing the system from overwhelming an already struggling dependency through controlled spacing of attempts.

## When to Use

- Operations against external dependencies that experience transient failures (network, database, APIs)
- `architecture.style=microservice` where network calls are frequent and unreliable
- Idempotent operations that are safe to retry without side effects
- Connection establishment where transient rejection is expected under load
- Reference: `core/09-resilience-principles.md` defines retry as a core resilience mechanism

## When NOT to Use

- Non-idempotent operations (INSERT without idempotency key, financial transactions without deduplication)
- Business validation errors (400 Bad Request, authorization failures, constraint violations)
- Permanent failures that will not resolve with time (invalid credentials, missing resource)
- Operations inside an open circuit breaker (retries are suppressed by the circuit)
- When the client's timeout budget does not allow for retry delays

## Structure

```
    Request ──► Attempt 1 ──[fail]──► Wait (base delay)
                                          │
                                          ▼
                    Attempt 2 ──[fail]──► Wait (base * 2 + jitter)
                                              │
                                              ▼
                        Attempt 3 ──[fail]──► Wait (base * 4 + jitter)
                                                  │
                                                  ▼
                            Attempt 4 ──[fail]──► Exhausted ──► Fail / Fallback

    Delay Progression (exponential with jitter):
    ┌────┐   ┌─────────┐   ┌──────────────────┐   ┌─────────────────────────┐
    │ 1s │   │ 2s +jit │   │   4s + jitter    │   │     8s + jitter         │
    └────┘   └─────────┘   └──────────────────┘   └─────────────────────────┘
```

## Implementation Guidelines

### Backoff Strategies

| Strategy | Delay Formula | Use Case |
|----------|--------------|----------|
| Fixed | delay = constant | Simple cases; low concurrency |
| Linear | delay = base * attempt | Gradual increase; moderate concurrency |
| Exponential | delay = base * 2^attempt | Standard choice; scales well under contention |
| Exponential with cap | delay = min(base * 2^attempt, max_delay) | Prevents excessively long waits |
| Decorrelated jitter | delay = random(base, previous_delay * 3) | Best distribution under high concurrency |

### Jitter: Why It Is Mandatory

Without jitter, all clients that failed at the same time will retry at the same time, creating a thundering herd that overwhelms the recovering dependency. Jitter randomizes retry timing to spread the load.

| Jitter Type | Mechanism | Effectiveness |
|-------------|-----------|---------------|
| Full jitter | delay = random(0, base * 2^attempt) | High spread; some retries are immediate |
| Equal jitter | delay = base * 2^attempt / 2 + random(0, base * 2^attempt / 2) | Good spread; minimum wait guaranteed |
| Decorrelated jitter | delay = random(base, previous_delay * 3) | Best overall distribution |

**Rule:** ALWAYS use jitter. Retry without jitter is an anti-pattern that causes synchronized retry storms.

### Configuration Guidelines

| Parameter | Guideline | Rationale |
|-----------|-----------|-----------|
| Max attempts | 3-5 | Enough for transient recovery; not so many that failures take minutes |
| Base delay | 100ms-1s | Short enough for quick recovery; long enough to be meaningful |
| Max delay (cap) | 10-30s | Prevents individual retries from exceeding useful timeout budgets |
| Overall timeout | Sum of all delays + execution time < client timeout | Retries must complete within the caller's patience window |

### Retryable vs Non-Retryable Classification

| Retryable (Transient) | Non-Retryable (Permanent) |
|----------------------|--------------------------|
| Connection reset / refused | Authentication failure (401, 403) |
| DNS resolution timeout | Bad request (400) |
| Socket timeout | Not found (404) |
| HTTP 503 Service Unavailable | Unique constraint violation |
| HTTP 429 Too Many Requests (with Retry-After) | Business validation error |
| Lock wait timeout | Data format / parsing error |
| Optimistic lock conflict | Unsupported media type (415) |

**Rule:** Classify errors explicitly. The default for unknown errors should be non-retryable. Only retry errors you have confirmed are transient.

### Retry Budgets

A retry budget limits the total number of retries a service performs across all operations within a time window, preventing a service from amplifying load on a struggling dependency.

| Aspect | Guideline |
|--------|-----------|
| Budget scope | Per dependency (not global) |
| Budget size | 10-20% of normal request volume |
| Measurement window | Rolling 10-60 second window |
| Exceeded budget | Stop retrying; fail immediately |
| Monitoring | Track budget utilization as a metric |

### Interaction with Timeouts

- The total time for all retry attempts MUST be less than the caller's timeout
- Each individual attempt has its own timeout (connection timeout + read timeout)
- Calculate: max_attempts * (attempt_timeout + max_delay) must fit within the overall timeout budget
- If remaining budget is less than one attempt, do not retry

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| Retry without jitter | Thundering herd on recovery | Always add jitter |
| Retry non-idempotent operations | Duplicated side effects | Only retry idempotent operations |
| Infinite retries | Never gives up; accumulates threads | Set max attempts |
| Fixed delay without backoff | Does not give dependency time to recover | Use exponential or decorrelated backoff |
| Retry across all services simultaneously | Amplified load cascades | Use retry budgets |
| Retry on circuit-open | Bypasses the circuit breaker | Retry wraps inside circuit breaker |
| Retry business errors | Will never succeed | Classify errors; only retry transient |

## Relationship to Other Patterns

- **Reference**: `core/09-resilience-principles.md` defines retry as a core resilience primitive with retryable vs non-retryable classification
- **Circuit Breaker**: The circuit breaker wraps the retry logic; when the circuit opens, retries are suppressed. Retried failures contribute to the circuit breaker's failure count
- **Timeout Patterns**: Each retry attempt must have its own timeout; the total retry time must fit within the overall operation timeout
- **Idempotency**: Retries are only safe when the operation is idempotent; without idempotency, retries risk duplication
- **Bulkhead**: Retries consume bulkhead capacity; a high retry rate can exhaust the bulkhead for legitimate new requests
- **Dead Letter Queue**: Operations that fail after all retries are exhausted should be routed to a DLQ for investigation


---

# Timeout Patterns

## Intent

Timeout patterns prevent operations from waiting indefinitely for responses that may never arrive. In distributed systems, a missing timeout means a single slow or unresponsive dependency can consume threads, connections, and memory until the entire service becomes unresponsive. Timeouts are the foundational mechanism that makes all other resilience patterns (circuit breakers, retries, bulkheads) effective by ensuring that failures are detected within bounded time.

## When to Use

- Every external call: database queries, API calls, cache lookups, message broker operations
- Every network operation: connection establishment, TLS handshake, data transfer
- `architecture.style=microservice` where service-to-service calls traverse the network
- Long-running internal operations that must complete within a business SLA
- Reference: `core/09-resilience-principles.md` defines timeout as a core resilience mechanism

## When NOT to Use

- In-process computations with deterministic, bounded execution time
- Fire-and-forget operations where the caller does not wait for a result
- Streaming operations that are expected to run indefinitely (use heartbeat and idle timeout instead)

## Structure

```
    Client                   Service A                  Service B
      │                         │                          │
      │── Overall Timeout ─────►│                          │
      │   (e.g., 30s)          │── Connection Timeout ───►│
      │                         │   (e.g., 3s)            │
      │                         │                          │
      │                         │◄─ Connected ─────────────│
      │                         │                          │
      │                         │── Read Timeout ─────────►│
      │                         │   (e.g., 5s)            │
      │                         │                          │
      │                         │◄─ Response ──────────────│
      │◄─ Response ─────────────│                          │
      │                         │                          │

    Deadline Propagation:
    ┌──────────────────────────────────────────────────────────┐
    │ Client deadline: T=30s                                    │
    │                                                           │
    │ Service A receives at T=0, spends 2s processing           │
    │ Service A calls B with deadline: T=28s                    │
    │                                                           │
    │ Service B receives, spends 1s processing                  │
    │ Service B calls C with deadline: T=27s                    │
    │                                                           │
    │ Each hop reduces the remaining deadline                   │
    └──────────────────────────────────────────────────────────┘
```

## Implementation Guidelines

### Timeout Types

| Timeout Type | What It Guards | Typical Range |
|-------------|---------------|---------------|
| Connection timeout | Time to establish a TCP connection | 1-5 seconds |
| TLS handshake timeout | Time to complete TLS negotiation | 2-5 seconds |
| Read timeout (socket) | Time waiting for data after connection established | 3-10 seconds |
| Write timeout | Time to send request data | 3-10 seconds |
| Request timeout (overall) | Total time from request start to response complete | 5-30 seconds |
| Idle timeout | Time a connection can remain idle before being closed | 30s-10 minutes |
| Pool acquire timeout | Time to acquire a connection from a pool | 3-5 seconds |

### Configuration Guidelines

| Operation | Connection Timeout | Read Timeout | Overall Timeout |
|-----------|-------------------|-------------|-----------------|
| Database query | 3s | 5s | 10s |
| Database write | 3s | 5s | 10s |
| Internal API call | 2s | 5s | 10s |
| External API call | 5s | 10s | 15s |
| Cache lookup | 1s | 2s | 3s |
| Message publish | 2s | 5s | 8s |
| Health check | 1s | 2s | 3s |

**Rule:** Application-level timeouts MUST be shorter than client-level timeouts. A response that arrives after the client has given up is wasted work and wasted resources.

### Deadline Propagation

Deadline propagation passes the remaining time budget from service to service through a call chain, ensuring downstream services know how much time they have and can fail fast if the budget is already exhausted.

| Principle | Guideline |
|-----------|-----------|
| Carry deadline | Pass the absolute deadline (wall clock time) or remaining duration in request headers/metadata |
| Subtract overhead | Each service subtracts its own processing time before passing the deadline downstream |
| Fail fast | If remaining deadline is less than the expected operation time, fail immediately without calling downstream |
| Standard headers | Use gRPC deadlines natively; for HTTP, use a custom header (e.g., X-Request-Deadline) |
| Clock skew | Prefer duration-based (remaining seconds) over absolute timestamps to avoid clock sync issues |

### Timeout Hierarchy

Timeouts must be ordered from innermost to outermost:

```
    Connection timeout < Read timeout < Request timeout < Client timeout

    Example:
    Connection: 3s < Read: 5s < Request: 10s < Client: 30s
```

**Rule:** Inner timeouts MUST be shorter than outer timeouts. A read timeout of 30s inside a request timeout of 10s means the request timeout is ineffective.

### Timeout on Pooled Resources

| Pool Type | Acquire Timeout | Guideline |
|-----------|----------------|-----------|
| Database connection pool | 3-5s | Fail fast; do not queue indefinitely for a connection |
| HTTP connection pool | 2-3s | Create new connection or fail; do not wait |
| Thread pool (bulkhead) | 0-1s | Reject immediately if pool is full |

### What to Do When a Timeout Fires

| Action | Detail |
|--------|--------|
| Cancel the operation | Close the connection, cancel the query, abort the request |
| Release resources | Return connections to pools, release semaphore permits |
| Log with context | Operation name, timeout value, elapsed time, destination |
| Emit metric | Counter for timeout events per operation type |
| Apply fallback | Return default, cached value, or error based on the pattern in use |
| Count as failure | Timeouts count toward circuit breaker failure ratio |

### Anti-Patterns

| Anti-Pattern | Problem | Solution |
|-------------|---------|----------|
| No timeout configured | Thread hangs indefinitely waiting for response | Every external call MUST have a timeout |
| Timeout too large (> 60s) | Slow leak of threads/connections on failing dependencies | Align with SLA; usually 5-15s for synchronous calls |
| Inner timeout > outer timeout | Inner timeout never fires; outer fires first confusingly | Order: connection < read < request < client |
| Timeout without cancellation | Operation continues after timeout; wasted resources | Cancel downstream operations when timeout fires |
| Same timeout for all operations | Slow queries and fast cache lookups treated the same | Configure per-operation-type |
| Hardcoded timeouts | Cannot adjust without code changes | Externalize to configuration |

## Relationship to Other Patterns

- **Reference**: `core/09-resilience-principles.md` defines timeout as a core resilience mechanism with guidelines per operation type
- **Circuit Breaker**: Timeout events count as failures for the circuit breaker; without timeouts, the circuit breaker cannot detect slow failures
- **Retry with Backoff**: Each retry attempt has its own timeout; total retry time (attempts * timeout + backoff delays) must fit within the overall deadline
- **Bulkhead**: Timeout ensures that slow operations do not hold bulkhead permits indefinitely
- **Saga Pattern**: Each saga step has an individual timeout; the saga itself has an overall deadline
- **Dead Letter Queue**: Messages that timeout during processing should be negatively acknowledged and eventually routed to a DLQ
