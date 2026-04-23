# Partitioning Decision Tree

Flowchart for selecting the optimal partitioning strategy based on data characteristics and query patterns.

## Decision Flow

### Step 1: Do You Need Partitioning?

| Indicator | Threshold | Action |
|-----------|-----------|--------|
| Table row count | > 100M rows | Consider partitioning |
| Table size | > 100 GB | Consider partitioning |
| Query latency | Degrading despite indexes | Consider partitioning |
| Maintenance time | VACUUM/REINDEX > 1 hour | Consider partitioning |
| Data lifecycle | Need to archive old data | Consider partitioning |

If none apply: **Do not partition** -- partitioning adds complexity.

### Step 2: Choose Partition Strategy

```
Is data time-series or has natural date ordering?
  YES -> Range Partitioning (by date)
  NO  -> Are queries filtered by a categorical column?
           YES -> List Partitioning (by category)
           NO  -> Do you need even distribution across partitions?
                    YES -> Hash Partitioning (by ID or composite key)
                    NO  -> Consider composite partitioning
```

### Step 3: Range Partitioning Details

| Data Volume | Partition Interval | Example |
|-------------|-------------------|---------|
| < 10M rows/month | Monthly | `transactions_2026_03` |
| 10M-100M rows/month | Weekly | `transactions_2026_w13` |
| > 100M rows/month | Daily | `transactions_2026_03_26` |
| > 1B rows/month | Consider sharding | Shard by tenant + partition by date |

### Step 4: Hash Partitioning Details

| Total Rows | Partition Count | Rationale |
|------------|----------------|-----------|
| 100M - 500M | 8-16 | Moderate parallelism |
| 500M - 2B | 16-64 | High parallelism |
| > 2B | 64-256 | Maximum parallelism, consider sharding |

Choose partition count as power of 2 for easier rebalancing.

### Step 5: List Partitioning Details

| Use Case | Partition Key | Example |
|----------|--------------|---------|
| Multi-tenant | tenant_id | One partition per tenant |
| Geographic | region | Partitions: us, eu, apac |
| Status lifecycle | status | active, archived, deleted |

## Sharding vs Partitioning

| Aspect | Partitioning | Sharding |
|--------|-------------|----------|
| Scope | Single database | Multiple databases |
| Complexity | Low-Medium | High |
| Cross-partition queries | Supported | Expensive |
| Scalability limit | Single server | Horizontal |
| Use when | Single server sufficient | Need horizontal scale |

## Partition Maintenance

| Task | Frequency | Description |
|------|-----------|-------------|
| Create future partitions | Automated, ahead of time | Prevent insert failures |
| Detach old partitions | Per retention policy | Move to archive or drop |
| Monitor partition sizes | Weekly | Detect skew early |
| Reindex partitions | Monthly or after bulk operations | Maintain query performance |
| Analyze partition stats | After major data changes | Keep query planner accurate |

## Anti-Patterns

- Partitioning tables with < 10M rows (overhead exceeds benefit)
- Too many partitions (> 1000 causes planner overhead)
- Partition key not matching query patterns (forces partition scans)
- Cross-partition foreign keys (use application-level enforcement)
- Not pre-creating future partitions (causes insert failures)
