# CockroachDB — Query Optimization

## EXPLAIN ANALYZE

```sql
EXPLAIN ANALYZE
SELECT o.id, o.total_amount, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING'
  AND o.created_at > now() - interval '7 days';
```

### Key Metrics

| Metric | Good | Bad |
|--------|------|-----|
| Scan type | `index scan` or `index join` | `full scan` |
| Rows read vs rows returned | Close match | Orders of magnitude apart |
| KV rows read | Proportional to result | Very high |
| Contention time | 0 ms | > 10 ms |
| Network latency | < 5 ms (same region) | > 50 ms (cross-region) |

**Rule:** Every production query MUST use index scans. Full scans on tables with > 10,000 rows are PROHIBITED.

## AS OF SYSTEM TIME (Historical Reads)

Read data at a past timestamp, avoiding contention with current writes:

```sql
-- Read data from 5 seconds ago (no contention)
SELECT * FROM orders
AS OF SYSTEM TIME '-5s'
WHERE customer_id = $1;

-- Read at specific timestamp
SELECT * FROM orders
AS OF SYSTEM TIME '2024-01-15 10:30:00+00'
WHERE status = 'COMPLETED';

-- Follower reads (read from nearest replica)
SELECT * FROM orders
AS OF SYSTEM TIME follower_read_timestamp()
WHERE customer_id = $1;
```

| Use Case | Staleness | Syntax |
|----------|-----------|--------|
| Reporting | 5-30 seconds | `AS OF SYSTEM TIME '-30s'` |
| Analytics | Minutes | `AS OF SYSTEM TIME '-5m'` |
| Follower reads | Auto-calculated | `AS OF SYSTEM TIME follower_read_timestamp()` |
| Backup reads | Point-in-time | `AS OF SYSTEM TIME 'timestamp'` |

**Rule:** Use `AS OF SYSTEM TIME` for all read-only queries that tolerate staleness. Reduces contention and enables follower reads.

## Serialization Retry Handling

CockroachDB uses serializable isolation by default. Transactions may receive retry errors:

```java
// CRDB serialization retry pattern
public <T> T executeWithRetry(
        Function<Connection, T> operation) {
    int maxRetries = 5;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            T result = operation.apply(conn);
            conn.commit();
            return result;
        } catch (SQLException e) {
            if ("40001".equals(e.getSQLState())
                    && attempt < maxRetries - 1) {
                // Serialization error, retry
                continue;
            }
            throw new RuntimeException(e);
        }
    }
    throw new RuntimeException(
            "Max retries exceeded");
}
```

### Reducing Contention

| Strategy | Implementation |
|----------|---------------|
| Short transactions | Keep transactions under 100ms |
| Index on hot columns | Reduce scan range to minimize lock contention |
| `AS OF SYSTEM TIME` | Read-only queries avoid write contention |
| SELECT FOR UPDATE | Explicit locking prevents retry loops |
| Batch size limits | Process 100-1000 rows per transaction |

## Contention Monitoring

```sql
-- View contention events
SELECT * FROM crdb_internal.cluster_contention_events
ORDER BY count DESC
LIMIT 20;

-- Transaction contention statistics
SELECT * FROM crdb_internal.transaction_contention_events
ORDER BY contention_duration DESC
LIMIT 20;

-- Hot ranges (most requests)
SELECT range_id, start_key, qps, write_bytes_per_second
FROM crdb_internal.ranges_no_leases
ORDER BY qps DESC
LIMIT 10;
```

## Index Optimization

### STORING for Covering Queries

```sql
-- Avoids table lookup (index-only scan)
CREATE INDEX idx_orders_status_storing
    ON orders (status)
    STORING (total_amount, customer_id, created_at);

-- Verify index-only scan in EXPLAIN
EXPLAIN SELECT total_amount, customer_id
FROM orders WHERE status = 'PENDING';
```

### Inverted Index for JSONB

```sql
CREATE INVERTED INDEX idx_events_data ON events (data);

-- Query using inverted index
SELECT * FROM events
WHERE data @> '{"type": "purchase"}';
```

## Multi-Region Query Optimization

```sql
-- Query with region filter (avoids cross-region reads)
SELECT * FROM users
WHERE region = 'us-east1'
  AND email = 'user@example.com';

-- Global table read (fast from any region)
SELECT * FROM countries WHERE code = 'US';
```

| Query Type | LOCALITY | Performance |
|-----------|----------|-------------|
| Local region filter | REGIONAL BY ROW | Fast (single region) |
| Cross-region scan | REGIONAL BY ROW | Slow (scatter-gather) |
| Global table read | GLOBAL | Fast everywhere |
| Global table write | GLOBAL | Slow (all regions must ack) |

## Query Tuning Parameters

| Parameter | Default | Tuning |
|-----------|---------|--------|
| `distsql` | auto | Force distributed: `SET distsql = always` |
| `vectorize` | on | Keep enabled for batch processing |
| `reorder_joins_limit` | 8 | Increase for complex JOINs |
| `statement_timeout` | 0 | Set per environment |
| `idle_in_transaction_session_timeout` | 0 | Set to prevent idle locks |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No serialization retry | Transaction failures in application | Implement retry loop for `40001` errors |
| Full scan on large table | Slow cross-range query | Create appropriate indexes |
| Long-running transactions | Lock contention, range splits | Keep under 100ms |
| Cross-region writes without LOCALITY | High latency | Use REGIONAL BY ROW |
| Missing STORING | Table lookup per index hit | Add STORING for accessed columns |
| Ignoring contention metrics | Hidden performance issues | Monitor `cluster_contention_events` |
