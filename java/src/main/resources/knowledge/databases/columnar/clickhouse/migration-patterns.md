# ClickHouse — Migration Patterns

## Overview

ClickHouse has **no built-in migration framework**. Schema evolution uses versioned SQL scripts managed by application-level tools.

| Approach | Best For | Tool |
|----------|----------|------|
| **Goose / golang-migrate** | Go projects, ops-driven | CLI + SQL files |
| **Flyway** | Java/JVM projects | Flyway with ClickHouse dialect |
| **Custom SQL scripts** | Any project, CI/CD pipelines | `clickhouse-client` |
| **Atlas** | Declarative schema management | HCL or SQL schema files |

## DDL Operations

### Safe DDL (Non-Blocking)

| Operation | Blocking? | Notes |
|-----------|-----------|-------|
| `CREATE TABLE` | No | Instant |
| `DROP TABLE` | No | Instant (data deleted async) |
| `ALTER TABLE ADD COLUMN` | No | Metadata-only change |
| `ALTER TABLE DROP COLUMN` | No | Metadata-only; data removed on merge |
| `ALTER TABLE RENAME COLUMN` | No | Metadata-only (23.4+) |
| `ALTER TABLE MODIFY COLUMN` | **Depends** | Type widening is safe; narrowing rewrites |
| `CREATE INDEX` (skipping) | No | Applied to new parts only |

### Column Operations

```sql
-- Add column (instant, metadata-only)
ALTER TABLE events ADD COLUMN region LowCardinality(String)
    DEFAULT 'unknown' AFTER user_id;

-- Drop column (metadata; data cleaned on merge)
ALTER TABLE events DROP COLUMN old_field;

-- Rename column (23.4+)
ALTER TABLE events RENAME COLUMN old_name TO new_name;

-- Modify type (safe widening)
ALTER TABLE events MODIFY COLUMN amount Decimal128(4);

-- Add column with codec
ALTER TABLE events ADD COLUMN tags Array(String)
    CODEC(ZSTD(3));
```

## Mutations

Mutations are heavy operations that rewrite parts. Use sparingly.

```sql
-- Update existing data (creates a mutation)
ALTER TABLE events UPDATE region = 'US'
    WHERE country_code = 'US';

-- Delete rows (creates a mutation)
ALTER TABLE events DELETE
    WHERE event_date < '2023-01-01';

-- Lightweight delete (23.x+, marks rows, no rewrite)
DELETE FROM events WHERE event_date < '2023-01-01';
```

### Mutation Monitoring

```sql
-- Check mutation progress
SELECT database, table, mutation_id, command, is_done,
       parts_to_do, latest_fail_reason
FROM system.mutations
WHERE is_done = 0;
```

| Rule | Detail |
|------|--------|
| Avoid frequent mutations | Each mutation rewrites affected parts |
| Prefer TTL over DELETE | Let TTL drop old partitions automatically |
| Monitor mutation queue | Long queues indicate design problems |

## Partition Operations

```sql
-- Drop old partition (instant, no mutation)
ALTER TABLE events DROP PARTITION '202301';

-- Detach partition (move to detached/ directory)
ALTER TABLE events DETACH PARTITION '202301';

-- Attach partition from another table
ALTER TABLE events_archive ATTACH PARTITION '202301'
    FROM events;

-- Move partition between tables
ALTER TABLE events MOVE PARTITION '202301'
    TO TABLE events_archive;
```

**Rule:** Partition dropping is the preferred method for data retention. Design partition keys to align with retention periods.

## Schema Versioning Strategy

### File Structure

```
db/clickhouse/migrations/
|-- V001__create_raw_events.sql
|-- V002__create_hourly_stats.sql
|-- V003__add_region_column.sql
|-- V004__create_materialized_view.sql
+-- V005__add_ttl_policy.sql
```

### Migration Script Rules

| Rule | Detail |
|------|--------|
| Idempotent | Use `IF NOT EXISTS` / `IF EXISTS` on all DDL |
| One change per file | Single logical change per migration |
| Versioned in Git | Migration files alongside application code |
| Tested | Run against test ClickHouse before production |
| No data mutations in DDL | Separate DDL migrations from data backfills |

### Example Migration

```sql
-- V003__add_region_column.sql
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS region LowCardinality(String)
    DEFAULT 'unknown';

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS processed_at DateTime64(3)
    DEFAULT now64(3);
```

## TTL Management

```sql
-- Table-level TTL (drop rows)
ALTER TABLE events MODIFY TTL event_date + INTERVAL 90 DAY;

-- Column-level TTL (null out column after retention)
ALTER TABLE events MODIFY COLUMN payload String
    TTL event_date + INTERVAL 30 DAY;

-- Move to cold storage
ALTER TABLE events MODIFY TTL
    event_date + INTERVAL 7 DAY TO VOLUME 'hot',
    event_date + INTERVAL 30 DAY TO VOLUME 'warm',
    event_date + INTERVAL 90 DAY TO VOLUME 'cold';
```

## Replication and Cluster Migrations

### ReplicatedMergeTree DDL

```sql
-- DDL on cluster applies to all replicas
ALTER TABLE events ON CLUSTER '{cluster}'
    ADD COLUMN region LowCardinality(String) DEFAULT 'unknown';
```

| Rule | Detail |
|------|--------|
| Use `ON CLUSTER` | Ensures DDL applies to all replicas |
| Verify replication lag | Check `system.replication_queue` before next migration |
| Test on single replica first | For risky changes, detach replica, test, re-attach |

## Codec Migration

```sql
-- Change compression codec
ALTER TABLE events MODIFY COLUMN payload
    CODEC(ZSTD(3));

-- Force recompression of existing parts
OPTIMIZE TABLE events FINAL;
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Frequent mutations | Rewrites parts, blocks merges | Design for append-only |
| DELETE instead of DROP PARTITION | Expensive mutation | Use partition-aligned retention |
| Type narrowing without check | Data loss on incompatible values | Validate data before MODIFY |
| DDL without `ON CLUSTER` | Schema drift across replicas | Always use ON CLUSTER |
| Missing `IF NOT EXISTS` | Migration fails on re-run | All DDL must be idempotent |
