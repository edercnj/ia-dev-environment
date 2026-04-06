# InfluxDB — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **2.7** | Flux language GA, tasks, bucket management | OSS stable |
| **3.0 (Edge)** | Apache Arrow, DataFusion, Parquet storage | New architecture |
| **Cloud** | Managed, serverless, InfluxQL + SQL support | Hosted |

## Core Data Model

### Line Protocol

InfluxDB ingests data via line protocol:

```
measurement,tag1=value1,tag2=value2 field1=value1,field2=value2 timestamp
```

Examples:

```
cpu,host=server01,region=us-east usage_user=23.5,usage_system=5.2 1705312200000000000
http_requests,method=GET,path=/api/v1/orders count=1i,latency=45.2 1705312200000000000
temperature,sensor=t001,room=server-room value=22.5 1705312200000000000
```

### Data Elements

| Element | Purpose | Example | Notes |
|---------|---------|---------|-------|
| Measurement | Logical grouping (like a table) | `cpu`, `http_requests` | snake_case |
| Tags | Indexed metadata for filtering | `host=server01` | Low cardinality, indexed |
| Fields | Measured values | `usage=23.5` | Not indexed, high cardinality OK |
| Timestamp | Time of measurement | nanosecond precision | UTC, nanosecond Unix epoch |

## Bucket Design

Buckets are the top-level container (replacing databases in v1):

```
Organization
|-- Bucket: metrics_raw (retention: 7 days)
|-- Bucket: metrics_rollup_5m (retention: 30 days)
|-- Bucket: metrics_rollup_1h (retention: 365 days)
+-- Bucket: metrics_rollup_1d (retention: forever)
```

| Parameter | Recommendation | Notes |
|-----------|---------------|-------|
| Retention | Align with data lifecycle | Automatic deletion |
| Shard duration | Auto (based on retention) | Controls file granularity |
| Name convention | `{domain}_{resolution}` | `metrics_raw`, `metrics_5m` |

## Tag Design

### Tag vs Field Decision

```
# Good: host and region as tags (used in WHERE, low cardinality)
cpu,host=server01,region=us-east usage=23.5

# Bad: request_id as tag (high cardinality, index bloat)
http_requests,request_id=abc-123-xyz count=1i
```

| Tag Rule | Detail |
|----------|--------|
| Index | Tags are always indexed; fields are not |
| Cardinality limit | Keep total series cardinality < 1M per bucket |
| Naming | snake_case, descriptive | 
| Avoid | UUIDs, timestamps, session IDs as tags |

### Cardinality Estimation

```
Total series = measurement_count * tag1_cardinality * tag2_cardinality * ...
```

Example: 10 measurements x 100 hosts x 5 regions x 3 environments = 15,000 series (healthy).

## Field Design

```
# Multiple fields per measurement (wide schema, preferred)
cpu,host=server01 usage_user=23.5,usage_system=5.2,usage_idle=71.3

# Avoid: separate measurements per field (narrow, more series)
cpu_user,host=server01 value=23.5
cpu_system,host=server01 value=5.2
```

| Field Type | Suffix Convention | Example |
|-----------|------------------|---------|
| Float | None or `_pct`, `_ratio` | `usage=23.5`, `cpu_pct=78.0` |
| Integer | `_count`, `_total` | `request_count=1024i` |
| String | `_status`, `_message` | `status="healthy"` |
| Boolean | `_enabled`, `_active` | `active=true` |

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Measurement | snake_case, noun | `cpu`, `http_requests`, `disk_io` |
| Tag key | snake_case | `host`, `region`, `environment` |
| Tag value | Original case (not encoded) | `server-01`, `us-east-1` |
| Field key | snake_case, with unit hint | `usage_pct`, `latency_ms`, `bytes_total` |
| Bucket | snake_case with resolution | `metrics_raw`, `metrics_5m` |

## Schema Patterns

### Application Metrics

```
http_requests,method=GET,path=/api/orders,status=200 count=1i,latency_ms=45.2
http_requests,method=POST,path=/api/orders,status=201 count=1i,latency_ms=120.5
```

### Infrastructure Metrics

```
system_cpu,host=srv01,cpu=cpu0 usage_user=23.5,usage_system=5.2,usage_idle=71.3
system_memory,host=srv01 used_bytes=8589934592i,available_bytes=8589934592i
system_disk,host=srv01,device=sda1 read_bytes=1048576i,write_bytes=524288i
```

### IoT Sensor Data

```
temperature,sensor_id=t001,location=room-a value=22.5
humidity,sensor_id=h001,location=room-a value=45.2
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| High-cardinality tags | Index bloat, OOM | Move to fields or reduce cardinality |
| UUID as tag | Unbounded series | Store as field, never as tag |
| One field per measurement | Series multiplication | Use wide schema (multiple fields) |
| No retention policy | Unbounded storage growth | Set retention on every bucket |
| Timestamps in tags | Infinite cardinality | Timestamps are automatic |
| String fields for metrics | Cannot aggregate | Use numeric types for measured values |
