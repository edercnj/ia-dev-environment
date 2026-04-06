# YugaByteDB — Types and Conventions

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **2.18** | Colocated tables GA, read replicas | LTS |
| **2.20** | Improved YSQL performance, xCluster | Current stable |
| **2.21+** | Connection Manager, enhanced CDC | Latest |

## Wire Compatibility

YugaByteDB exposes two APIs:

| API | Wire Protocol | Use Case | Port |
|-----|-------------|----------|------|
| **YSQL** | PostgreSQL | Relational workloads, SQL features | 5433 |
| **YCQL** | Cassandra | Key-value, wide-column workloads | 9042 |

**Decision shortcut:** Use YSQL unless you have an existing Cassandra workload or need Cassandra-specific features (wide columns, TTL per cell).

## Data Types (YSQL)

| Data | Recommended Type | Notes |
|------|-----------------|-------|
| Primary key | `UUID` | Use `gen_random_uuid()` for distributed-friendly IDs |
| Auto-increment | Avoid `SERIAL` | Sequential IDs cause hot spots; use UUID or hash |
| Timestamps | `TIMESTAMPTZ` | Always timezone-aware |
| Money | `BIGINT` (cents) | Avoid DECIMAL for performance |
| Status / Enum | `TEXT` with CHECK | Enums require DDL to modify |
| JSON | `JSONB` | Supported, same as PostgreSQL |
| Boolean | `BOOLEAN` | Native support |
| Arrays | `ARRAY` | Supported, same as PostgreSQL |

### UUID Primary Keys

```sql
CREATE TABLE orders (
    id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    total_amount BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id)
);
```

**Rule:** NEVER use `SERIAL` or `BIGSERIAL` for primary keys in YugaByteDB. Sequential IDs create tablet hot spots. Use `UUID` with `gen_random_uuid()`.

## Colocated Tables

Small tables colocated in a single tablet to reduce overhead:

```sql
CREATE DATABASE myapp WITH COLOCATION = true;

-- All tables in this DB share a tablet by default
CREATE TABLE countries (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

-- Opt out of colocation for large tables
CREATE TABLE events (
    id UUID DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL,
    payload JSONB
) WITH (COLOCATION = false)
PARTITION BY RANGE (event_time);
```

| Guideline | Rule |
|-----------|------|
| Table size < 100 MB | Colocate (reduces tablet count) |
| Table size > 100 MB | Do NOT colocate (needs own tablets) |
| Reference/lookup tables | Always colocate |
| High-throughput tables | Never colocate |

## Table Partitioning

```sql
-- Range partitioning (time-based)
CREATE TABLE events (
    id UUID DEFAULT gen_random_uuid(),
    event_time TIMESTAMPTZ NOT NULL,
    data JSONB
) PARTITION BY RANGE (event_time);

CREATE TABLE events_2024_q1 PARTITION OF events
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

-- Hash partitioning for even distribution
CREATE TABLE sessions (
    id UUID DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    data JSONB
) PARTITION BY HASH (user_id);

CREATE TABLE sessions_p0 PARTITION OF sessions
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);
```

## Index Conventions

```sql
-- Standard index (LSM-tree based)
CREATE INDEX idx_orders_customer
    ON orders (customer_id);

-- Covering index (avoid table lookup)
CREATE INDEX idx_orders_customer_covering
    ON orders (customer_id)
    INCLUDE (total_amount, status);

-- Partial index
CREATE INDEX idx_orders_pending
    ON orders (created_at)
    WHERE status = 'PENDING';

-- GIN index for JSONB
CREATE INDEX idx_events_payload
    ON events USING GIN (payload);
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Database | snake_case | `order_service`, `user_mgmt` |
| Table | snake_case, plural | `orders`, `order_items` |
| Column | snake_case | `customer_id`, `created_at` |
| Index | `idx_{table}_{columns}` | `idx_orders_customer` |
| Constraint | `ck_{table}_{rule}` | `ck_orders_amount_positive` |
| Partition | `{table}_{qualifier}` | `events_2024_q1` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| `SERIAL` / `BIGSERIAL` PKs | Tablet hot spots, range split bottleneck | Use `UUID` with `gen_random_uuid()` |
| Large colocated tables | Single tablet becomes bottleneck | Set `COLOCATION = false` for tables > 100 MB |
| Missing tablet split config | Unbalanced data distribution | Monitor tablet sizes via yb-admin |
| Cross-region transactions | High latency per commit | Use geo-partitioning or follower reads |
| Sequential scan on large table | Full tablet scan | Ensure index exists for all filter columns |
