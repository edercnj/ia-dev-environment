# ClickHouse — Query Optimization

## Query Execution Pipeline

```
Parse -> Analyze -> Plan -> Execute (vectorized) -> Merge -> Return
```

Each stage operates on columns in blocks (default 65,536 rows per block).

## EXPLAIN and Query Analysis

### EXPLAIN Plan

```sql
EXPLAIN
SELECT event_type, count() AS cnt
FROM events
WHERE event_date = '2024-01-15'
GROUP BY event_type
ORDER BY cnt DESC;
```

### EXPLAIN PIPELINE

```sql
EXPLAIN PIPELINE
SELECT event_type, count() AS cnt
FROM events
WHERE event_date = '2024-01-15'
GROUP BY event_type;
```

Shows parallel execution threads and data flow between processors.

### Query Log Analysis

```sql
SELECT query, read_rows, read_bytes, memory_usage,
       query_duration_ms, result_rows
FROM system.query_log
WHERE type = 'QueryFinish'
  AND query_duration_ms > 1000
ORDER BY query_duration_ms DESC
LIMIT 20;
```

## PREWHERE Optimization

`PREWHERE` reads only filter columns first, then loads remaining columns for matched rows:

```sql
-- Automatic (ClickHouse moves light filters to PREWHERE)
SELECT user_id, event_type, payload
FROM events
WHERE event_type = 'purchase' AND user_id = 12345;

-- Explicit PREWHERE for control
SELECT user_id, event_type, payload
FROM events
PREWHERE event_type = 'purchase'
WHERE user_id = 12345;
```

| Rule | Detail |
|------|--------|
| Automatic PREWHERE | Enabled by default; moves selective filters |
| Manual PREWHERE | Use when auto-selection is suboptimal |
| PREWHERE candidates | Low-selectivity columns (few distinct values) |
| Avoid in PREWHERE | Heavy columns (large strings, arrays) |

## Partition Pruning

Queries MUST include partition key columns in WHERE to skip irrelevant partitions:

```sql
-- Good: partition pruning on event_date (partition key)
SELECT count() FROM events
WHERE event_date >= '2024-01-01' AND event_date < '2024-02-01';

-- Bad: no partition pruning (scans all partitions)
SELECT count() FROM events
WHERE event_type = 'purchase';
```

### Verify Partition Pruning

```sql
-- Check which parts are read
SELECT partition, rows, bytes_on_disk
FROM system.parts
WHERE table = 'events' AND active;
```

## Primary Key and Sort Order

ClickHouse primary key is a sparse index (not unique constraint):

| Concept | Detail |
|---------|--------|
| Granularity | Every N-th row indexed (default 8192) |
| Index blocks | Binary search on sparse index, then scan granule |
| Sort order impact | First column in ORDER BY = most effective filter |

```sql
-- Optimal: filter on first sort key column
SELECT * FROM events WHERE event_type = 'purchase';

-- Suboptimal: filter on second sort key column only
SELECT * FROM events WHERE user_id = 12345;
```

**Rule:** ORDER BY column order MUST match query filter priority: most-filtered column first.

## Skipping Indexes

Secondary indexes that skip data blocks not matching the filter:

```sql
-- MinMax index (good for range queries)
ALTER TABLE events ADD INDEX idx_amount amount
    TYPE minmax GRANULARITY 4;

-- Set index (good for equality on low-cardinality)
ALTER TABLE events ADD INDEX idx_region region
    TYPE set(100) GRANULARITY 4;

-- Bloom filter (good for equality on high-cardinality)
ALTER TABLE events ADD INDEX idx_user_id user_id
    TYPE bloom_filter(0.01) GRANULARITY 4;

-- Token bloom filter (good for string token search)
ALTER TABLE events ADD INDEX idx_payload payload
    TYPE tokenbf_v1(10240, 3, 0) GRANULARITY 4;
```

| Index Type | Best For | Granularity Rule |
|-----------|----------|-----------------|
| minmax | Range queries on numeric/date | 4-8 |
| set | Low-cardinality equality | 4-8 |
| bloom_filter | High-cardinality equality | 2-4 |
| tokenbf_v1 | Token substring search | 2-4 |
| ngrambf_v1 | N-gram substring search | 2-4 |

## Materialized Columns

Pre-computed columns stored on disk, computed at insert time:

```sql
ALTER TABLE events ADD COLUMN event_hour DateTime
    MATERIALIZED toStartOfHour(event_time);

ALTER TABLE events ADD COLUMN amount_bucket UInt8
    MATERIALIZED multiIf(
        amount < 10, 1,
        amount < 100, 2,
        amount < 1000, 3,
        4
    );
```

## JOIN Optimization

| Strategy | When to Use | Syntax |
|----------|-------------|--------|
| Hash JOIN (default) | Right table fits in memory | `SELECT ... FROM big JOIN small ON ...` |
| Partial merge JOIN | Both tables too large for memory | `SET join_algorithm = 'partial_merge'` |
| Direct JOIN (Dictionary) | Static dimension lookup | `dictGet('dim_name', 'col', key)` |

```sql
-- Dictionary for dimension lookups (avoids JOIN)
CREATE DICTIONARY country_dict (
    code String,
    name String,
    region String
) PRIMARY KEY code
SOURCE(CLICKHOUSE(TABLE 'countries' DB 'default'))
LAYOUT(HASHED())
LIFETIME(MIN 3600 MAX 7200);

-- Use in query
SELECT event_type,
       dictGet('country_dict', 'name', country_code) AS country
FROM events;
```

## Performance Settings

| Setting | Default | Tuning | Purpose |
|---------|---------|--------|---------|
| `max_threads` | CPU cores | Adjust per query complexity | Parallelism |
| `max_memory_usage` | 10 GB | Set per-query limit | Prevent OOM |
| `max_bytes_before_external_group_by` | 0 | Set to 50% of `max_memory_usage` | Spill to disk |
| `optimize_read_in_order` | 1 | Keep enabled | ORDER BY using sort key |
| `use_uncompressed_cache` | 0 | Enable for repeated queries | Cache decompressed blocks |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| `SELECT *` | Reads all columns | Select only needed columns |
| Missing partition filter | Full table scan | Always include partition key in WHERE |
| Large right-side JOINs | OOM on hash table build | Use partial_merge or dictionaries |
| ORDER BY without LIMIT | Sorts entire result set | Always combine ORDER BY with LIMIT |
| Subquery instead of JOIN | Poor optimization | Rewrite as explicit JOIN |
| No PREWHERE awareness | Reads unnecessary data | Profile queries and check PREWHERE usage |
