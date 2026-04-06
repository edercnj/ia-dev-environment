# CockroachDB — Migration Patterns

## Overview

CockroachDB supports online schema changes natively. Standard PostgreSQL migration tools work:

| Tool | Support | Notes |
|------|---------|-------|
| **Flyway** | Full | Use PostgreSQL JDBC driver, CockroachDB dialect |
| **Liquibase** | Full | Use PostgreSQL JDBC driver |
| **golang-migrate** | Full | cockroachdb driver |
| **Atlas** | Full | CockroachDB-aware dialect |

## Online Schema Changes

CockroachDB performs schema changes **online** without blocking reads or writes:

| Operation | Online? | Notes |
|-----------|---------|-------|
| `CREATE TABLE` | Yes | Instant |
| `DROP TABLE` | Yes | Instant (GC async) |
| `ADD COLUMN` | Yes | Backfill runs in background |
| `DROP COLUMN` | Yes | Background cleanup |
| `ADD COLUMN ... DEFAULT` | Yes | Backfill with default value |
| `CREATE INDEX` | Yes | Background index backfill |
| `DROP INDEX` | Yes | Instant |
| `ALTER COLUMN TYPE` | Partially | Some conversions require rewrite |
| `ADD CONSTRAINT` | Yes | Background validation |
| `RENAME TABLE/COLUMN` | Yes | Metadata-only |

### DDL Examples

```sql
-- Add column (online, no blocking)
ALTER TABLE orders ADD COLUMN notes STRING;

-- Add column with default (backfill in background)
ALTER TABLE orders ADD COLUMN priority INT DEFAULT 0;

-- Create index (online backfill)
CREATE INDEX idx_orders_status ON orders (status);

-- Add foreign key (validates in background)
ALTER TABLE order_items
    ADD CONSTRAINT fk_order
    FOREIGN KEY (order_id) REFERENCES orders (id);
```

## Schema Change Monitoring

```sql
-- View running schema changes
SELECT * FROM [SHOW JOBS]
WHERE job_type = 'SCHEMA CHANGE'
  AND status = 'running';

-- Check schema change progress
SHOW JOBS WHEN COMPLETE (SELECT job_id FROM [SHOW JOBS]
    WHERE job_type = 'SCHEMA CHANGE');
```

## Changefeed for CDC

Changefeeds stream row-level changes to external systems:

```sql
-- Create changefeed to Kafka
CREATE CHANGEFEED FOR TABLE orders, order_items
    INTO 'kafka://broker:9092'
    WITH updated, resolved = '10s',
         format = avro,
         confluent_schema_registry = 'http://registry:8081';

-- Changefeed to cloud storage
CREATE CHANGEFEED FOR TABLE orders
    INTO 's3://bucket/cdc?AWS_ACCESS_KEY_ID=key&AWS_SECRET_ACCESS_KEY=secret'
    WITH updated, resolved = '30s';

-- Changefeed to webhook
CREATE CHANGEFEED FOR TABLE orders
    INTO 'webhook-https://api.example.com/events'
    WITH updated;
```

### Changefeed During Migrations

| Rule | Detail |
|------|--------|
| Schema changes | Changefeeds continue through online schema changes |
| New columns | Appear in changefeed output after backfill completes |
| Dropped columns | Disappear from changefeed output after DDL |
| Table drops | Changefeed fails; must be recreated |

## Hash-Sharded Index Migration

Convert sequential indexes to hash-sharded to eliminate hot spots:

```sql
-- Create hash-sharded index
CREATE INDEX idx_logs_time
    ON logs (log_time)
    USING HASH WITH (bucket_count = 8);

-- Hash-sharded primary key
CREATE TABLE time_series (
    ts TIMESTAMPTZ NOT NULL,
    sensor_id UUID NOT NULL,
    value FLOAT8,
    PRIMARY KEY (ts, sensor_id) USING HASH
);
```

## Multi-Region Migration

```sql
-- Step 1: Add regions to database
ALTER DATABASE myapp SET PRIMARY REGION "us-east1";
ALTER DATABASE myapp ADD REGION "eu-west1";
ALTER DATABASE myapp ADD REGION "ap-southeast1";

-- Step 2: Convert tables to regional
ALTER TABLE users SET LOCALITY REGIONAL BY ROW;

-- Step 3: Backfill region column
UPDATE users SET region = 'us-east1'
    WHERE region IS NULL;

-- Step 4: Global reference tables
ALTER TABLE countries SET LOCALITY GLOBAL;
```

## Backup and Restore

```sql
-- Full backup to cloud storage
BACKUP DATABASE myapp
    INTO 's3://bucket/backups'
    WITH revision_history;

-- Incremental backup
BACKUP DATABASE myapp
    INTO LATEST IN 's3://bucket/backups'
    WITH revision_history;

-- Point-in-time restore
RESTORE DATABASE myapp
    FROM LATEST IN 's3://bucket/backups'
    AS OF SYSTEM TIME '-1h';
```

## Migration File Structure

```
db/migration/
|-- V001__create_orders_table.sql
|-- V002__create_order_items_table.sql
|-- V003__add_hash_sharded_index.sql
|-- V004__setup_multi_region.sql
|-- V005__create_changefeed.sql
+-- V006__add_global_reference_tables.sql
```

## Migration Rules

| Rule | Detail |
|------|--------|
| Online by default | CockroachDB DDL is non-blocking; no special syntax needed |
| Monitor jobs | Check `SHOW JOBS` for schema change progress |
| Avoid large backfills | Schema changes on billion-row tables take time |
| Test with production volume | Schema change duration depends on data size |
| Changefeed awareness | Schema changes affect changefeed output format |
| Region planning | Multi-region setup should be done before data loading |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Blocking DDL assumptions | CockroachDB DDL is non-blocking (unlike PostgreSQL) | No need for `CONCURRENTLY` |
| Multiple schema changes at once | Queued sequentially, slow completion | One major schema change at a time |
| Ignoring job status | Schema changes may fail silently | Monitor with `SHOW JOBS` |
| Manual range splits | Automatic splitting is usually sufficient | Use hash-sharded indexes instead |
| DDL in transactions | Some DDL cannot be in explicit transactions | Use implicit transactions for DDL |
