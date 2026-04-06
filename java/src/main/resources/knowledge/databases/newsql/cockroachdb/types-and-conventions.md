# CockroachDB — Types and Conventions

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **23.1** | User-defined functions, trigram indexes | LTS |
| **23.2** | Improved changefeed, physical cluster replication | Current stable |
| **24.1+** | Enhanced multi-region, improved cost-based optimizer | Latest |

## Wire Compatibility

CockroachDB is PostgreSQL wire-compatible:

| Aspect | Detail |
|--------|--------|
| Protocol | PostgreSQL wire protocol (v3) |
| Port | 26257 (default) |
| Driver | `org.postgresql:postgresql` |
| SQL dialect | CockroachDB SQL (PostgreSQL subset + extensions) |
| ORM support | Hibernate, JOOQ, Spring Data JPA (PostgreSQL dialect) |

## Data Types

| Data | Recommended Type | Notes |
|------|-----------------|-------|
| Primary key | `UUID DEFAULT gen_random_uuid()` | Distributed-friendly |
| Auto-increment | `unique_rowid()` | Combines timestamp + node ID; no hot spots |
| Timestamps | `TIMESTAMPTZ` | Always timezone-aware |
| Money | `INT8` (cents) | Avoid `DECIMAL` for high-throughput |
| Status / Enum | `STRING` with CHECK | Native ENUM also supported |
| JSON | `JSONB` | Indexed with inverted indexes |
| Boolean | `BOOL` | Native support |
| Arrays | `ARRAY` | Supported |
| Spatial | `GEOMETRY` / `GEOGRAPHY` | PostGIS-compatible |

### Primary Key Strategies

```sql
-- UUID (recommended for most cases)
CREATE TABLE orders (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id UUID NOT NULL,
    total_amount INT8 NOT NULL,
    status STRING NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- unique_rowid (when UUID overhead is unacceptable)
CREATE TABLE events (
    id INT8 DEFAULT unique_rowid() PRIMARY KEY,
    event_time TIMESTAMPTZ NOT NULL,
    data JSONB
);

-- Hash-sharded index (for sequential workloads)
CREATE TABLE logs (
    id UUID DEFAULT gen_random_uuid(),
    log_time TIMESTAMPTZ NOT NULL,
    message STRING,
    PRIMARY KEY (id) USING HASH
);
```

**Rule:** NEVER use `SERIAL` — it creates sequential values causing range hot spots. Use `UUID` or `unique_rowid()`.

## Multi-Region Configuration

### LOCALITY Annotations

```sql
-- Regional by row (data pinned to region)
CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid(),
    region crdb_internal_region NOT NULL,
    email STRING NOT NULL,
    PRIMARY KEY (region, id)
) LOCALITY REGIONAL BY ROW;

-- Regional table (entire table in one region)
CREATE TABLE audit_log (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    entry JSONB NOT NULL
) LOCALITY REGIONAL BY TABLE IN "us-east1";

-- Global table (read-optimized, replicated everywhere)
CREATE TABLE countries (
    code STRING PRIMARY KEY,
    name STRING NOT NULL
) LOCALITY GLOBAL;
```

| LOCALITY | Reads | Writes | Use Case |
|----------|-------|--------|----------|
| REGIONAL BY ROW | Local | Local (to row's region) | User data, per-region |
| REGIONAL BY TABLE | Local (in region) | Local (in region) | Region-specific data |
| GLOBAL | Fast everywhere | Slow (all regions) | Reference tables |

## Index Conventions

```sql
-- Standard index
CREATE INDEX idx_orders_customer
    ON orders (customer_id);

-- STORING index (covering, avoids table lookup)
CREATE INDEX idx_orders_customer_storing
    ON orders (customer_id)
    STORING (total_amount, status);

-- Inverted index for JSONB
CREATE INVERTED INDEX idx_events_data
    ON events (data);

-- Hash-sharded index (prevent hot spots on sequential keys)
CREATE INDEX idx_logs_time
    ON logs (log_time)
    USING HASH WITH (bucket_count = 8);

-- Partial index
CREATE INDEX idx_orders_pending
    ON orders (created_at)
    WHERE status = 'PENDING';
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Database | snake_case | `order_service` |
| Table | snake_case, plural | `orders`, `order_items` |
| Column | snake_case | `customer_id`, `created_at` |
| Index | `idx_{table}_{columns}` | `idx_orders_customer` |
| Constraint | `ck_{table}_{rule}` | `ck_orders_amount_positive` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| `SERIAL` primary keys | Hot spot on single range | Use `UUID` or `unique_rowid()` |
| Missing STORING clause | Table lookup for every index hit | Add STORING for frequently accessed columns |
| GLOBAL for write-heavy tables | Cross-region consensus on every write | Use REGIONAL BY ROW |
| Large transactions (> 64 MB) | Transaction exceeds range size | Break into smaller batches |
| No region-aware queries | Cross-region reads for local data | Use REGIONAL BY ROW with region filter |
