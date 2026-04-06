# TiDB — Types and Conventions

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **7.1** | Resource control, flashback cluster, TTL GA | LTS |
| **7.5** | Improved TiFlash, global memory control | Current LTS |
| **8.0+** | Enhanced cost model, improved DDL | Latest |

## Wire Compatibility

TiDB is MySQL wire-compatible:

| Aspect | Detail |
|--------|--------|
| Protocol | MySQL wire protocol |
| Port | 4000 (default) |
| Driver | `com.mysql:mysql-connector-j` |
| SQL dialect | MySQL-compatible with TiDB extensions |
| ORM support | Hibernate (MySQL dialect), MyBatis, Spring Data JPA |

## Data Types

| Data | Recommended Type | Notes |
|------|-----------------|-------|
| Primary key | `BIGINT` with `AUTO_RANDOM` | Distributed-friendly |
| Auto-increment | `AUTO_RANDOM(5)` | Avoids hot spot; shard bits in high bits |
| Timestamps | `TIMESTAMP` or `DATETIME` | TIMESTAMP auto-converts to UTC |
| Money | `BIGINT` (cents) | Avoid `DECIMAL` for high-throughput |
| Status / Enum | `ENUM('A','B','C')` or `VARCHAR` with CHECK | MySQL ENUM supported |
| JSON | `JSON` | Native JSON type, indexed with generated columns |
| Boolean | `BOOLEAN` (alias for `TINYINT(1)`) | MySQL convention |
| Text | `VARCHAR(N)` | Prefer bounded over `TEXT` |
| Binary | `VARBINARY(N)` or `BLOB` | For binary data |

### AUTO_RANDOM Primary Keys

```sql
CREATE TABLE orders (
    id BIGINT AUTO_RANDOM(5) PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);
```

| Parameter | Detail |
|-----------|--------|
| `AUTO_RANDOM(5)` | 5 shard bits = 32 shards for distribution |
| Shard bits range | 1-15 (default 5) |
| Remaining bits | Auto-increment within each shard |
| Uniqueness | Globally unique within the table |

**Rule:** Use `AUTO_RANDOM` for all primary keys in TiDB. `AUTO_INCREMENT` creates hot spots on a single TiKV region.

## TiFlash (HTAP)

TiFlash provides real-time analytics on transactional data:

```sql
-- Add TiFlash replica for a table
ALTER TABLE orders SET TIFLASH REPLICA 1;

-- Query automatically uses TiFlash for analytical queries
SELECT status, COUNT(*), SUM(total_amount)
FROM orders
WHERE created_at > '2024-01-01'
GROUP BY status;

-- Force TiFlash execution
SET tidb_isolation_read_engines = 'tiflash';
SELECT status, COUNT(*) FROM orders GROUP BY status;

-- Force TiKV execution (transactional)
SET tidb_isolation_read_engines = 'tikv';
SELECT * FROM orders WHERE id = 12345;
```

| Engine | Best For | Execution |
|--------|----------|-----------|
| TiKV | Point lookups, OLTP | Row-based |
| TiFlash | Aggregations, OLAP | Columnar, vectorized |
| MPP mode | Large analytical queries | Distributed TiFlash nodes |

## Index Conventions

```sql
-- Standard index
CREATE INDEX idx_orders_customer ON orders (customer_id);

-- Composite index
CREATE INDEX idx_orders_status_date
    ON orders (status, created_at);

-- Expression index (for JSON queries)
CREATE INDEX idx_events_type
    ON events ((CAST(data->>'$.type' AS CHAR(50))));

-- Invisible index (test before dropping)
ALTER TABLE orders ALTER INDEX idx_old_index INVISIBLE;

-- Clustered index (stores data in PK order)
CREATE TABLE sessions (
    id BIGINT AUTO_RANDOM PRIMARY KEY CLUSTERED,
    user_id BIGINT NOT NULL,
    data JSON
);
```

### Clustered vs Non-Clustered

| Type | Storage | Point Lookup | Range Scan |
|------|---------|-------------|-----------|
| Clustered (default for INT PK) | Data stored in PK order | Fast (no double read) | Fast on PK range |
| Non-clustered | Data stored separately | Extra lookup via _tidb_rowid | Fast on any index |

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
| `AUTO_INCREMENT` PKs | Hot region on single TiKV node | Use `AUTO_RANDOM(5)` |
| No TiFlash for analytics | OLAP queries compete with OLTP | Add TiFlash replicas for analytical tables |
| Large transactions (> 100 MB) | Transaction size limit exceeded | Break into smaller batches |
| Missing index on JOIN columns | Full table scan on large tables | Create indexes for all JOIN predicates |
| `SELECT *` on TiFlash tables | Reads all columns from columnar store | Select only needed columns |
| Non-clustered PK without reason | Extra lookup overhead | Use clustered PK by default |
