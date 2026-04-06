# YugaByteDB — Migration Patterns

## Overview

YugaByteDB YSQL is PostgreSQL wire-compatible, so standard PostgreSQL migration tools work:

| Tool | Support | Notes |
|------|---------|-------|
| **Flyway** | Full | Use PostgreSQL JDBC driver |
| **Liquibase** | Full | Use PostgreSQL JDBC driver |
| **golang-migrate** | Full | PostgreSQL driver |
| **Atlas** | Full | PostgreSQL dialect |

## Flyway Configuration

```yaml
# application.yml (Spring Boot)
spring:
  flyway:
    url: jdbc:postgresql://yb-tserver:5433/myapp
    user: ${DB_USER}
    password: ${DB_PASSWORD}
    schemas: public
    locations: classpath:db/migration
    baseline-on-migrate: true
```

### Migration File Structure

```
db/migration/
|-- V001__create_orders_table.sql
|-- V002__create_order_items_table.sql
|-- V003__add_customer_index.sql
|-- V004__partition_events_table.sql
+-- V005__colocate_reference_tables.sql
```

## DDL Operations

### Safe DDL

| Operation | Online? | Notes |
|-----------|---------|-------|
| `CREATE TABLE` | Yes | Instant |
| `DROP TABLE` | Yes | Instant |
| `ADD COLUMN` | Yes | Metadata-only (no default) |
| `ADD COLUMN ... DEFAULT` | Yes | Backfill required for existing rows |
| `DROP COLUMN` | Yes | Metadata-only; data cleaned on compaction |
| `CREATE INDEX` | Partially | Backfill runs in background |
| `DROP INDEX` | Yes | Instant |
| `RENAME TABLE` | Yes | Metadata-only |

### Online DDL Considerations

```sql
-- Add column (safe, no default)
ALTER TABLE orders ADD COLUMN notes TEXT;

-- Add column with default (backfills existing rows)
ALTER TABLE orders ADD COLUMN priority INT DEFAULT 0;

-- Create index (runs backfill in background)
CREATE INDEX CONCURRENTLY idx_orders_status
    ON orders (status);
```

**Rule:** Always use `CREATE INDEX CONCURRENTLY` in production to avoid blocking writes.

## Tablet Splitting

YugaByteDB automatically splits tablets when they exceed the configured threshold:

| Parameter | Default | Recommendation |
|-----------|---------|---------------|
| `tablet_split_size` | 64 GB | Reduce to 10 GB for faster rebalancing |
| `tablet_split_low_watermark` | N/A | Auto-managed |
| `enable_automatic_tablet_splitting` | true | Keep enabled |

### Manual Tablet Management

```bash
# Check tablet distribution
yb-admin -master_addresses host:7100 \
    list_tablets ysql.myapp orders

# Manual split (rarely needed)
yb-admin -master_addresses host:7100 \
    split_tablet <tablet_id>
```

## Colocated Database Migration

```sql
-- Create colocated database
CREATE DATABASE myapp WITH COLOCATION = true;

-- Migrate reference tables (stay colocated)
CREATE TABLE countries (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);  -- Automatically colocated

-- Migrate large tables (opt out)
CREATE TABLE events (
    id UUID DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL,
    data JSONB
) WITH (COLOCATION = false);
```

## Partitioning Migration

```sql
-- Step 1: Create partitioned table
CREATE TABLE events (
    id UUID DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL,
    data JSONB
) PARTITION BY RANGE (event_time);

-- Step 2: Create partitions
CREATE TABLE events_2024_q1 PARTITION OF events
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE events_2024_q2 PARTITION OF events
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

-- Step 3: Attach existing table as partition
ALTER TABLE events ATTACH PARTITION events_legacy
    FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
```

## xCluster Replication Migration

For migrating to multi-region:

```bash
# Set up xCluster replication
yb-admin -master_addresses primary:7100 \
    setup_universe_replication \
    replication_id \
    standby_master:7100 \
    table_id_1,table_id_2

# Check replication status
yb-admin -master_addresses primary:7100 \
    get_universe_replication replication_id
```

## Geo-Partitioning Migration

```sql
-- Create tablespace per region
CREATE TABLESPACE us_east WITH (
    replica_placement = '{"num_replicas": 3,
    "placement_blocks": [{"cloud": "aws",
    "region": "us-east-1", "zone": "us-east-1a",
    "min_num_replicas": 1}]}'
);

-- Partition by region
CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid(),
    region TEXT NOT NULL,
    email TEXT NOT NULL
) PARTITION BY LIST (region);

CREATE TABLE users_us PARTITION OF users
    FOR VALUES IN ('US') TABLESPACE us_east;
```

## Migration Rules

| Rule | Detail |
|------|--------|
| UUID for new PKs | Never use SERIAL in new tables |
| Colocate small tables | Reference tables < 100 MB should be colocated |
| Test tablet splits | Run migrations in staging with production-like data volume |
| Index concurrently | Always use CONCURRENTLY in production |
| Monitor replication | Verify xCluster lag before cutting over |
| Flyway baseline | Set baseline when migrating from PostgreSQL |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Large transactions in migration | Distributed transaction timeout | Break into smaller batches |
| DDL + DML in same migration | Locks held across phases | Separate DDL and DML migrations |
| Missing CONCURRENTLY | Index creation blocks writes | Always use CONCURRENTLY |
| Ignoring tablet count | Too many tablets per node | Monitor with `yb-admin` |
| No colocation strategy | Excessive tablet overhead for small tables | Plan colocation at design time |
