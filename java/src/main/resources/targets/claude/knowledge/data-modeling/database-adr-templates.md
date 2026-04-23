# Database ADR Templates

Architecture Decision Record templates for common database decisions. Each template follows the standard ADR format with Context, Decision Drivers, Considered Options, Decision Outcome, and Consequences sections.

> **Usage:** Copy the relevant template, fill in project-specific details in `[brackets]`, and save as `docs/adr/NNNN-title.md`.

## ADR: SQL vs NoSQL database selection

### Context

The system requires persistent storage for [describe domain entities and access patterns]. The team must choose between a relational (SQL) database and a non-relational (NoSQL) database to meet functional and non-functional requirements.

### Decision Drivers

- Data consistency requirements (strong vs eventual)
- Query complexity and join frequency
- Schema stability vs evolution speed
- Read/write ratio and throughput targets
- Horizontal scalability requirements
- Team expertise and operational maturity

### Considered Options

**Option A: SQL (Relational)**
- Pros: ACID transactions, complex joins, mature tooling, strong consistency
- Cons: Vertical scaling limits, rigid schema, sharding complexity

**Option B: NoSQL (Document)**
- Pros: Flexible schema, horizontal scaling, high write throughput
- Cons: Limited joins, eventual consistency by default, denormalization overhead

**Option C: NoSQL (Key-Value / Wide-Column)**
- Pros: Extreme throughput, predictable latency, linear scalability
- Cons: Limited query patterns, no ad-hoc queries, data modeling constraints

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]

## ADR: Embedded vs Referenced data model (document DBs)

### Context

The application uses a document database for [describe use case]. The team must decide whether to embed related data within parent documents or use references (foreign keys) to link separate collections.

### Decision Drivers

- Read vs write frequency for related data
- Document size growth patterns
- Atomicity requirements for related updates
- Query patterns (always fetched together vs independently)
- Data duplication tolerance

### Considered Options

**Option A: Embedded (denormalized)**
- Pros: Single read for complete data, atomic updates within document, no joins
- Cons: Document size growth, update anomalies, duplication

**Option B: Referenced (normalized)**
- Pros: No duplication, independent updates, smaller documents
- Cons: Multiple reads required, no atomic cross-document updates

**Option C: Hybrid (selective embedding)**
- Pros: Optimized per access pattern, balanced trade-offs
- Cons: Increased complexity, inconsistent modeling approach

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]

## ADR: Partitioning/sharding strategy selection

### Context

The [table/collection name] has grown beyond [size/row count] and requires partitioning to maintain query performance, enable efficient maintenance operations, and support data lifecycle management.

### Decision Drivers

- Query patterns and partition key selectivity
- Data distribution uniformity
- Growth rate and retention requirements
- Cross-partition query frequency
- Maintenance operation complexity

### Considered Options

**Option A: Range partitioning**
- Pros: Natural for time-series data, efficient range scans, simple archival
- Cons: Risk of hotspots on recent partitions, uneven distribution

**Option B: Hash partitioning**
- Pros: Even data distribution, no hotspots, predictable partition sizes
- Cons: Range queries span all partitions, rebalancing on partition count change

**Option C: List partitioning**
- Pros: Logical grouping by category, partition pruning for filtered queries
- Cons: Uneven partition sizes, requires known value set

**Option D: Composite partitioning**
- Pros: Combines strategies (e.g., range + hash), fine-grained control
- Cons: Higher complexity, more partitions to manage, planner overhead

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]

## ADR: Caching layer selection and topology

### Context

The system experiences [describe performance bottleneck or latency requirement]. A caching layer is needed to reduce database load and improve response times for [describe access patterns].

### Decision Drivers

- Latency requirements (p50, p95, p99 targets)
- Cache hit ratio expectations
- Data freshness requirements (TTL tolerance)
- Consistency model (cache-aside vs write-through)
- Infrastructure complexity budget

### Considered Options

**Option A: Local in-process cache**
- Pros: Lowest latency, no network hop, simple setup
- Cons: Memory bound per instance, no sharing across nodes, cold start on deploy

**Option B: Distributed cache (Redis/Memcached)**
- Pros: Shared across instances, survives deploys, large capacity
- Cons: Network latency added, operational overhead, serialization cost

**Option C: Multi-tier cache (local + distributed)**
- Pros: L1 local for hot data, L2 distributed for warm data, best latency
- Cons: Cache coherence complexity, invalidation propagation, debugging difficulty

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]

## ADR: Read replica topology (sync vs async, regional)

### Context

The system requires read scaling or geographic distribution for [describe workload]. The team must decide on a replication strategy that balances consistency, latency, and availability requirements.

### Decision Drivers

- Consistency requirements for read-after-write scenarios
- Geographic distribution of users
- Acceptable replication lag tolerance
- Failover and high availability requirements
- Write throughput impact tolerance

### Considered Options

**Option A: Synchronous replication**
- Pros: Zero data loss, strong read-after-write consistency
- Cons: Higher write latency, reduced availability on replica failure

**Option B: Asynchronous replication**
- Pros: No write latency impact, tolerates replica failures gracefully
- Cons: Replication lag, potential stale reads, data loss on primary failure

**Option C: Regional async replicas**
- Pros: Low-latency reads per region, geographic redundancy
- Cons: Cross-region lag, eventual consistency, conflict resolution complexity

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]

## ADR: Distributed transaction strategy (2PC vs Saga vs Outbox)

### Context

The system spans multiple services/databases that must maintain data consistency for [describe business operation]. The team must select a distributed transaction coordination strategy.

### Decision Drivers

- Consistency requirements (strong vs eventual)
- Transaction latency budget
- Failure recovery complexity tolerance
- Service coupling preference (tight vs loose)
- Compensating action feasibility

### Considered Options

**Option A: Two-Phase Commit (2PC)**
- Pros: Strong consistency (ACID across participants), familiar model
- Cons: Blocking protocol, reduced availability, coordinator is SPOF

**Option B: Saga pattern**
- Pros: Non-blocking, high availability, per-service autonomy
- Cons: Eventual consistency, compensating transactions required, complex debugging

**Option C: Transactional Outbox**
- Pros: At-least-once delivery, no distributed lock, decoupled publishing
- Cons: Polling or CDC overhead, message ordering complexity, eventual consistency

### Decision Outcome

[Chosen option] because [justification based on decision drivers above].

### Consequences

- **Positive:** [List benefits aligned with chosen option]
- **Negative:** [List trade-offs and mitigations needed]
- **Risks:** [Identify risks and monitoring strategies]
