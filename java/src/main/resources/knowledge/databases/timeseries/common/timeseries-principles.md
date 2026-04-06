# Time-Series Database Principles

## What is Time-Series Data

Time-series data is a sequence of data points indexed by timestamp. Each point represents a measurement or event at a specific moment.

| Aspect | Time-Series | Relational (OLTP) | Columnar (OLAP) |
|--------|-------------|-------------------|-----------------|
| Write pattern | Append-only, high-throughput | Random insert/update | Batch insert |
| Read pattern | Time-range scans, aggregations | Point lookups | Full-column scans |
| Data lifecycle | Recent data hot, old data cold | All data equally accessible | All data scanned |
| Schema | Fixed structure per metric | Flexible schema | Wide denormalized |
| Primary index | Timestamp + series identifier | Primary key | Sort key |

## Core Concepts

### Measurements and Series

| Concept | Detail | Example |
|---------|--------|---------|
| Measurement/Metric | What is being measured | `cpu_usage`, `temperature`, `http_requests` |
| Tag/Label | Metadata for filtering and grouping | `host=server-1`, `region=us-east` |
| Field/Value | The actual measured value | `usage=78.5`, `count=1024` |
| Timestamp | When the measurement was taken | `2024-01-15T10:30:00Z` |
| Series | Unique combination of measurement + tags | `cpu_usage{host=server-1,cpu=0}` |

### Cardinality

| Concept | Detail |
|---------|--------|
| Series cardinality | Total unique combinations of measurement + tag values |
| Tag cardinality | Number of distinct values for a single tag |
| High cardinality | Tags with many unique values (e.g., user_id, request_id) |

**Rule:** Series cardinality MUST be bounded. High-cardinality tags (unbounded values like UUIDs) cause index bloat and memory exhaustion.

### Retention Policies

| Strategy | Detail |
|----------|--------|
| TTL-based | Automatically delete data older than N days |
| Downsampling | Aggregate old data to coarser granularity before deletion |
| Tiered storage | Move old data to cheaper storage (S3, HDD) |
| Continuous aggregates | Pre-compute rollups incrementally |

### Downsampling Pattern

```
Raw data (1s resolution) -> 5min aggregates -> 1h aggregates -> 1d aggregates
      Keep 7 days             Keep 30 days      Keep 1 year      Keep forever
```

## When to Use Time-Series

### Choose Time-Series DB When

| Criterion | Threshold |
|-----------|-----------|
| Write pattern | Append-only, > 100K writes/sec |
| Query pattern | Time-range aggregations dominate |
| Data lifecycle | Retention policies needed (TTL) |
| Data shape | Timestamped measurements with tags |
| Downsampling | Automatic rollup needed |

### Anti-Patterns (When NOT to Use Time-Series)

| Scenario | Why TSDB is Wrong | Better Alternative |
|----------|------------------|-------------------|
| Transactional data | No ACID, no updates | PostgreSQL, CockroachDB |
| Document storage | Schema-free flexibility | MongoDB |
| Relationship-heavy data | No JOIN support | PostgreSQL, Neo4j |
| Full-text search | No inverted indexes | Elasticsearch |
| Low-volume structured data | Overhead not justified | PostgreSQL |
| Event sourcing | Need event replay, projections | EventStoreDB, Kafka |

## Data Modeling Principles

### Tag vs Field Decision

| Use As Tag When | Use As Field When |
|----------------|-------------------|
| Used in WHERE/GROUP BY | Only displayed or aggregated |
| Low cardinality (< 10K values) | High cardinality (unique per row) |
| Metadata (host, region, env) | Measured values (temperature, count) |
| Indexed for fast filtering | Not indexed |

### Schema Design Patterns

| Pattern | Use Case | Example |
|---------|----------|---------|
| Wide | Few series, many fields per point | `cpu{host=X}: user=10, system=5, idle=85` |
| Narrow | Many series, one field per point | `cpu_user{host=X}: 10`, `cpu_system{host=X}: 5` |

**Rule:** Prefer wide schema (multiple fields per measurement) over narrow (one field per measurement). Wide schema reduces series cardinality and ingestion overhead.

## Compression Techniques

| Technique | Applied To | Compression Ratio |
|-----------|-----------|-------------------|
| Delta-of-delta | Timestamps | 10-100x |
| XOR encoding (Gorilla) | Float values | 2-10x |
| Dictionary encoding | Tag values | 5-50x |
| Run-length encoding | Repeated values | 5-50x |
| LZ4/ZSTD block compression | General blocks | 2-5x |

## Continuous Aggregates

Pre-computed rollups that update incrementally:

| Property | Detail |
|----------|--------|
| Real-time | Updated as new data arrives |
| Incremental | Only processes new data since last refresh |
| Materialized | Stored on disk, not computed at query time |
| Cascading | Aggregates can build on other aggregates |

## CAP Positioning

| System | CAP Choice | Consistency Model | Notes |
|--------|-----------|-------------------|-------|
| InfluxDB | AP | Eventual (clustered) | Strong on single node |
| TimescaleDB | CP | ACID (PostgreSQL) | Full PostgreSQL transactions |

## Framework Integration

| Database | Java Client | Protocol |
|----------|------------|----------|
| InfluxDB | `com.influxdb:influxdb-client-java` | HTTP (Flux/InfluxQL), line protocol |
| TimescaleDB | `org.postgresql:postgresql` | PostgreSQL wire protocol |

## Sensitive Data

| Rule | Detail |
|------|--------|
| PII in tags | NEVER store PII as tags (indexed, widely replicated) |
| PII in fields | Encrypt before storage if required |
| Retention compliance | Enforce TTL aligned with data retention regulations |
| Access control | Implement per-database or per-measurement access |
