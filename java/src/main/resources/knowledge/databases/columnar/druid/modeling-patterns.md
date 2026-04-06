# Apache Druid — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **28.x** | Multi-stage query engine (MSQ), SQL improvements | Current stable |
| **29.x** | Improved MSQ, async query results | Latest |
| **30.x** | Enhanced SQL compatibility, catalog improvements | Upcoming |

## Core Concepts

### Datasource Design

A Druid datasource is analogous to a table. Data is stored in immutable **segments**.

| Concept | Detail |
|---------|--------|
| Segment | Immutable chunk of data, partitioned by time |
| Segment granularity | Time range per segment (HOUR, DAY, MONTH) |
| Query granularity | Minimum time resolution in stored data |
| Roll-up | Pre-aggregation at ingestion time |
| Denormalization | JOINs at query time are expensive; pre-join data |

### Column Types

| Type | Druid Type | Use Case |
|------|-----------|----------|
| Timestamp | `__time` (required) | Primary time column, always present |
| Dimensions | `string`, `long`, `float`, `double` | Filter and group-by columns |
| Metrics | `long`, `float`, `double`, `complex` | Aggregated values (sum, count, HLL) |
| Multi-value dimensions | `string` (array) | Tags, categories (one row = multiple values) |

### Roll-Up Design

Roll-up pre-aggregates rows with identical dimension values at ingestion:

```json
{
  "type": "uniform",
  "segmentGranularity": "DAY",
  "queryGranularity": "HOUR",
  "rollup": true
}
```

| Setting | Impact |
|---------|--------|
| `rollup: true` | Rows with same dimensions + query granularity merged |
| `queryGranularity: HOUR` | Timestamps rounded to hour; finer detail lost |
| `queryGranularity: NONE` | No roll-up; raw data preserved |

**Rule:** Enable roll-up when raw row detail is not needed. A 10:1 roll-up ratio is common for event data.

## Ingestion Patterns

### Batch Ingestion (MSQ)

```sql
INSERT INTO "page_views"
SELECT
  TIME_FLOOR("timestamp", 'PT1H') AS __time,
  "page" AS page,
  "country" AS country,
  COUNT(*) AS view_count,
  SUM("bytes") AS total_bytes
FROM TABLE(EXTERN(
  '{"type":"s3","uris":["s3://bucket/data/*.json"]}',
  '{"type":"json"}',
  '[{"name":"timestamp","type":"string"},
    {"name":"page","type":"string"},
    {"name":"country","type":"string"},
    {"name":"bytes","type":"long"}]'
))
GROUP BY 1, 2, 3
PARTITIONED BY DAY
```

### Streaming Ingestion (Kafka)

```json
{
  "type": "kafka",
  "dataSchema": {
    "dataSource": "page_views_stream",
    "timestampSpec": {"column": "timestamp", "format": "iso"},
    "dimensionsSpec": {
      "dimensions": ["page", "country", "browser"]
    },
    "metricsSpec": [
      {"type": "count", "name": "count"},
      {"type": "longSum", "name": "total_bytes", "fieldName": "bytes"}
    ],
    "granularitySpec": {
      "segmentGranularity": "HOUR",
      "queryGranularity": "MINUTE",
      "rollup": true
    }
  },
  "ioConfig": {
    "topic": "page-views",
    "consumerProperties": {"bootstrap.servers": "kafka:9092"}
  }
}
```

## Segment Design

| Parameter | Small Clusters | Large Clusters |
|-----------|---------------|----------------|
| Segment size | 300-700 MB | 500 MB - 1 GB |
| Segment granularity | HOUR or DAY | HOUR |
| Segments per datasource | < 10,000 | < 50,000 |
| Compaction | Auto-compact small segments | Scheduled compaction tasks |

### Partitioning Within Segments

| Strategy | Use Case | Syntax |
|----------|----------|--------|
| Dynamic | Default, automatic sizing | `PARTITIONED BY DAY` |
| Hashed | Even distribution | `CLUSTERED BY "dim" INTO N` |
| Range | Ordered queries | `CLUSTERED BY "dim"` |

## Lookup Tables (Dimension Enrichment)

```json
{
  "type": "cachedNamespace",
  "extractionNamespace": {
    "type": "jdbc",
    "connectorConfig": {
      "connectURI": "jdbc:mysql://host/db"
    },
    "table": "countries",
    "keyColumn": "code",
    "valueColumn": "name",
    "pollPeriod": "PT1H"
  }
}
```

Use lookups instead of JOINs for static dimension enrichment.

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Datasource | snake_case | `page_views`, `order_events` |
| Dimension | snake_case | `user_id`, `event_type` |
| Metric | snake_case, descriptive | `total_bytes`, `view_count` |
| Lookup | snake_case with prefix | `lookup_country`, `lookup_device` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| High-cardinality dimensions | Segment bloat, slow group-by | Keep dimension cardinality < 1M |
| No roll-up on event data | Excessive storage, slow queries | Enable roll-up with appropriate granularity |
| Fine segment granularity | Too many small segments | Use HOUR or DAY, not MINUTE |
| JOINs at query time | Expensive, limited support | Pre-join at ingestion or use lookups |
| Missing `__time` filter | Full datasource scan | Always filter on `__time` range |
| Storing raw JSON as dimension | Poor compression, no indexing | Extract fields at ingestion |
