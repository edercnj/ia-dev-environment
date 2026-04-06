# Columnar/OLAP Database Principles

## Columnar vs Row Storage

| Aspect | Row-Oriented (OLTP) | Column-Oriented (OLAP) |
|--------|---------------------|------------------------|
| Storage layout | Rows stored contiguously | Columns stored contiguously |
| Read pattern | Full row retrieval | Selected columns only |
| Write pattern | Single-row inserts/updates | Batch inserts, rare updates |
| Compression | Low (mixed types per block) | High (same type per block) |
| Best for | Transactional workloads | Analytical queries, aggregations |
| Scan efficiency | Full row scan for any column | Only reads requested columns |

## Core Concepts

### Vectorized Execution

Processes data in batches (vectors) of 1000+ values instead of row-by-row:

| Benefit | Detail |
|---------|--------|
| CPU cache efficiency | Operates on contiguous memory blocks |
| SIMD utilization | Single instruction processes multiple values |
| Branch prediction | Uniform types reduce branch mispredictions |
| Throughput | 10-100x faster than row-at-a-time for scans |

### Compression Techniques

| Technique | When Applied | Compression Ratio |
|-----------|-------------|-------------------|
| Dictionary encoding | Low-cardinality columns (status, country) | 10-100x |
| Run-length encoding (RLE) | Sorted columns with repeating values | 5-50x |
| Delta encoding | Timestamps, sequential IDs | 2-10x |
| LZ4 / ZSTD | General-purpose block compression | 2-5x |
| Bit-packing | Small integer ranges | 2-8x |

**Rule:** Sort keys MUST align with the most common filter/group-by columns for maximum compression.

### Massively Parallel Processing (MPP)

| Concept | Detail |
|---------|--------|
| Data distribution | Rows distributed across nodes by partition key |
| Query parallelism | Each node processes its local partition |
| Shuffle operations | Data exchange between nodes for JOINs/aggregations |
| Coordinator node | Receives query, distributes, merges results |

## When to Use Columnar/OLAP

### Choose Columnar When

| Criterion | Threshold |
|-----------|-----------|
| Query pattern | Aggregations over millions/billions of rows |
| Column selectivity | Queries touch < 20% of total columns |
| Write pattern | Batch inserts, append-only or infrequent updates |
| Data volume | > 100 GB analytical dataset |
| Latency tolerance | Sub-second to seconds for complex aggregations |
| Concurrency | Low-to-moderate concurrent queries (10-100) |

### Anti-Patterns (When NOT to Use Columnar)

| Scenario | Why Columnar is Wrong | Better Alternative |
|----------|----------------------|-------------------|
| Point lookups by PK | Column scan overhead for single row | PostgreSQL, MySQL |
| High-concurrency OLTP | Not designed for row-level transactions | SQL databases |
| Frequent single-row updates | Immutable storage requires rewrite | PostgreSQL, MongoDB |
| Small datasets (< 1 GB) | Overhead exceeds benefit | PostgreSQL with indexes |
| Real-time event ingestion | Use time-series optimized storage | InfluxDB, TimescaleDB |
| Full-text search | No inverted index support | Elasticsearch, OpenSearch |

## Partitioning Strategies

| Strategy | Use Case | Example |
|----------|----------|---------|
| Time-based | Event data, logs, metrics | Partition by day/month |
| Hash-based | Even distribution across shards | Partition by user_id hash |
| Range-based | Ordered data with range queries | Partition by amount range |

**Rule:** Partition key MUST appear in WHERE clauses of 90%+ of queries to enable partition pruning.

## Data Modeling for Analytics

### Denormalization

Columnar databases favor wide, denormalized tables over JOINs:

| Principle | Rule |
|-----------|------|
| Star schema preferred | Fact tables + dimension columns denormalized |
| Minimize JOINs | Pre-join at ingestion time |
| Pre-aggregate | Materialized views or summary tables for common aggregations |
| Immutable facts | Append-only design; corrections as new rows with flags |

### Sort Key Selection

| Consideration | Rule |
|---------------|------|
| Primary filter | First sort key = most common WHERE clause column |
| Time column | Almost always in sort key for time-series analytics |
| Cardinality | Low-cardinality columns first for better compression |
| Query alignment | Sort order matches GROUP BY / ORDER BY patterns |

## CAP Positioning

| System | CAP Choice | Consistency Model | Notes |
|--------|-----------|-------------------|-------|
| ClickHouse | AP (tunable) | Eventual (async replication) | Strong on single node |
| Druid | AP | Eventual (segment-based) | Immutable segments, consistent after publish |

## Framework Integration

| Database | Java Client | Protocol |
|----------|------------|----------|
| ClickHouse | `com.clickhouse:clickhouse-jdbc` | HTTP/Native TCP |
| Druid | HTTP client (REST API) | HTTP (JSON) |
| Druid (JDBC) | `org.apache.calcite.avatica` | Avatica JDBC |

## Sensitive Data

| Rule | Detail |
|------|--------|
| Column encryption | Encrypt PII columns before ingestion |
| Access control | Row-level or column-level policies where supported |
| Audit logging | Track all query access to sensitive columns |
| Data masking | Apply masking views for non-privileged users |
| Retention | Enforce TTL via partition dropping, not row deletion |
