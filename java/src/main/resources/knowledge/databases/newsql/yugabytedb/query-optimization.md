# YugaByteDB — Query Optimization

## EXPLAIN ANALYZE with Distribution Info

```sql
EXPLAIN (ANALYZE, DIST, COSTS)
SELECT o.id, o.total_amount, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING'
  AND o.created_at > now() - interval '7 days';
```

### Key EXPLAIN Output Fields

| Field | Good | Bad |
|-------|------|-----|
| `Storage Table Read Requests` | Low, proportional to result | Very high (full scan) |
| `Storage Table Rows Scanned` | Close to rows returned | Orders of magnitude higher |
| `Storage Index Read Requests` | Present (index used) | Absent (seq scan) |
| `Actual Rows` vs `Plan Rows` | Close match | Wildly different |
| `DocDB Scanned Rows` | Low | High (need better index) |

**Rule:** Every production query MUST show `Index Scan` or `Index Only Scan` in EXPLAIN output. `Seq Scan` on tables with more than 10,000 rows is PROHIBITED.

## Follower Reads

Read from the closest replica instead of the leader for lower latency:

```sql
-- Session-level follower reads
SET yb_read_from_followers = true;
SET yb_follower_read_staleness_ms = 2000;

-- Per-query (application code)
SELECT * FROM orders
WHERE customer_id = $1;
```

| Setting | Default | Purpose |
|---------|---------|---------|
| `yb_read_from_followers` | false | Enable follower reads |
| `yb_follower_read_staleness_ms` | 30000 | Max staleness tolerance |

### When to Use Follower Reads

| Scenario | Follower Reads? |
|----------|----------------|
| Dashboard / reporting queries | Yes (tolerate 2-5s staleness) |
| Read-after-write consistency needed | No (use leader reads) |
| Geo-distributed reads | Yes (local replica) |
| Transaction isolation required | No (use default leader reads) |

## Tablet-Aware Query Design

### Hash-Distributed Tables

```sql
-- Query touches single tablet (good)
SELECT * FROM orders WHERE id = 'uuid-value';

-- Query touches all tablets (scatter-gather)
SELECT * FROM orders WHERE status = 'PENDING';
```

### Range-Distributed Tables

```sql
-- Create table with range sharding
CREATE TABLE events (
    event_time TIMESTAMPTZ,
    user_id UUID,
    data JSONB,
    PRIMARY KEY (event_time ASC, user_id)
);

-- Query benefits from range: single tablet range
SELECT * FROM events
WHERE event_time >= '2024-01-01' AND event_time < '2024-01-02';
```

## Index Optimization

### Covering Index to Avoid Table Lookup

```sql
-- Without covering: index scan + table lookup
CREATE INDEX idx_orders_status ON orders (status);

-- With covering: index-only scan
CREATE INDEX idx_orders_status_covering
    ON orders (status)
    INCLUDE (total_amount, customer_id, created_at);
```

### Index for Sort Avoidance

```sql
-- Index matches ORDER BY, avoids sort
CREATE INDEX idx_orders_created_desc
    ON orders (created_at DESC);

SELECT id, total_amount FROM orders
ORDER BY created_at DESC
LIMIT 20;
```

## Batch Operations

### Bulk Insert

```sql
-- Use multi-row INSERT for efficiency
INSERT INTO events (id, event_time, data) VALUES
    (gen_random_uuid(), now(), '{"type":"A"}'),
    (gen_random_uuid(), now(), '{"type":"B"}'),
    (gen_random_uuid(), now(), '{"type":"C"}');
```

### Bulk Update with CTE

```sql
WITH batch AS (
    SELECT id FROM orders
    WHERE status = 'PENDING'
      AND created_at < now() - interval '30 days'
    LIMIT 1000
)
UPDATE orders SET status = 'EXPIRED'
WHERE id IN (SELECT id FROM batch);
```

**Rule:** Limit batch operations to 1000 rows per transaction to avoid distributed transaction timeout.

## Connection Management

| Parameter | Recommended | Notes |
|-----------|-------------|-------|
| `max_connections` | 300 per tserver | Higher than PostgreSQL due to distributed nature |
| Pool size (HikariCP) | 5-20 per service | Standard pool sizing applies |
| `ysql_conn_mgr_enabled` | true (2.21+) | Built-in connection pooling |

## Query Tuning Parameters

| Parameter | Default | Tuning |
|-----------|---------|--------|
| `yb_enable_optimizer_statistics` | false | Enable for complex queries |
| `yb_bnl_batch_size` | 1024 | Batch nested loop size |
| `yb_parallel_range_size` | auto | Parallel scan range |
| `statement_timeout` | 0 (none) | Set per-environment |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Seq scan on large table | Scans all tablets | Create appropriate indexes |
| Cross-shard ORDER BY without LIMIT | Full sort across nodes | Always pair ORDER BY with LIMIT |
| Large transactions | Distributed lock timeout | Keep transactions short, batch updates |
| Not using follower reads for reports | Unnecessary leader load | Enable follower reads for stale-tolerant queries |
| Missing INCLUDE on index | Table lookup for each row | Use covering indexes |
| Sequential ID patterns | Tablet hot spots | Use UUID-based primary keys |
