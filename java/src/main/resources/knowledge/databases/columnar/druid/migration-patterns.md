# Apache Druid — Migration Patterns

## Overview

Druid uses **immutable segments** and has no traditional DDL migration framework. Schema changes require re-ingestion, compaction, or supervisor updates.

| Approach | Use Case | Impact |
|----------|----------|--------|
| Re-ingestion | Schema changes, add/remove dimensions | Rewrites affected segments |
| Compaction | Consolidate small segments, change granularity | Background task |
| Supervisor update | Streaming schema changes | New segments use new schema |
| Lookup update | Dimension enrichment changes | Hot-reloadable |

## Schema Evolution Strategies

### Adding a Dimension

**Streaming (Kafka supervisor):**

```json
{
  "type": "kafka",
  "dataSchema": {
    "dimensionsSpec": {
      "dimensions": [
        "page", "country", "browser",
        "device_type"
      ]
    }
  }
}
```

Suspend supervisor, update spec, resume. New segments include the new dimension; old segments return `null` for it.

**Batch (re-ingestion):**

```sql
REPLACE INTO "page_views" OVERWRITE ALL
SELECT
  __time,
  "page", "country", "browser",
  "device_type",
  "count", "total_bytes"
FROM "page_views"
PARTITIONED BY DAY
```

### Removing a Dimension

1. Update ingestion spec to exclude the dimension
2. Run compaction to rewrite segments without the dimension
3. Old segments retain the dimension until compacted

### Changing Metric Aggregation

Metrics are aggregated at ingestion. Changing aggregation type requires full re-ingestion:

```sql
REPLACE INTO "page_views" OVERWRITE ALL
SELECT
  TIME_FLOOR(__time, 'PT1H') AS __time,
  "page", "country",
  COUNT(*) AS event_count,
  APPROX_COUNT_DISTINCT_DS_HLL("user_id") AS unique_users
FROM "page_views_raw"
GROUP BY 1, 2, 3
PARTITIONED BY DAY
```

## Compaction

### Auto-Compaction

```json
{
  "dataSource": "page_views",
  "taskPriority": 25,
  "inputSpec": {
    "type": "interval",
    "interval": "2024-01-01/2024-12-31"
  },
  "granularitySpec": {
    "segmentGranularity": "DAY",
    "queryGranularity": "HOUR"
  },
  "tuningConfig": {
    "type": "index_parallel",
    "maxRowsPerSegment": 5000000,
    "maxNumConcurrentSubTasks": 4
  }
}
```

### Compaction Use Cases

| Scenario | Configuration |
|----------|---------------|
| Merge small segments | Increase `maxRowsPerSegment` |
| Change segment granularity | Modify `segmentGranularity` |
| Change query granularity | Modify `queryGranularity` (re-rolls up data) |
| Apply new roll-up | Add `metricsSpec` to compaction task |
| Drop column | Omit column from `dimensionsSpec` in compaction |

## Lookup Management

### JDBC Lookup Update

```json
{
  "type": "cachedNamespace",
  "extractionNamespace": {
    "type": "jdbc",
    "connectorConfig": {
      "connectURI": "jdbc:mysql://host/db"
    },
    "table": "countries_v2",
    "keyColumn": "code",
    "valueColumn": "name",
    "pollPeriod": "PT1H"
  }
}
```

Lookups are hot-reloaded without re-ingestion. Update the source table and let polling refresh.

### Map Lookup Update

```
POST /druid/coordinator/v1/lookups/tier/__default/country_lookup
{
  "version": "v2",
  "lookupExtractorFactory": {
    "type": "map",
    "map": {"US": "United States", "BR": "Brazil"}
  }
}
```

## Retention Policies

### Rule-Based Retention

```json
[
  {
    "type": "loadByInterval",
    "interval": "2024-01-01/2025-01-01",
    "tieredReplicants": {"_default_tier": 2}
  },
  {
    "type": "dropForever"
  }
]
```

| Rule Type | Purpose |
|-----------|---------|
| `loadByPeriod` | Keep last N periods (e.g., P90D = 90 days) |
| `loadByInterval` | Keep specific time range |
| `dropByPeriod` | Drop data older than N periods |
| `dropForever` | Drop everything not matched by prior rules |

### Applying Rules

```
POST /druid/coordinator/v1/rules/page_views
[
  {"type": "loadByPeriod", "period": "P90D",
   "tieredReplicants": {"_default_tier": 2}},
  {"type": "dropForever"}
]
```

## Supervisor Lifecycle

| Operation | API | Use Case |
|-----------|-----|----------|
| Create/Update | `POST /druid/indexer/v1/supervisor` | Deploy new or updated spec |
| Suspend | `POST /druid/indexer/v1/supervisor/{id}/suspend` | Pause ingestion |
| Resume | `POST /druid/indexer/v1/supervisor/{id}/resume` | Resume ingestion |
| Reset | `POST /druid/indexer/v1/supervisor/{id}/reset` | Reset offsets (re-consume) |
| Terminate | `POST /druid/indexer/v1/supervisor/{id}/terminate` | Stop permanently |

## Migration Versioning

Since Druid has no DDL migration framework, version ingestion specs in Git:

```
druid/
|-- datasources/
|   |-- page_views/
|   |   |-- V001__initial_schema.json
|   |   |-- V002__add_device_type.json
|   |   +-- V003__change_rollup_granularity.json
|   +-- order_events/
|       +-- V001__initial_schema.json
+-- lookups/
    |-- country_lookup.json
    +-- device_lookup.json
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Schema change without re-ingestion plan | Mixed schemas across segments | Plan re-ingestion or accept nulls |
| Compacting during peak hours | Resource contention | Schedule compaction in off-peak windows |
| Ignoring segment count | Coordinator overhead | Monitor and compact regularly |
| Manual segment deletion | Inconsistent state | Use retention rules or `markUnused` API |
| Changing `__time` column | Breaks all queries | Never change the time column |
