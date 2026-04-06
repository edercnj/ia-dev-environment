# TimescaleDB — Migration Patterns

## Overview

TimescaleDB is a PostgreSQL extension. Standard PostgreSQL migration tools work with TimescaleDB-specific DDL:

| Tool | Support | Notes |
|------|---------|-------|
| **Flyway** | Full | PostgreSQL JDBC driver, custom SQL for hypertable DDL |
| **Liquibase** | Full | PostgreSQL JDBC driver |
| **golang-migrate** | Full | PostgreSQL driver |
| **Atlas** | Partial | Custom schema introspection for hypertables |

## Extension Installation

```sql
-- V001__install_timescaledb.sql
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Verify installation
SELECT extversion FROM pg_extension WHERE extname = 'timescaledb';
```

## Hypertable Migrations

### Create Hypertable

```sql
-- V002__create_metrics_hypertable.sql
CREATE TABLE IF NOT EXISTS metrics (
    time TIMESTAMPTZ NOT NULL,
    sensor_id INTEGER NOT NULL,
    location TEXT NOT NULL,
    temperature DOUBLE PRECISION,
    humidity DOUBLE PRECISION,
    pressure DOUBLE PRECISION
);

SELECT create_hypertable('metrics', by_range('time'),
    if_not_exists => TRUE);
```

### Convert Existing Table to Hypertable

```sql
-- V003__convert_events_to_hypertable.sql
-- Table must exist with a time column
SELECT create_hypertable('events', by_range('event_time'),
    migrate_data => TRUE,
    if_not_exists => TRUE);
```

**Rule:** Use `migrate_data => TRUE` when converting a table that already contains data. This operation locks the table during migration.

## ALTER TABLE Operations

### Add Column

```sql
-- V004__add_battery_level.sql
ALTER TABLE metrics
    ADD COLUMN IF NOT EXISTS battery_level DOUBLE PRECISION;
```

### Modify Chunk Interval

```sql
-- V005__adjust_chunk_interval.sql
SELECT set_chunk_time_interval('metrics', INTERVAL '1 day');
```

Note: Only affects new chunks. Existing chunks retain their original interval.

## Compression Migrations

### Enable Compression

```sql
-- V006__enable_metrics_compression.sql
ALTER TABLE metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'sensor_id, location',
    timescaledb.compress_orderby = 'time DESC'
);

SELECT add_compression_policy('metrics',
    compress_after => INTERVAL '7 days',
    if_not_exists => TRUE);
```

### Modify Compression Settings

```sql
-- V007__update_compression_settings.sql
-- Must decompress affected chunks first
SELECT decompress_chunk(c, if_compressed => TRUE)
FROM show_chunks('metrics',
    older_than => INTERVAL '7 days') AS c;

-- Update settings
ALTER TABLE metrics SET (
    timescaledb.compress_segmentby = 'sensor_id, location, region',
    timescaledb.compress_orderby = 'time DESC'
);

-- Recompress
SELECT compress_chunk(c, if_not_compressed => TRUE)
FROM show_chunks('metrics',
    older_than => INTERVAL '7 days') AS c;
```

## Continuous Aggregate Migrations

### Create Continuous Aggregate

```sql
-- V008__create_metrics_hourly.sql
CREATE MATERIALIZED VIEW IF NOT EXISTS metrics_hourly
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

SELECT add_continuous_aggregate_policy('metrics_hourly',
    start_offset => INTERVAL '3 hours',
    end_offset   => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);
```

### Drop and Recreate (Schema Change)

```sql
-- V009__recreate_metrics_hourly.sql
-- Remove policy first
SELECT remove_continuous_aggregate_policy('metrics_hourly',
    if_exists => TRUE);

-- Drop old aggregate
DROP MATERIALIZED VIEW IF EXISTS metrics_hourly;

-- Create with updated schema
CREATE MATERIALIZED VIEW metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    sensor_id,
    location,
    AVG(temperature) AS avg_temp,
    PERCENTILE_CONT(0.95) WITHIN GROUP
        (ORDER BY temperature) AS p95_temp,
    COUNT(*) AS sample_count
FROM metrics
GROUP BY bucket, sensor_id, location
WITH NO DATA;
```

## Retention Policy Migrations

```sql
-- V010__add_retention_policy.sql
SELECT add_retention_policy('metrics',
    drop_after => INTERVAL '90 days',
    if_not_exists => TRUE);

-- Update retention
SELECT remove_retention_policy('metrics', if_exists => TRUE);
SELECT add_retention_policy('metrics',
    drop_after => INTERVAL '180 days');
```

## Index Migrations

```sql
-- V011__create_metrics_indexes.sql
-- Composite index on time + sensor (covers most queries)
CREATE INDEX IF NOT EXISTS idx_metrics_sensor_time
    ON metrics (sensor_id, time DESC);

-- BRIN index for time-ordered data (small, efficient)
CREATE INDEX IF NOT EXISTS idx_metrics_time_brin
    ON metrics USING BRIN (time);
```

## Migration File Structure

```
db/migration/
|-- V001__install_timescaledb.sql
|-- V002__create_metrics_hypertable.sql
|-- V003__enable_compression.sql
|-- V004__create_continuous_aggregates.sql
|-- V005__add_retention_policies.sql
+-- V006__add_indexes.sql
```

## Migration Rules

| Rule | Detail |
|------|--------|
| Extension first | Install timescaledb extension before any hypertable DDL |
| Idempotent | Use `if_not_exists` / `IF NOT EXISTS` on all operations |
| Compression before retention | Enable compression to reduce storage before retention drops data |
| Test chunk intervals | Verify chunk sizes in staging with production-like data rates |
| Cagg recreation for schema changes | Continuous aggregates cannot be altered; drop and recreate |
| Policy ordering | Compression policy interval < retention policy interval |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| DDL without `if_not_exists` | Migration fails on re-run | All DDL must be idempotent |
| `migrate_data` on large table without downtime | Table locked during migration | Plan maintenance window |
| Compression without decompress for ALTER | Error on compressed chunks | Decompress, alter, recompress |
| Cagg ALTER attempt | Not supported | Drop and recreate |
| Missing extension check | Migration fails without extension | Install extension in first migration |
