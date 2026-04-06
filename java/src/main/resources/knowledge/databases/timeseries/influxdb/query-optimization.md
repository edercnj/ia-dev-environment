# InfluxDB — Query Optimization

## Query Languages

| Language | Version | Best For |
|----------|---------|----------|
| **Flux** | 2.x (default) | Complex transformations, joins, tasks |
| **InfluxQL** | 1.x + 2.x (DBRP) | Simple queries, v1 compatibility |
| **SQL** | 3.x / Cloud | Standard SQL syntax, DataFusion engine |

## Flux Query Optimization

### Push-Down Filters

Filters that execute at the storage layer for maximum performance:

```flux
// Good: pushdown-eligible filters
from(bucket: "metrics_raw")
    |> range(start: -1h)                    // ALWAYS required
    |> filter(fn: (r) =>
        r._measurement == "cpu" and        // Measurement filter
        r.host == "server-01" and          // Tag filter
        r._field == "usage_user")          // Field filter
    |> mean()

// Bad: non-pushdown operations before filter
from(bucket: "metrics_raw")
    |> range(start: -1h)
    |> map(fn: (r) => ({r with _value: r._value * 100.0}))
    |> filter(fn: (r) => r._value > 50.0)  // Filter after transform
```

### Pushdown-Eligible Operations

| Operation | Pushed Down? | Notes |
|-----------|-------------|-------|
| `range()` | Yes (required) | Must be first after `from()` |
| `filter()` on tags | Yes | Tag predicates |
| `filter()` on `_measurement` | Yes | Measurement selection |
| `filter()` on `_field` | Yes | Field selection |
| `filter()` on `_value` | No | Value filtering in memory |
| `count()`, `sum()`, `mean()` | Partially | Simple aggregates can be pushed |
| `group()` | No | In-memory operation |
| `map()` | No | In-memory transformation |

**Rule:** Place `range()`, then tag/measurement/field filters BEFORE any transformations. Order matters for pushdown optimization.

### Aggregate Window

```flux
// Efficient: downsample then aggregate
from(bucket: "metrics_raw")
    |> range(start: -24h)
    |> filter(fn: (r) => r._measurement == "cpu")
    |> aggregateWindow(every: 5m, fn: mean, createEmpty: false)

// Multiple aggregates in one pass
from(bucket: "metrics_raw")
    |> range(start: -24h)
    |> filter(fn: (r) => r._measurement == "http_requests")
    |> aggregateWindow(every: 1h, fn: sum, createEmpty: false)
```

### Join Optimization

```flux
// Join two measurements
cpu = from(bucket: "metrics_raw")
    |> range(start: -1h)
    |> filter(fn: (r) => r._measurement == "cpu")
    |> aggregateWindow(every: 5m, fn: mean)

memory = from(bucket: "metrics_raw")
    |> range(start: -1h)
    |> filter(fn: (r) => r._measurement == "memory")
    |> aggregateWindow(every: 5m, fn: mean)

join(tables: {cpu: cpu, memory: memory}, on: ["_time", "host"])
```

**Rule:** Aggregate and filter each stream BEFORE joining. Never join raw high-resolution data.

## InfluxQL Optimization

```sql
-- Good: specific measurement, time filter, tag filter
SELECT mean("usage_user") FROM "cpu"
WHERE "host" = 'server-01'
  AND time >= now() - 1h
GROUP BY time(5m), "host"
FILL(none)

-- Bad: no time filter (scans entire shard group)
SELECT mean("usage_user") FROM "cpu"
GROUP BY time(5m)
```

## Storage Engine Tuning

### TSM Engine (v2)

| Parameter | Default | Tuning | Purpose |
|-----------|---------|--------|---------|
| `storage-cache-max-memory-size` | 1 GB | 2-4 GB for high write loads | In-memory write buffer |
| `storage-cache-snapshot-memory-size` | 25 MB | 100 MB for high throughput | Snapshot trigger threshold |
| `storage-compact-full-write-cold-duration` | 4h | 2h for faster compaction | Cold compaction interval |
| `storage-max-concurrent-compactions` | 0 (auto) | CPU cores / 2 | Parallel compactions |

### Shard Group Duration

| Retention | Recommended Shard Duration | Reason |
|-----------|---------------------------|--------|
| < 2 days | 1 hour | Fine-grained deletion |
| 2 days - 6 months | 1 day | Balance between query and retention |
| 6 months - 1 year | 7 days | Reduce shard count |
| > 1 year | 30 days | Minimize shard management overhead |

## Query Performance Patterns

### Efficient Time Range Selection

```flux
// Narrow time range = fewer shards read
from(bucket: "metrics_raw")
    |> range(start: -15m)      // Reads 1-2 shards
    |> filter(fn: (r) => r._measurement == "cpu")

// Wide time range = many shards read
from(bucket: "metrics_raw")
    |> range(start: -365d)     // Reads hundreds of shards
    |> filter(fn: (r) => r._measurement == "cpu")
```

### Pre-Computed Rollups

Query rollup buckets instead of raw data for long time ranges:

```flux
// Dashboard: last 7 days -> use 5m rollup
from(bucket: "metrics_rollup_5m")
    |> range(start: -7d)
    |> filter(fn: (r) => r._measurement == "cpu")

// Dashboard: last 30 days -> use 1h rollup
from(bucket: "metrics_rollup_1h")
    |> range(start: -30d)
    |> filter(fn: (r) => r._measurement == "cpu")
```

## Monitoring Query Performance

```flux
// Check query execution statistics
from(bucket: "_monitoring")
    |> range(start: -1h)
    |> filter(fn: (r) =>
        r._measurement == "query_log" and
        r._field == "duration_ms")
    |> filter(fn: (r) => r._value > 1000)
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Missing `range()` | Scans all shards | Always specify time range |
| Filter after transform | No pushdown optimization | Filter before `map()` or `pivot()` |
| Joining raw data | Memory explosion | Aggregate before joining |
| Long-range on raw bucket | Reads millions of points | Use rollup buckets for > 1 day |
| `GROUP BY *` | Unbounded grouping | Specify explicit group keys |
| No `createEmpty: false` | Generates null-filled rows | Add to `aggregateWindow` |
