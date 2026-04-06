# ClickHouse — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **23.x** | Lightweight deletes, new JSON type (experimental) | Production stable |
| **24.x** | Improved JOIN algorithms, variant type, refreshable MVs | Current LTS |
| **24.8+** | SharedMergeTree (cloud), dynamic subcolumns | Latest |

## Table Engine Selection

| Engine | Use Case | Key Properties |
|--------|----------|----------------|
| **MergeTree** | Default analytical tables | Sorted, partitioned, primary key |
| **ReplacingMergeTree** | Deduplication by sort key | Last version wins on merge |
| **AggregatingMergeTree** | Pre-aggregated rollups | Stores intermediate aggregation states |
| **SummingMergeTree** | Auto-sum numeric columns | Collapses rows with same sort key |
| **CollapsingMergeTree** | Mutable state via sign column | Sign=1 insert, Sign=-1 cancel |
| **VersionedCollapsingMergeTree** | Collapse with out-of-order inserts | Version column resolves order |
| **Distributed** | Query routing across shards | Virtual table over local tables |

### MergeTree Table Example

```sql
CREATE TABLE events (
    event_date Date,
    event_time DateTime64(3),
    user_id UInt64,
    event_type LowCardinality(String),
    payload String,
    amount Decimal64(2)
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_type, user_id, event_time)
TTL event_date + INTERVAL 90 DAY
SETTINGS index_granularity = 8192;
```

## Column Type Conventions

| Data | ClickHouse Type | Notes |
|------|----------------|-------|
| Identifiers | `UInt64` or `UUID` | Prefer UInt64 for sort key performance |
| Low-cardinality strings | `LowCardinality(String)` | Dictionary-encoded, < 10K distinct values |
| Timestamps | `DateTime64(3)` | Millisecond precision |
| Dates | `Date` or `Date32` | Use Date32 for dates beyond 2149 |
| Monetary amounts | `Decimal64(2)` | Fixed-point, 2 decimal places |
| Nullable fields | `Nullable(T)` | Adds overhead; avoid in sort key columns |
| Enums | `Enum8` / `Enum16` | When values are fixed and known at DDL time |
| JSON-like data | `String` + JSON functions | Or experimental `JSON` type in 24.x+ |
| IP addresses | `IPv4` / `IPv6` | Native types with optimized storage |
| Arrays | `Array(T)` | Supports nested arrays and array functions |

## Materialized Views

Materialized views in ClickHouse are insert triggers, not periodic refreshes:

```sql
-- Source table
CREATE TABLE raw_events (
    timestamp DateTime64(3),
    user_id UInt64,
    event_type LowCardinality(String),
    amount Decimal64(2)
) ENGINE = MergeTree()
ORDER BY (event_type, timestamp);

-- Aggregated destination
CREATE TABLE hourly_stats (
    hour DateTime,
    event_type LowCardinality(String),
    total_amount AggregateFunction(sum, Decimal64(2)),
    event_count AggregateFunction(count, UInt64)
) ENGINE = AggregatingMergeTree()
ORDER BY (event_type, hour);

-- MV that populates on insert
CREATE MATERIALIZED VIEW hourly_stats_mv
TO hourly_stats AS
SELECT
    toStartOfHour(timestamp) AS hour,
    event_type,
    sumState(amount) AS total_amount,
    countState() AS event_count
FROM raw_events
GROUP BY hour, event_type;
```

## Distributed Table Pattern

```sql
-- Local table on each shard
CREATE TABLE events_local ON CLUSTER '{cluster}'
(/* columns */)
ENGINE = ReplicatedMergeTree('/clickhouse/{cluster}/events', '{replica}')
ORDER BY (event_type, user_id, event_time);

-- Distributed table for cross-shard queries
CREATE TABLE events_distributed ON CLUSTER '{cluster}'
AS events_local
ENGINE = Distributed('{cluster}', default, events_local, rand());
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Database | snake_case | `analytics`, `event_store` |
| Table | snake_case, plural | `raw_events`, `daily_metrics` |
| Column | snake_case | `user_id`, `event_time` |
| MV | `{target}_mv` | `hourly_stats_mv` |
| Distributed table | `{base}_distributed` | `events_distributed` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Frequent single-row inserts | High overhead per insert | Batch inserts (1000+ rows) |
| `Nullable` in ORDER BY | Degrades sort key performance | Use default values instead |
| JOINs on large tables | Memory-intensive | Pre-join at ingestion |
| `SELECT *` | Reads all columns, defeats columnar benefit | Select only needed columns |
| No partition pruning | Full table scan | Include partition key in WHERE |
| String-typed enums | Poor compression | Use `LowCardinality(String)` or `Enum8` |
