# TiDB — Query Optimization

## EXPLAIN ANALYZE

```sql
EXPLAIN ANALYZE
SELECT o.id, o.total_amount, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING'
  AND o.created_at > NOW() - INTERVAL 7 DAY;
```

### Key Metrics

| Metric | Good | Bad |
|--------|------|-----|
| Operator | `IndexRangeScan`, `IndexLookUp` | `TableFullScan` |
| `execution info: rows` | Close to actual result | Orders of magnitude higher |
| `cop_task: num` | Low (few coprocessor tasks) | Very high (many regions scanned) |
| `time` | < 100ms for OLTP | > 1s |
| `memory` | Proportional to result set | Large (bad plan) |

**Rule:** Every production OLTP query MUST use index scans. `TableFullScan` on tables with > 10,000 rows is PROHIBITED for OLTP workloads.

## Coprocessor Pushdown

TiDB pushes computation to TiKV/TiFlash coprocessor for efficiency:

```sql
-- Pushed down to TiKV (filter + aggregate executed on storage)
SELECT status, COUNT(*) FROM orders
WHERE created_at > '2024-01-01'
GROUP BY status;

-- Verify pushdown in EXPLAIN
EXPLAIN SELECT status, COUNT(*) FROM orders
WHERE created_at > '2024-01-01' GROUP BY status;
-- Look for: cop[tikv] or cop[tiflash]
```

### Pushdown-Friendly Operations

| Operation | TiKV Pushdown | TiFlash Pushdown |
|-----------|--------------|-----------------|
| Filter (WHERE) | Yes | Yes |
| Aggregation (GROUP BY) | Yes | Yes (MPP) |
| TopN (ORDER BY + LIMIT) | Yes | Yes |
| JOIN | No (TiDB layer) | Yes (MPP broadcast/hash) |
| Subquery | No | Partially |

## Stale Reads

Read data at a past timestamp for reduced latency and no lock contention:

```sql
-- Read from 5 seconds ago
SELECT * FROM orders
AS OF TIMESTAMP NOW() - INTERVAL 5 SECOND
WHERE customer_id = 123;

-- Read at specific timestamp
SELECT * FROM orders
AS OF TIMESTAMP '2024-01-15 10:30:00'
WHERE status = 'COMPLETED';

-- Session-level stale reads
SET @@tidb_read_staleness = '-5';
SELECT * FROM orders WHERE customer_id = 123;
```

| Use Case | Staleness | Syntax |
|----------|-----------|--------|
| Dashboard queries | 5-30 seconds | `AS OF TIMESTAMP NOW() - INTERVAL 5 SECOND` |
| Reporting | Minutes | `AS OF TIMESTAMP NOW() - INTERVAL 5 MINUTE` |
| Session-level | Configurable | `SET @@tidb_read_staleness = '-5'` |

**Rule:** Use stale reads for all read-only queries that tolerate staleness. Reduces TiKV leader load and enables follower reads.

## Hot Region Monitoring

```sql
-- Find hot regions
SELECT * FROM information_schema.tikv_region_status
ORDER BY WRITTEN_BYTES DESC
LIMIT 10;

-- Find hot tables
SELECT TABLE_NAME, REGION_COUNT,
       SUM(WRITTEN_BYTES) AS total_writes
FROM information_schema.tikv_region_status r
JOIN information_schema.tables t
    ON r.DB_NAME = t.TABLE_SCHEMA
    AND r.TABLE_NAME = t.TABLE_NAME
GROUP BY TABLE_NAME, REGION_COUNT
ORDER BY total_writes DESC;
```

### Hot Region Mitigation

| Cause | Solution |
|-------|----------|
| Sequential PK inserts | Use `AUTO_RANDOM` |
| Single index hot spot | Add shard bits or hash-based distribution |
| Large single-partition writes | Pre-split regions with `SPLIT TABLE` |
| Frequently updated counter | Use application-level sharding |

```sql
-- Pre-split table into regions
SPLIT TABLE orders BETWEEN (0) AND (1000000) REGIONS 16;

-- Pre-split by prefix
SPLIT TABLE orders BY ('A'), ('M'), ('Z');
```

## TiFlash Query Optimization

```sql
-- Force TiFlash for analytical queries
SET @@tidb_isolation_read_engines = 'tiflash';

-- Enable MPP mode for distributed TiFlash execution
SET @@tidb_allow_mpp = 1;
SET @@tidb_enforce_mpp = 1;

-- Analytical query with MPP
SELECT DATE(created_at) AS day,
       status,
       COUNT(*) AS cnt,
       SUM(total_amount) AS total
FROM orders
WHERE created_at > '2024-01-01'
GROUP BY day, status
ORDER BY day, total DESC;
```

## SQL Hints

```sql
-- Force index usage
SELECT /*+ USE_INDEX(orders, idx_orders_status) */
    id, total_amount
FROM orders WHERE status = 'PENDING';

-- Force hash join
SELECT /*+ HASH_JOIN(orders, customers) */
    o.id, c.name
FROM orders o JOIN customers c ON o.customer_id = c.id;

-- Force TiFlash read
SELECT /*+ READ_FROM_STORAGE(TIFLASH[orders]) */
    status, COUNT(*) FROM orders GROUP BY status;

-- Force TiKV read
SELECT /*+ READ_FROM_STORAGE(TIKV[orders]) */
    * FROM orders WHERE id = 12345;

-- Memory limit per query
SELECT /*+ MEMORY_QUOTA(1 GB) */
    * FROM large_table WHERE condition;
```

## Query Tuning Parameters

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `tidb_distsql_scan_concurrency` | 15 | Parallel scan goroutines |
| `tidb_index_lookup_concurrency` | 4 | Parallel index lookup goroutines |
| `tidb_executor_concurrency` | 5 | General executor concurrency |
| `tidb_mem_quota_query` | 1 GB | Max memory per query |
| `tidb_opt_agg_push_down` | 0 | Push aggregation to coprocessor |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| `TableFullScan` for OLTP | Slow, high TiKV load | Create appropriate indexes |
| OLAP on TiKV | Competes with OLTP workload | Use TiFlash for analytics |
| No stale reads for reports | Unnecessary leader contention | Enable stale reads |
| Ignoring hot regions | Uneven load distribution | Monitor and use `AUTO_RANDOM` |
| Large result sets without LIMIT | OOM on TiDB server | Always paginate or LIMIT |
| Missing SQL hints for critical queries | Optimizer may choose bad plan | Pin plan with hints |
