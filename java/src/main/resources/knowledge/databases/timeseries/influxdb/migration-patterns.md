# InfluxDB — Migration Patterns

## Overview

InfluxDB has no DDL migration framework. Schema evolution is implicit (schema-on-write) with explicit management of buckets, tasks, and retention.

| Approach | Use Case | Tool |
|----------|----------|------|
| **InfluxDB CLI** | Bucket management, DBRP mapping | `influx` CLI |
| **API** | Programmatic bucket/task management | HTTP REST API |
| **Flux tasks** | Downsampling, data transformation | Flux language |
| **Templates** | Reproducible configuration | InfluxDB templates |

## Bucket Management

### Create Buckets

```bash
# Create bucket with retention
influx bucket create \
  --name metrics_raw \
  --retention 7d \
  --org my-org

# Create bucket with shard group duration
influx bucket create \
  --name metrics_rollup_1h \
  --retention 365d \
  --shard-group-duration 7d \
  --org my-org
```

### Update Retention

```bash
# Extend retention period
influx bucket update \
  --id <bucket-id> \
  --retention 30d

# Set infinite retention (0 = forever)
influx bucket update \
  --id <bucket-id> \
  --retention 0
```

## Schema Evolution

InfluxDB uses schema-on-write. New fields and tags are added automatically on first write:

| Change | Impact | Action Required |
|--------|--------|----------------|
| Add new field | None, auto-detected | Write data with new field |
| Add new tag | None, auto-detected | Write data with new tag |
| Remove field | Old data retains field | Stop writing field; old data expires via retention |
| Change field type | **Conflict** | Create new field name, migrate with task |
| Rename measurement | Not supported directly | Write to new measurement, backfill |

### Field Type Change (Breaking)

```
# Original: latency as integer
http_requests,host=srv01 latency=45i

# Error: cannot write float to integer field
http_requests,host=srv01 latency=45.2

# Solution: use new field name
http_requests,host=srv01 latency_ms=45.2
```

**Rule:** Field type is set on first write and cannot be changed. Use a new field name for type changes.

## Flux Tasks for Data Migration

### Downsampling Task

```flux
option task = {
    name: "downsample_5m",
    every: 5m,
    offset: 30s
}

from(bucket: "metrics_raw")
    |> range(start: -task.every)
    |> filter(fn: (r) => r._measurement == "cpu")
    |> aggregateWindow(every: 5m, fn: mean)
    |> to(bucket: "metrics_rollup_5m")
```

### Data Backfill Task

```flux
option task = {
    name: "backfill_5m_rollup",
    every: 1h
}

from(bucket: "metrics_raw")
    |> range(start: -30d, stop: -7d)
    |> filter(fn: (r) => r._measurement == "cpu")
    |> aggregateWindow(every: 5m, fn: mean)
    |> to(bucket: "metrics_rollup_5m")
```

### Cross-Measurement Migration

```flux
option task = {
    name: "migrate_cpu_metrics",
    every: 10m
}

from(bucket: "metrics_raw")
    |> range(start: -task.every)
    |> filter(fn: (r) => r._measurement == "cpu_old")
    |> set(key: "_measurement", value: "cpu")
    |> to(bucket: "metrics_raw")
```

## Cardinality Management

### Check Cardinality

```bash
# Show series cardinality
influx query 'import "influxdata/influxdb"
  influxdb.cardinality(bucket: "metrics_raw",
    start: -30d)' --org my-org
```

### Reduce Cardinality

```flux
// Delete high-cardinality series
import "influxdata/influxdb"

influxdb.cardinality(
    bucket: "metrics_raw",
    start: -30d,
    predicate: (r) => r._measurement == "problematic"
)
```

```bash
# Delete series by tag
influx delete \
  --bucket metrics_raw \
  --start '2024-01-01T00:00:00Z' \
  --stop '2024-12-31T23:59:59Z' \
  --predicate '_measurement="old_metric"'
```

## DBRP Mapping (v1 Compatibility)

```bash
# Create DBRP mapping for InfluxQL compatibility
influx v1 dbrp create \
  --db legacy_db \
  --rp autogen \
  --bucket-id <bucket-id> \
  --default
```

## Template-Based Migration

```yaml
# template.yml
apiVersion: influxdata.com/v2alpha1
kind: Bucket
metadata:
  name: metrics_raw
spec:
  retentionRules:
    - everySeconds: 604800  # 7 days
      type: expire
---
apiVersion: influxdata.com/v2alpha1
kind: Task
metadata:
  name: downsample_5m
spec:
  every: 5m
  query: |
    from(bucket: "metrics_raw")
      |> range(start: -task.every)
      |> aggregateWindow(every: 5m, fn: mean)
      |> to(bucket: "metrics_rollup_5m")
```

```bash
influx apply -f template.yml --org my-org
```

## Migration Versioning

```
influxdb/
|-- buckets/
|   |-- V001__create_raw_bucket.sh
|   |-- V002__create_rollup_buckets.sh
|   +-- V003__update_retention.sh
|-- tasks/
|   |-- downsample_5m.flux
|   +-- downsample_1h.flux
+-- templates/
    +-- full_setup.yml
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Changing field types | Write rejection, mixed types | Create new field name |
| No retention policy | Unbounded disk growth | Set retention on every bucket |
| Backfill without batching | Memory exhaustion | Process in time windows |
| Ignoring cardinality | OOM, slow queries | Monitor and cap at < 1M series |
| Manual schema tracking | Schema drift | Use InfluxDB templates |
