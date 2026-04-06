# TimescaleDB — Query Optimization

## Time-Bucket Queries

The `time_bucket` function is the primary tool for time-series aggregation:

```sql
-- Hourly aggregation
SELECT
    time_bucket('1 hour', time) AS hour,
    sensor_id,
    AVG(temperature) AS avg_temp,
    MAX(temperature) AS max_temp,
    COUNT(*) AS samples
FROM metrics
WHERE time >= NOW() - INTERVAL '24 hours'
  AND sensor_id = 42
GROUP BY hour, sensor_id
ORDER BY hour DESC;

-- Custom bucket with offset
SELECT
    time_bucket('15 minutes', time, INTERVAL '5 minutes') AS bucket,
    AVG(temperature) AS avg_temp
FROM metrics
WHERE time >= NOW() - INTERVAL '6 hours'
GROUP BY bucket
ORDER BY bucket;
```

## Chunk Exclusion

TimescaleDB automatically excludes chunks outside the query's time range:

```sql
-- Good: chunk exclusion (reads only relevant chunks)
SELECT * FROM metrics
WHERE time >= NOW() - INTERVAL '1 hour'
  AND sensor_id = 42;

-- Bad: no time filter (reads all chunks)
SELECT * FROM metrics
WHERE sensor_id = 42;

-- Verify chunk exclusion with EXPLAIN
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM metrics
WHERE time >= NOW() - INTERVAL '1 hour';
```

### Key EXPLAIN Indicators

| Indicator | Good | Bad |
|-----------|------|-----|
| Chunks scanned | 1-2 (recent) | All chunks |
| Scan type | Index Scan / Index Only Scan | Seq Scan on large chunks |
| `Buffers: shared hit` | High hit ratio | Many `shared read` (disk I/O) |
| Chunk exclusion | `Chunks excluded: N` | `Chunks excluded: 0` |

**Rule:** Every production query MUST include a `WHERE time >= ...` clause to enable chunk exclusion. Queries without time filters scan the entire hypertable.

## Continuous Aggregate Queries

Query pre-computed rollups instead of raw data:

```sql
-- Dashboard: last 7 days -> query hourly aggregate
SELECT bucket, sensor_id, avg_temp, max_temp
FROM metrics_hourly
WHERE bucket >= NOW() - INTERVAL '7 days'
  AND sensor_id = 42
ORDER BY bucket DESC;

-- Dashboard: last 30 days -> query daily aggregate
SELECT bucket, sensor_id, avg_temp
FROM metrics_daily
WHERE bucket >= NOW() - INTERVAL '30 days'
ORDER BY bucket;
```

### Resolution Selection

| Time Range | Resolution | Query Source |
|-----------|-----------|-------------|
| Last 1 hour | Raw data | `metrics` |
| Last 24 hours | 5-minute rollup | `metrics_5m` |
| Last 7 days | 1-hour rollup | `metrics_hourly` |
| Last 30 days | 1-day rollup | `metrics_daily` |
| Last year | 1-day rollup | `metrics_daily` |

## JOIN Optimization

### Dimension Table Joins

```sql
-- Join time-series with metadata (small dimension table)
SELECT m.time, m.temperature, s.name, s.location
FROM metrics m
JOIN sensors s ON m.sensor_id = s.id
WHERE m.time >= NOW() - INTERVAL '1 hour'
  AND s.location = 'building-a'
ORDER BY m.time DESC;
```

**Rule:** Always filter on time BEFORE the JOIN. TimescaleDB eliminates chunks first, then joins against the dimension table.

### Lateral Join for "Last N per Group"

```sql
-- Last reading per sensor
SELECT s.id, s.name, m.*
FROM sensors s
CROSS JOIN LATERAL (
    SELECT time, temperature, humidity
    FROM metrics
    WHERE sensor_id = s.id
      AND time >= NOW() - INTERVAL '1 hour'
    ORDER BY time DESC
    LIMIT 1
) m;
```

## Compression Query Performance

### Compressed Chunk Queries

```sql
-- Queries on compressed data are transparent
SELECT time_bucket('1 day', time) AS day,
       AVG(temperature) AS avg_temp
FROM metrics
WHERE time >= NOW() - INTERVAL '90 days'
  AND sensor_id = 42
GROUP BY day
ORDER BY day;
```

| Aspect | Compressed | Uncompressed |
|--------|-----------|--------------|
| Storage | 10-20x smaller | Full size |
| Sequential scan | Faster (less I/O) | Standard speed |
| Point lookups | Decompression overhead | Fast |
| Inserts | Must decompress chunk first | Direct insert |

### Segmentby Optimization

```sql
-- Fast: filter on segmentby column (reads one segment)
SELECT * FROM metrics
WHERE sensor_id = 42
  AND time >= NOW() - INTERVAL '30 days';

-- Slow: filter on non-segmentby column (decompresses all segments)
SELECT * FROM metrics
WHERE temperature > 30.0
  AND time >= NOW() - INTERVAL '30 days';
```

## Index Strategies

```sql
-- Composite index (sensor + time, most common query pattern)
CREATE INDEX idx_metrics_sensor_time
    ON metrics (sensor_id, time DESC);

-- Partial index (only recent data, reduces index size)
CREATE INDEX idx_metrics_recent
    ON metrics (sensor_id, time DESC)
    WHERE time > NOW() - INTERVAL '7 days';

-- BRIN index (compact, good for time-ordered data)
CREATE INDEX idx_metrics_time_brin
    ON metrics USING BRIN (time)
    WITH (pages_per_range = 32);
```

| Index Type | Size | Best For |
|-----------|------|----------|
| B-tree | Larger | Point lookups, range scans with complex filters |
| BRIN | Very small | Time-ordered scans, chunk-level filtering |

## Hyperfunctions

```sql
-- Time-weighted average
SELECT sensor_id,
       average(time_weight('LOCF', time, temperature))
FROM metrics
WHERE time >= NOW() - INTERVAL '1 hour'
GROUP BY sensor_id;

-- Approximate percentile (fast)
SELECT sensor_id,
       approx_percentile(0.95, percentile_agg(temperature))
           AS p95_temp
FROM metrics
WHERE time >= NOW() - INTERVAL '1 hour'
GROUP BY sensor_id;

-- Gap filling
SELECT time_bucket_gapfill('5 minutes', time) AS bucket,
       sensor_id,
       locf(AVG(temperature)) AS temperature
FROM metrics
WHERE time >= NOW() - INTERVAL '1 hour'
GROUP BY bucket, sensor_id;
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No time filter | Scans all chunks | Always include `WHERE time >= ...` |
| Raw data for dashboards | Slow aggregation on large datasets | Use continuous aggregates |
| Non-segmentby filter on compressed | Full decompression | Include filter columns in `segmentby` |
| Missing composite index | Seq scan per chunk | Create `(sensor_id, time DESC)` index |
| `ORDER BY time ASC` on DESC index | Index not usable | Match ORDER direction to index |
| Large result set without LIMIT | Memory exhaustion | Always paginate time-series queries |
