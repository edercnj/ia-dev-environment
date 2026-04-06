# TiDB — Migration Patterns

## Overview

TiDB is MySQL wire-compatible, so standard MySQL migration tools work:

| Tool | Support | Notes |
|------|---------|-------|
| **Flyway** | Full | MySQL JDBC driver |
| **Liquibase** | Full | MySQL JDBC driver |
| **golang-migrate** | Full | MySQL driver |
| **gh-ost** | Not needed | TiDB DDL is online by default |
| **TiDB Lightning** | Bulk import | Fast parallel data import |

## DDL Operations

TiDB performs schema changes **online** using an internal DDL job queue:

| Operation | Online? | Notes |
|-----------|---------|-------|
| `CREATE TABLE` | Yes | Instant |
| `DROP TABLE` | Yes | Instant |
| `ADD COLUMN` | Yes | Online, no blocking |
| `DROP COLUMN` | Yes | Online reorganization |
| `ADD INDEX` | Yes | Background backfill (distributed) |
| `DROP INDEX` | Yes | Instant |
| `MODIFY COLUMN` | Partially | Type changes may rewrite data |
| `RENAME TABLE` | Yes | Metadata-only |
| `ADD FOREIGN KEY` | Yes | Background validation |
| `TRUNCATE TABLE` | Yes | Instant (new table ID) |

### DDL Examples

```sql
-- Add column (online)
ALTER TABLE orders ADD COLUMN notes TEXT;

-- Add column with default (backfill in background)
ALTER TABLE orders ADD COLUMN priority INT DEFAULT 0;

-- Add index (distributed backfill)
ALTER TABLE orders ADD INDEX idx_orders_status (status);

-- Modify column type (may rewrite)
ALTER TABLE orders MODIFY COLUMN notes VARCHAR(2000);
```

## DDL Job Management

```sql
-- View running DDL jobs
ADMIN SHOW DDL JOBS;

-- View specific DDL job
ADMIN SHOW DDL JOB QUERIES job_id;

-- Cancel a DDL job
ADMIN CANCEL DDL JOBS job_id;

-- Pause a DDL job (7.2+)
ADMIN PAUSE DDL JOBS job_id;

-- Resume a paused DDL job
ADMIN RESUME DDL JOBS job_id;
```

### DDL Concurrency

```sql
-- Set DDL concurrency (parallel DDL jobs)
SET GLOBAL tidb_ddl_reorg_worker_cnt = 8;

-- Set batch size for DDL reorganization
SET GLOBAL tidb_ddl_reorg_batch_size = 1024;
```

## TiDB Lightning (Bulk Import)

For large-scale data migration:

```toml
# tidb-lightning.toml
[lightning]
level = "info"

[tikv-importer]
backend = "local"  # Fastest: local sort + ingest
sorted-kv-dir = "/tmp/lightning-sorted"

[mydumper]
data-source-dir = "/data/export"

[tidb]
host = "tidb-server"
port = 4000
user = "root"
```

### Import Modes

| Mode | Speed | Impact | Use Case |
|------|-------|--------|----------|
| Local backend | Fastest | Blocks reads during import | Initial data load |
| TiDB backend | Moderate | No blocking | Online migration |
| Physical import | Very fast | Requires downtime | Large-scale migration |

## Batch DML Patterns

```sql
-- Non-transactional DML (7.0+, processes in batches)
BATCH ON id LIMIT 1000
DELETE FROM events WHERE created_at < '2023-01-01';

-- Batch update
BATCH ON id LIMIT 1000
UPDATE orders SET status = 'EXPIRED'
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL 30 DAY;
```

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `BATCH ON column` | Required | Column for batching (usually PK) |
| `LIMIT N` | 1000 | Rows per batch |
| `DRY RUN` | Off | Preview batch plan without executing |

**Rule:** Use `BATCH` DML for large updates/deletes. Single large transactions may exceed TiDB's transaction size limit (100 MB default).

## TiFlash Replica Management

```sql
-- Add TiFlash replica (data replicated asynchronously)
ALTER TABLE orders SET TIFLASH REPLICA 1;

-- Check TiFlash replication progress
SELECT TABLE_SCHEMA, TABLE_NAME, REPLICA_COUNT,
       AVAILABLE, PROGRESS
FROM information_schema.tiflash_replica;

-- Remove TiFlash replica
ALTER TABLE orders SET TIFLASH REPLICA 0;
```

## Migration File Structure

```
db/migration/
|-- V001__create_orders_table.sql
|-- V002__create_order_items_table.sql
|-- V003__add_auto_random_pk.sql
|-- V004__add_tiflash_replicas.sql
|-- V005__add_expression_index.sql
+-- V006__batch_cleanup_old_data.sql
```

## Migration Rules

| Rule | Detail |
|------|--------|
| `AUTO_RANDOM` for new PKs | Never use `AUTO_INCREMENT` in new tables |
| Monitor DDL jobs | Use `ADMIN SHOW DDL JOBS` to track progress |
| Batch large DML | Use `BATCH` syntax for updates/deletes > 10K rows |
| TiDB Lightning for imports | Use for initial loads > 1 GB |
| Test DDL duration | DDL on billion-row tables can take hours |
| Flyway baseline | Set baseline when migrating from MySQL |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| gh-ost / pt-osc | Unnecessary; TiDB DDL is online | Use native DDL directly |
| Single large DELETE | Transaction size limit (100 MB) | Use `BATCH ON id LIMIT 1000 DELETE` |
| DDL + DML in same migration | Blocks DDL queue | Separate DDL and DML scripts |
| Ignoring DDL job status | DDL may fail silently | Monitor with `ADMIN SHOW DDL JOBS` |
| Manual data import for large datasets | Slow, transaction limits | Use TiDB Lightning |
