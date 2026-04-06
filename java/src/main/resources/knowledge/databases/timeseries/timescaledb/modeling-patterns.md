# TimescaleDB — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **2.13** | Continuous aggregates GA, compression | Stable |
| **2.14** | Improved compression, cagg-on-cagg | Current stable |
| **2.15+** | Enhanced vectorized aggregation | Latest |

## Core Concepts

TimescaleDB is a PostgreSQL extension for time-series workloads. All PostgreSQL features are available.

| Concept | Detail |
|---------|--------|
| Hypertable | A virtual table automatically partitioned by time |
| Chunk | A partition of a hypertable (one chunk per time interval) |
| Continuous aggregate | Materialized view with incremental refresh |
| Compression | Column-oriented compression on old chunks |

## Hypertable Design

### Creating a Hypertable

```sql
-- Create regular table first
CREATE TABLE metrics (
    time TIMESTAMPTZ NOT NULL,
    sensor_id INTEGER NOT NULL,
    location TEXT NOT NULL,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    pressure DOUBLE PRECISION
);

-- Convert to hypertable (partition by time)
SELECT create_hypertable('metrics', by_range('time'));

-- With custom chunk interval (default: 7 days)
SELECT create_hypertable('metrics', by_range('time',
    INTERVAL '1 day'));
```

### Chunk Interval Selection

| Data Volume (per day) | Recommended Chunk Interval | Reason |
|----------------------|---------------------------|--------|
| < 100 MB | 7 days | Reduce chunk overhead |
| 100 MB - 1 GB | 1 day | Balance between query and management |
| 1 GB - 10 GB | 1 day | Standard interval |
| > 10 GB | 6 hours - 12 hours | Keep chunks < 1 GB compressed |

**Rule:** Target uncompressed chunk size of 1-5 GB. Adjust interval based on ingest rate.

### Multi-Dimensional Partitioning

```sql
-- Partition by time AND space dimension
SELECT create_hypertable('metrics', by_range('time'),
    by_hash('sensor_id', 4));
```

Use space partitioning only when data exceeds single-node capacity or for multi-node setups.

## Column Type Conventions

| Data | PostgreSQL Type | Notes |
|------|----------------|-------|
| Timestamp | `TIMESTAMPTZ NOT NULL` | Always timezone-aware, always NOT NULL |
| Series identifier | `INTEGER` or `TEXT` | sensor_id, device_id |
| Tag/metadata | `TEXT` | location, region, environment |
| Measured values | `DOUBLE PRECISION` or `BIGINT` | float for measurements, int for counters |
| Status | `TEXT` or `SMALLINT` | Low-cardinality categorical |
| JSON payload | `JSONB` | For semi-structured data |

## Continuous Aggregates

Pre-computed rollups with incremental refresh:

```sql
-- Create continuous aggregate (hourly rollup)
CREATE MATERIALIZED VIEW metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    sensor_id,
    location,
    AVG(temperature) AS avg_temp,
    MIN(temperature) AS min_temp,
    MAX(temperature) AS max_temp,
    COUNT(*) AS sample_count
FROM metrics
GROUP BY bucket, sensor_id, location
WITH NO DATA;

-- Set refresh policy
SELECT add_continuous_aggregate_policy('metrics_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset   => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');
```

### Cascading Aggregates (Aggregate of Aggregate)

```sql
-- Daily aggregate from hourly (2.14+)
CREATE MATERIALIZED VIEW metrics_daily
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', bucket) AS bucket,
    sensor_id,
    AVG(avg_temp) AS avg_temp,
    MIN(min_temp) AS min_temp,
    MAX(max_temp) AS max_temp,
    SUM(sample_count) AS sample_count
FROM metrics_hourly
GROUP BY 1, 2
WITH NO DATA;
```

## Compression

```sql
-- Enable compression on hypertable
ALTER TABLE metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'sensor_id, location',
    timescaledb.compress_orderby = 'time DESC'
);

-- Add compression policy (compress chunks older than 7 days)
SELECT add_compression_policy('metrics', INTERVAL '7 days');
```

### Compression Design

| Parameter | Rule |
|-----------|------|
| `compress_segmentby` | Columns used in WHERE/GROUP BY (low cardinality) |
| `compress_orderby` | Time column DESC (primary access pattern) |
| Compression ratio | Typical 10-20x for time-series data |
| Compressed chunk access | Decompression transparent to queries |

## Retention Policy

```sql
-- Automatic chunk deletion (drop chunks older than 90 days)
SELECT add_retention_policy('metrics', INTERVAL '90 days');

-- Manual chunk management
SELECT drop_chunks('metrics', older_than => INTERVAL '90 days');
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Hypertable | snake_case, plural | `metrics`, `sensor_readings` |
| Column | snake_case | `sensor_id`, `created_at` |
| Continuous aggregate | `{table}_{interval}` | `metrics_hourly`, `metrics_daily` |
| Index | `idx_{table}_{columns}` | `idx_metrics_sensor_time` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No time column as NOT NULL | Hypertable requires NOT NULL time | Always `TIMESTAMPTZ NOT NULL` |
| Very small chunks (< 10 MB) | Excessive chunk overhead | Increase chunk interval |
| No compression policy | Unbounded storage growth | Enable compression for old chunks |
| `segmentby` high-cardinality column | Poor compression | Use low-cardinality columns |
| No continuous aggregates | Slow dashboard queries | Pre-compute rollups |
| Missing retention policy | Disk exhaustion | Set retention for all hypertables |
