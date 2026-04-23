---
name: database-patterns
description: "Database conventions for {{DB_TYPE}} + {{CACHE_TYPE}}: schema design, migrations, indexing, query optimization, caching patterns, and connection pool management."
---

# Knowledge Pack: Database Patterns

## Purpose

Provides database conventions for schema design, migration management, indexing strategies, query optimization, and caching patterns. All patterns are {{DB_TYPE}}-aware and use {{DB_MIGRATION}} for schema versioning. Cache patterns use {{CACHE_TYPE}}. Included when `database != "none"` or `cache != "none"`.

### Database References

**SQL databases (postgresql, oracle, mysql):**
- `references/sql-principles.md` — Universal SQL principles (DDL transactions, ACID, locking)
- `references/types-and-conventions.md` — Data types and naming for {{DB_TYPE}}
- `references/migration-patterns.md` — Migration patterns for {{DB_TYPE}} with {{DB_MIGRATION}}
- `references/query-optimization.md` — Query optimization for {{DB_TYPE}}

**NoSQL databases (mongodb, cassandra, eventstoredb):**
- `references/nosql-principles.md` — Universal NoSQL principles (CAP, query-driven modeling)
- `references/modeling-patterns.md` — Data modeling for {{DB_TYPE}}
- `references/migration-patterns.md` — Schema evolution for {{DB_TYPE}}
- `references/query-optimization.md` — Query optimization for {{DB_TYPE}}

**Graph databases (neo4j, neptune):**
- `references/graph-principles.md` — Property graph vs RDF, traversal paradigms, CAP positioning
- `references/modeling-patterns.md` — Node/edge design, constraint definitions for {{DB_TYPE}}
- `references/migration-patterns.md` — Schema evolution, constraint management for {{DB_TYPE}}
- `references/query-optimization.md` — Traversal optimization, memory tuning for {{DB_TYPE}}

**Columnar/OLAP databases (clickhouse, druid):**
- `references/columnar-principles.md` — Columnar storage, compression, MPP, vectorized execution
- `references/modeling-patterns.md` — Table engine selection, schema design for {{DB_TYPE}}
- `references/migration-patterns.md` — DDL operations, partition management for {{DB_TYPE}}
- `references/query-optimization.md` — Query execution, partition pruning for {{DB_TYPE}}

**NewSQL/Distributed databases (yugabytedb, cockroachdb, tidb):**
- `references/newsql-principles.md` — Raft consensus, distributed transactions, HLC clocks
- `references/types-and-conventions.md` — Data types, PK strategies for {{DB_TYPE}}
- `references/migration-patterns.md` — Online DDL, migration tooling for {{DB_TYPE}}
- `references/query-optimization.md` — Distributed query planning, follower reads for {{DB_TYPE}}

**Time-series databases (influxdb, timescaledb):**
- `references/timeseries-principles.md` — Time-series storage, retention, downsampling, continuous aggregates
- `references/modeling-patterns.md` — Schema design, measurement/hypertable design for {{DB_TYPE}}
- `references/migration-patterns.md` — Schema evolution, retention policies for {{DB_TYPE}}
- `references/query-optimization.md` — Time-range queries, aggregation optimization for {{DB_TYPE}}

**Search engines (elasticsearch, opensearch):**
- `references/search-principles.md` — Inverted index, BM25, analysis pipeline, mapping design
- `references/modeling-patterns.md` — Index design, mapping types, nested vs parent-child for {{DB_TYPE}}
- `references/migration-patterns.md` — Reindex API, alias strategy, lifecycle management for {{DB_TYPE}}
- `references/query-optimization.md` — Query vs filter context, aggregations, profiling for {{DB_TYPE}}

### Cache References

- `references/cache-principles.md` — Universal cache patterns (Cache-Aside, TTL, key naming)
- `references/{{CACHE_TYPE}}-patterns.md` — Specific patterns for {{CACHE_TYPE}}

### Cross-Reference

- `references/version-matrix.md` — Version compatibility table for all databases and caches

---

## Universal Rules (Always Apply)

### 1. Schema Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Schema/Database | lowercase, project name | `simulator`, `billing` |
| Table/Collection | snake_case, plural | `transactions`, `merchants` |
| Column/Field | snake_case | `response_code`, `created_at` |
| Index | `idx_{table}_{columns}` | `idx_transactions_stan_date` |
| Unique constraint | `uq_{table}_{column}` | `uq_merchants_mid` |
| Foreign key | `fk_{table}_{ref}` | `fk_terminals_merchant_id` |
| Check constraint | `ck_{table}_{rule}` | `ck_transactions_amount_positive` |

### 2. Mandatory Columns (SQL)

Every table MUST include:

```sql
id BIGSERIAL PRIMARY KEY,              -- or BIGINT AUTO_INCREMENT for MySQL
-- ... domain columns ...
created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
```

### 3. Data Security

| Data | Storage Rule |
|------|-------------|
| Passwords | Hashed (bcrypt/argon2), NEVER plaintext |
| PAN / Card numbers | Masked (first 6 + last 4) or tokenized |
| PIN blocks, CVV | NEVER persist |
| Documents (SSN, tax ID) | Encrypt at rest or mask |
| API keys | Hashed, NEVER plaintext |

### 4. Query Best Practices

- NEVER use `SELECT *` in production queries — list columns explicitly
- ALWAYS use parameterized queries (prevent SQL injection)
- ALWAYS paginate list queries (never return unbounded results)
- Prefer `EXISTS` over `COUNT` for existence checks

### 5. Connection Pool Sizing

| Environment | min-size | max-size | Notes |
|-------------|----------|----------|-------|
| Dev/Test | 2 | 5 | Minimal, fast startup |
| Staging | 5 | 15 | Mirrors production ratio |
| Production | 5 | 20 | Tuned to workload |

### 6. Cache Key Naming (when cache enabled)

```
{service}:{entity}:{id}
{service}:{entity}:{id}:{field}
{service}:{query}:{hash}
```

Always include TTL. Never cache sensitive data (PAN, PIN, credentials).

---

## Anti-Patterns

- `FLOAT` or `DECIMAL` for monetary values — use `BIGINT` (cents)
- `TEXT` without reason for bounded fields — use `VARCHAR(N)`
- Composite primary keys — use surrogate `BIGSERIAL` + UNIQUE constraint
- Cascading deletes in production — use soft delete (`status = 'DELETED'`)
- `SELECT *` in any production query
- Storing full sensitive data (PAN, PIN) unmasked
- Migrations that modify already-applied scripts
- Missing indexes on foreign key columns
- Cache without TTL — causes unbounded memory growth
- Storing sensitive data in cache

---

## Connection Pool Management

Proper connection pool configuration prevents resource exhaustion and improves throughput. Read `references/connection-pool-tuning.md` for detailed sizing guides per database and language.

### Sizing Formula

PostgreSQL recommended formula:

```
connections = ((core_count * 2) + effective_spindle_count)
```

For SSD-backed systems, use `connections = (core_count * 2) + 1` as a starting point.

### Timeout Configuration

| Parameter | Recommended Default | Description |
|-----------|-------------------|-------------|
| connectionTimeout | 30s | Maximum wait for connection from pool |
| idleTimeout | 10m | Maximum idle time before connection is retired |
| maxLifetime | 30m | Maximum total lifetime of a connection |
| validationTimeout | 5s | Timeout for connection validation query |
| leakDetectionThreshold | 60s | Log warning when connection held longer than threshold |

### Monitoring Metrics

| Metric | Alert Threshold | Action |
|--------|----------------|--------|
| Active connections | > 80% of max | Scale pool or optimize queries |
| Idle connections | > 50% of max | Reduce min-idle setting |
| Wait time (p99) | > 1s | Increase pool size or optimize slow queries |
| Timeout events | > 0 per minute | Investigate connection leaks or increase pool |
| Connection creation rate | Spikes | Check for connection leaks or pool misconfiguration |

### Pool Per Service vs Shared Pool

| Approach | Pros | Cons |
|----------|------|------|
| Pool per service | Isolation, independent scaling, fault containment | Higher total connection count |
| Shared pool (PgBouncer, ProxySQL) | Lower total connections, centralized management | Single point of failure, added latency |

**Recommendation:** Use pool-per-service for microservices; use a connection proxy (PgBouncer, ProxySQL) when total connections across all services exceed database limits.

### Framework-Specific Configuration

| Framework | Library | Key Configuration |
|-----------|---------|-------------------|
| Java (Spring/Quarkus) | HikariCP | `maximumPoolSize`, `minimumIdle`, `connectionTimeout` |
| Python (FastAPI/Django) | asyncpg pool / psycopg pool | `min_size`, `max_size`, `max_idle` |
| Go (Gin/Echo) | pgx pool / database/sql | `MaxOpenConns`, `MaxIdleConns`, `ConnMaxLifetime` |
| Rust (Axum/Actix) | sqlx pool / deadpool | `max_connections`, `min_connections`, `idle_timeout` |
| TypeScript (NestJS) | Prisma connection pool / pg pool | `connection_limit`, `pool_timeout` |

---

## Index Management

Indexes accelerate reads but slow writes and consume storage. Every index must be justified by a measured query pattern.

### Creation Strategy

1. **Measure first** — Run EXPLAIN ANALYZE on candidate queries before creating indexes
2. **Avoid index bloat** — Do not create indexes speculatively; unused indexes waste write I/O
3. **Prioritize** — Index columns in WHERE, JOIN, and ORDER BY clauses of frequent queries
4. **Composite order** — Place most selective column first in composite indexes

### Unused Index Detection

**PostgreSQL:**

```sql
SELECT schemaname, relname, indexrelname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
```

**MongoDB:**

```javascript
db.collection.aggregate([
  { $indexStats: {} },
  { $match: { "accesses.ops": 0 } }
])
```

Drop unused indexes after confirming with production query patterns over a sufficient observation window (minimum 30 days).

### Partial Indexes

Use partial indexes when queries frequently filter on a specific condition:

```sql
CREATE INDEX idx_orders_pending
  ON orders (created_at)
  WHERE status = 'PENDING';
```

Best for: sparse data distributions, frequent filtered queries, reducing index size.

### Covering Indexes

Include additional columns to satisfy queries entirely from the index:

**PostgreSQL 11+:**

```sql
CREATE INDEX idx_orders_customer_covering
  ON orders (customer_id)
  INCLUDE (total_amount, status);
```

**MongoDB (compound index as covering):**

```javascript
db.orders.createIndex(
  { customer_id: 1, total_amount: 1, status: 1 }
)
```

### Index Maintenance

| Operation | Database | When to Use |
|-----------|----------|-------------|
| REINDEX | PostgreSQL | Index bloat > 30%, after bulk operations |
| REINDEX CONCURRENTLY | PostgreSQL 12+ | Production reindex without blocking |
| CREATE INDEX CONCURRENTLY | PostgreSQL | Add index without locking writes |
| db.collection.reIndex() | MongoDB | After bulk deletes, index fragmentation |

---

## Maintenance Operations

Regular maintenance prevents performance degradation from bloat, fragmentation, and stale statistics.

### PostgreSQL Maintenance

| Operation | Purpose | Frequency | Impact |
|-----------|---------|-----------|--------|
| VACUUM | Reclaim dead tuple space | Autovacuum (continuous) | Minimal (does not block reads/writes) |
| VACUUM FULL | Rewrite table, reclaim all space | Quarterly or when bloat > 50% | **Blocks all access** — schedule in maintenance window |
| VACUUM FREEZE | Prevent transaction ID wraparound | When oldest xact age > 500M | Moderate (can be slow on large tables) |
| ANALYZE | Update query planner statistics | After bulk load/delete or autovacuum | Minimal |
| REINDEX | Rebuild indexes | When index bloat > 30% | Use CONCURRENTLY in production |
| pg_repack | Online table/index reorganization | When VACUUM FULL is needed without downtime | Requires extension, moderate I/O |

**Autovacuum Tuning:**

| Parameter | Default | High-Write Tables |
|-----------|---------|-------------------|
| autovacuum_vacuum_threshold | 50 | 50 |
| autovacuum_vacuum_scale_factor | 0.2 | 0.01 |
| autovacuum_analyze_threshold | 50 | 50 |
| autovacuum_analyze_scale_factor | 0.1 | 0.005 |

### MongoDB Maintenance

| Operation | Purpose | When to Use |
|-----------|---------|-------------|
| compact | Defragment collection and reclaim space | After bulk deletes |
| validate | Check collection data and index integrity | After unexpected shutdown |
| reIndex | Rebuild all indexes on a collection | When index fragmentation detected |
| repair | Repair database (standalone only) | Data corruption |

### Monitoring for Maintenance

| Metric | Tool | Alert Threshold |
|--------|------|-----------------|
| Table bloat ratio | pg_stat_user_tables | > 30% bloat |
| Dead tuple ratio | pg_stat_user_tables (n_dead_tup / n_live_tup) | > 10% |
| Index bloat | pgstattuple extension | > 30% bloat |
| Collection fragmentation | db.collection.stats() | storageSize >> dataSize |

---

## Data Governance Patterns

Data governance ensures data quality, compliance, and auditability across the data lifecycle.

### Classification Enforcement

| Classification | Access Policy | Storage Requirement |
|---------------|---------------|---------------------|
| PUBLIC | Unrestricted | Standard |
| INTERNAL | Role-based access | Standard |
| CONFIDENTIAL | Need-to-know, encrypted in transit | Encrypted at rest |
| RESTRICTED | Explicit approval, fully audited | Encrypted at rest + column-level encryption |

Apply column-level classification tags using comments or metadata tables:

```sql
COMMENT ON COLUMN users.email IS '@classification:CONFIDENTIAL';
COMMENT ON COLUMN users.ssn IS '@classification:RESTRICTED';
```

### Retention Automation

**Time-based partitioning + partition drop (PostgreSQL):**

```sql
CREATE TABLE events (
    id BIGSERIAL,
    event_time TIMESTAMPTZ NOT NULL,
    payload JSONB
) PARTITION BY RANGE (event_time);

-- Monthly partitions, drop partitions older than retention period
DROP TABLE IF EXISTS events_2024_01;
```

**TTL indexes (MongoDB):**

```javascript
db.sessions.createIndex(
  { "createdAt": 1 },
  { expireAfterSeconds: 86400 }  // 24 hours
)
```

### Audit Trail for Data Changes

**Trigger-based audit tables (PostgreSQL):**

```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    operation TEXT NOT NULL,  -- INSERT, UPDATE, DELETE
    old_data JSONB,
    new_data JSONB,
    changed_by TEXT NOT NULL,
    changed_at TIMESTAMPTZ DEFAULT NOW()
);
```

**Temporal tables (SQL:2011):**

Use system-versioned temporal tables for automatic history tracking where supported (PostgreSQL with temporal_tables extension, SQL Server, MariaDB).

### Data Masking

| Technique | Use Case | Implementation |
|-----------|----------|----------------|
| Static masking | Non-production environments | ETL pipeline transforms during data copy |
| Dynamic masking | Production queries by restricted roles | Views with masking functions |
| Tokenization | Replace sensitive values with tokens | Token vault service |

```sql
CREATE VIEW users_masked AS
SELECT id,
       regexp_replace(email, '(.).*@', '\1***@') AS email,
       'XXX-XX-' || right(ssn, 4) AS ssn
FROM users;
```

---

## Backup Strategy Patterns

Backup strategy must match Recovery Time Objective (RTO) and Recovery Point Objective (RPO) requirements.

### PostgreSQL Backup

| Method | Type | RPO | Use Case |
|--------|------|-----|----------|
| pg_dump | Logical | Point-in-time of dump | Small databases, schema-only backups, cross-version migration |
| pg_basebackup | Physical | Point-in-time of backup start | Full cluster backup, basis for PITR |
| WAL archiving | Continuous | Near-zero (last archived WAL) | Point-in-time recovery, streaming replication |
| pgBackRest | Full + incremental + differential | Near-zero with WAL | Production-grade backup with parallelism and encryption |

### MongoDB Backup

| Method | Type | RPO | Use Case |
|--------|------|-----|----------|
| mongodump | Logical | Point-in-time of dump | Small datasets, cross-version migration |
| Filesystem snapshots | Physical | Point-in-time of snapshot | Large datasets, consistent with journaling |
| Oplog-based PITR | Continuous | Near-zero (last oplog entry) | Point-in-time recovery for replica sets |

### Point-in-Time Recovery

**PostgreSQL (WAL replay):**

1. Restore base backup with `pg_basebackup`
2. Configure `recovery_target_time` in `postgresql.conf`
3. Start PostgreSQL — it replays WAL segments up to target time
4. Verify data integrity after recovery

**MongoDB (oplog replay):**

1. Restore from `mongodump` or filesystem snapshot
2. Replay oplog entries up to target timestamp using `mongorestore --oplogReplay`
3. Verify collection integrity

### Backup Verification

| Check | Frequency | Method |
|-------|-----------|--------|
| Backup completion | Every backup | Automated monitoring and alerting |
| Backup integrity | Daily | Checksum validation (pgBackRest verify, md5sum) |
| Restore test | Weekly (production) | Full restore to isolated test environment |
| PITR test | Monthly | Restore to specific timestamp and verify data |
| Cross-region copy | Daily | Verify offsite backup exists and is readable |

## Related Knowledge Packs

| Pack | Relationship |
|------|-------------|
| `data-modeling` | Cross-cutting data modeling patterns (schema design, concurrency, test data) |
| `data-management` | Data management lifecycle patterns |
| `architecture` | Core architecture principles |
