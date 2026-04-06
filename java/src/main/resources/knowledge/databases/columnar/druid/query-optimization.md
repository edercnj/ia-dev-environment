# Apache Druid — Query Optimization

## Query Engines

| Engine | When Used | Strengths |
|--------|----------|-----------|
| **Native** | JSON-based queries via REST | Full feature set, fine-grained control |
| **Druid SQL** | SQL queries via JDBC or REST | Familiar syntax, translated to native |
| **MSQ (Multi-Stage Query)** | SQL for batch ingestion and large queries | Handles large result sets, distributed execution |

## Druid SQL Queries

### Time Filtering (Critical)

Every query MUST filter on `__time` to enable segment pruning:

```sql
-- Good: segment pruning on __time
SELECT page, SUM(view_count) AS total_views
FROM page_views
WHERE __time >= TIMESTAMP '2024-01-01'
  AND __time < TIMESTAMP '2024-02-01'
GROUP BY page
ORDER BY total_views DESC
LIMIT 100;

-- Bad: no time filter (scans all segments)
SELECT page, SUM(view_count) FROM page_views GROUP BY page;
```

### Time Floor for Bucketing

```sql
SELECT
  TIME_FLOOR(__time, 'PT1H') AS hour,
  COUNT(*) AS events
FROM page_views
WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '7' DAY
GROUP BY TIME_FLOOR(__time, 'PT1H')
ORDER BY hour;
```

### Approximate Queries

Druid excels at approximate aggregations for high-cardinality dimensions:

```sql
-- Approximate distinct count (HyperLogLog)
SELECT
  TIME_FLOOR(__time, 'P1D') AS day,
  APPROX_COUNT_DISTINCT_DS_HLL(user_id) AS unique_users
FROM page_views
WHERE __time >= TIMESTAMP '2024-01-01'
GROUP BY 1;

-- Approximate quantiles (Quantiles Sketch)
SELECT
  DS_QUANTILES_SKETCH(response_time) AS sketch
FROM api_requests
WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '1' HOUR;

-- Approximate top-N (faster than exact ORDER BY + LIMIT)
SELECT page, SUM(view_count) AS total
FROM page_views
WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '1' DAY
GROUP BY page
ORDER BY total DESC
LIMIT 10;
```

| Sketch Type | Function | Use Case | Error Rate |
|-------------|----------|----------|------------|
| HyperLogLog | `APPROX_COUNT_DISTINCT_DS_HLL` | Unique counts | ~2% |
| Theta Sketch | `APPROX_COUNT_DISTINCT_DS_THETA` | Set operations (union, intersect) | ~2% |
| Quantiles | `DS_QUANTILES_SKETCH` | Percentiles (p50, p95, p99) | Configurable |

## Native JSON Queries

### TopN Query (Fastest for Single-Dimension Ranking)

```json
{
  "queryType": "topN",
  "dataSource": "page_views",
  "dimension": "page",
  "metric": "total_views",
  "threshold": 10,
  "aggregations": [
    {"type": "longSum", "name": "total_views", "fieldName": "view_count"}
  ],
  "intervals": ["2024-01-01/2024-02-01"],
  "granularity": "all"
}
```

### GroupBy Query

```json
{
  "queryType": "groupBy",
  "dataSource": "page_views",
  "dimensions": ["page", "country"],
  "aggregations": [
    {"type": "longSum", "name": "total_views", "fieldName": "view_count"},
    {"type": "count", "name": "row_count"}
  ],
  "intervals": ["2024-01-01/2024-02-01"],
  "granularity": "day"
}
```

### Query Type Selection

| Query Type | Best For | Performance |
|-----------|----------|-------------|
| `timeseries` | Single metric over time, no dimensions | Fastest |
| `topN` | Single dimension ranking | Very fast (approximate) |
| `groupBy` | Multi-dimension aggregation | Moderate |
| `scan` | Raw row retrieval | Slowest, avoid for analytics |

## Tiered Storage

```json
{
  "type": "loadByPeriod",
  "period": "P30D",
  "tieredReplicants": {
    "hot": 2,
    "cold": 1
  }
}
```

| Tier | Storage | SSD | Use Case |
|------|---------|-----|----------|
| Hot | In-memory + SSD | Yes | Recent data, frequent queries |
| Cold | HDD / S3 | No | Historical data, infrequent queries |

## Query Context Parameters

| Parameter | Default | Tuning | Purpose |
|-----------|---------|--------|---------|
| `timeout` | 300000 | Set per-query | Query timeout (ms) |
| `maxScatterGatherBytes` | unlimited | Set limit | Prevent OOM on large results |
| `useCache` | true | Disable for real-time | Use segment-level cache |
| `populateCache` | true | Disable for one-off queries | Populate cache for future queries |
| `priority` | 0 | -1 for background queries | Query priority in queue |
| `vectorize` | true | Keep enabled | Vectorized execution |

```sql
-- Set context in SQL
SELECT /*+ SET_QUERY_CONTEXT(timeout=60000, priority=-1) */
  page, SUM(view_count) AS total
FROM page_views
WHERE __time >= CURRENT_TIMESTAMP - INTERVAL '1' DAY
GROUP BY page;
```

## Segment Caching

| Cache Level | Scope | Best For |
|-------------|-------|----------|
| Segment cache | Per-segment results | Repeated time-range queries |
| Result cache | Full query results | Dashboard auto-refresh |
| Broker cache | Cross-segment merge | Common filter patterns |

## Performance Monitoring

```sql
-- Slow queries
SELECT * FROM sys.tasks
WHERE status = 'RUNNING'
  AND duration > 30000;

-- Segment statistics
SELECT datasource, COUNT(*) AS segment_count,
       SUM("size") AS total_size
FROM sys.segments
WHERE is_active = 1
GROUP BY datasource;
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No `__time` filter | Scans all segments | Always filter on `__time` |
| Exact distinct count | Very expensive on high cardinality | Use `APPROX_COUNT_DISTINCT_DS_HLL` |
| `scan` query for analytics | Bypasses aggregation engine | Use `topN` or `groupBy` |
| Large `LIMIT` on groupBy | Memory-intensive merge | Use topN for ranking queries |
| `SELECT *` | Returns all dimensions and metrics | Select only needed columns |
| Querying non-published segments | Real-time segments are slower | Use `queryWaitForSegmentLoad` |
