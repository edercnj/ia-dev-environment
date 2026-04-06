# NewSQL/Distributed Database Principles

## What is NewSQL

NewSQL databases combine SQL semantics and ACID transactions with horizontal scalability:

| Aspect | Traditional SQL | NoSQL | NewSQL |
|--------|----------------|-------|--------|
| Scalability | Vertical (scale-up) | Horizontal (scale-out) | Horizontal (scale-out) |
| Transactions | Full ACID | Eventual / limited | Full distributed ACID |
| Query language | SQL | Custom APIs | SQL (wire-compatible) |
| Schema | Strict | Schema-free | Strict |
| Consistency | Strong | Eventual (tunable) | Strong (serializable) |

## Consensus Protocols

### Raft Consensus

| Concept | Detail |
|---------|--------|
| Leader election | One leader per Raft group; handles all writes |
| Log replication | Leader replicates WAL entries to followers |
| Quorum | Majority (N/2 + 1) must acknowledge for commit |
| Fault tolerance | Survives (N-1)/2 node failures |
| Split-brain prevention | Only one leader per term; old leaders step down |

### Paxos vs Raft

| Aspect | Paxos | Raft |
|--------|-------|------|
| Understandability | Complex, many variants | Designed for clarity |
| Leader | Optional (Multi-Paxos uses leader) | Required |
| Adoption | Spanner (Google) | CockroachDB, YugaByteDB, TiDB |
| Log compaction | Implementation-dependent | Snapshot-based |

## Distributed Transactions

### Two-Phase Commit (2PC)

```
Phase 1 (Prepare): Coordinator asks all participants to prepare
Phase 2 (Commit):  If all prepared -> commit; else -> abort
```

| Property | Detail |
|----------|--------|
| Atomicity | All or nothing across shards |
| Blocking | Participants hold locks during prepare |
| Coordinator failure | Can block; mitigated by Raft-replicated coordinator |

### Clock Synchronization

| Mechanism | Database | Precision | Notes |
|-----------|----------|-----------|-------|
| **TrueTime** | Google Spanner | ~7ms uncertainty | GPS + atomic clocks |
| **Hybrid Logical Clock (HLC)** | CockroachDB, YugaByteDB | NTP-dependent | Physical + logical component |
| **TSO (Timestamp Oracle)** | TiDB | Centralized | Single-point timestamp service |

HLC combines physical clock with logical counter:

```
HLC = (physical_timestamp, logical_counter)
```

| Rule | Detail |
|------|--------|
| Causality | If event A happens before B, HLC(A) < HLC(B) |
| NTP dependency | Clock skew must be bounded (default: 500ms max offset) |
| Uncertainty window | Reads may wait for uncertainty interval to resolve |

## CAP Positioning

| System | CAP Choice | Default Consistency | Notes |
|--------|-----------|-------------------|-------|
| CockroachDB | CP | Serializable | Sacrifices availability on network partition |
| YugaByteDB | CP (tunable) | Strong (Raft-based) | Can tune to AP per table |
| TiDB | CP | Snapshot isolation | Optimistic + pessimistic locking |

## Data Distribution

### Range-Based Sharding

| Concept | Detail |
|---------|--------|
| Range | Contiguous key range assigned to a tablet/range |
| Split | Automatic when range exceeds size threshold (e.g., 512 MB) |
| Merge | Automatic when adjacent ranges are underutilized |
| Rebalance | Tablets moved between nodes for even distribution |

### Hash-Based Sharding

| Concept | Detail |
|---------|--------|
| Hash function | Key hashed to determine shard placement |
| Distribution | Even distribution regardless of key patterns |
| Range queries | Scatter-gather (less efficient than range sharding) |

## When to Use NewSQL

### Choose NewSQL When

| Criterion | Threshold |
|-----------|-----------|
| Transaction scope | Multi-row, multi-table ACID across nodes |
| Scale requirement | Beyond single-node capacity (> 1 TB or > 10K TPS) |
| SQL compatibility | Existing SQL workload needs horizontal scaling |
| Geo-distribution | Multi-region deployment with local reads |
| Consistency | Serializable isolation required at scale |

### Anti-Patterns (When NOT to Use NewSQL)

| Scenario | Why NewSQL is Wrong | Better Alternative |
|----------|-------------------|-------------------|
| Simple CRUD < 1 TB | Operational overhead not justified | PostgreSQL, MySQL |
| Analytics / OLAP | Not optimized for full-table scans | ClickHouse, Druid |
| Document storage | Schema-free flexibility needed | MongoDB, DynamoDB |
| Time-series workloads | Specialized storage more efficient | TimescaleDB, InfluxDB |
| Graph traversals | Not optimized for multi-hop queries | Neo4j, Neptune |

## Multi-Region Patterns

| Pattern | Latency | Consistency | Use Case |
|---------|---------|-------------|----------|
| **Leader in one region** | Low local, high remote writes | Strong | Single-region primary |
| **Geo-partitioned** | Low for local data | Strong per partition | Compliance (data residency) |
| **Follower reads** | Low reads everywhere | Bounded staleness | Read-heavy, global users |
| **Multi-region active** | Moderate everywhere | Serializable | Global writes needed |

## Framework Integration

| Database | Wire Protocol | Java Driver | Spring Integration |
|----------|-------------|------------|-------------------|
| CockroachDB | PostgreSQL | `org.postgresql:postgresql` | `spring-boot-starter-data-jpa` |
| YugaByteDB | PostgreSQL (YSQL) / Cassandra (YCQL) | `org.postgresql:postgresql` or `com.yugabyte:java-driver-core` | `spring-boot-starter-data-jpa` |
| TiDB | MySQL | `com.mysql:mysql-connector-j` | `spring-boot-starter-data-jpa` |

## Sensitive Data

| Rule | Detail |
|------|--------|
| Encryption at rest | All NewSQL databases support TDE; enable by default |
| Encryption in transit | TLS mandatory for inter-node and client connections |
| Row-level security | Use database-native RBAC where available |
| Audit logging | Enable query audit log for compliance |
| Geo-fencing | Use geo-partitioning to enforce data residency |
