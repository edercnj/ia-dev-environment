# Performance Metrics Guide

## RED Method (Request-Scoped Metrics)

The RED method focuses on metrics that matter from the user's perspective.

### Rate

- **Definition**: Number of requests per second (RPS) the system handles
- **Measurement**: Count requests over sliding window (1 minute typical)
- **Alerting**: Drop below baseline indicates capacity issue or upstream failure
- **Dashboard**: Time-series graph with baseline overlay

### Errors

- **Definition**: Percentage of requests resulting in errors
- **Categories**: Client errors (4xx), server errors (5xx), timeouts
- **Measurement**: `error_rate = error_count / total_count * 100`
- **Alerting**: Error rate > 0.1% triggers investigation; > 1% triggers incident
- **Dashboard**: Stacked area chart by error category

### Duration

- **Definition**: Time taken to process each request
- **Percentiles**: p50 (median), p95 (baseline), p99 (tail latency)
- **Measurement**: Histogram with configurable buckets
- **Alerting**: p99 exceeding SLO threshold triggers alert
- **Dashboard**: Heatmap or percentile overlay chart

## USE Method (Resource-Scoped Metrics)

The USE method focuses on infrastructure resource health.

### Utilization

- **Definition**: Percentage of time a resource is busy
- **Resources**: CPU, memory, disk, network interfaces
- **Measurement**: Time-averaged over sampling interval

| Resource | Healthy | Warning | Critical |
|----------|---------|---------|----------|
| CPU | < 60% | 60-80% | > 80% |
| Memory | < 70% | 70-85% | > 85% |
| Disk I/O | < 50% | 50-75% | > 75% |
| Network | < 40% | 40-70% | > 70% |

### Saturation

- **Definition**: Degree to which a resource has extra work queued
- **Indicators**: Queue depth, wait time, backlog size
- **Measurement**: Queue length at sampling point or time-in-queue

| Resource | Saturation Indicator | Healthy |
|----------|---------------------|---------|
| CPU | Run queue length | < 2x CPU cores |
| Memory | Swap usage | 0 bytes |
| Disk | I/O wait queue | < 4 per disk |
| Network | TCP retransmits | < 0.1% |
| DB connections | Pool wait time | < 10ms |

### Errors

- **Definition**: Count of error events for each resource
- **Types**: Hardware errors, software errors, timeout errors
- **Measurement**: Error counters from system metrics

| Resource | Error Type | Impact |
|----------|-----------|--------|
| CPU | Machine check exceptions | System instability |
| Memory | ECC corrections, OOM kills | Data corruption, crashes |
| Disk | Read/write errors, SMART warnings | Data loss risk |
| Network | CRC errors, dropped packets | Connection failures |

## Latency Analysis

### Latency Components

```
Total Latency = Network RTT + Queue Wait + Processing + I/O Wait + Serialization
```

| Component | Typical Range | Optimization Target |
|-----------|-------------|-------------------|
| Network RTT | 1-100ms | CDN, edge caching |
| Queue wait | 0-50ms | Scaling, load balancing |
| Processing | 1-100ms | Code optimization, caching |
| I/O wait | 1-500ms | Connection pooling, async I/O |
| Serialization | 0.1-10ms | Efficient formats, streaming |

### Latency Distribution Analysis

- **Normal distribution**: Consistent performance; optimize mean
- **Bimodal distribution**: Cache hit/miss pattern; improve cache hit rate
- **Long tail**: Outlier requests; investigate p99-p100 gap
- **Increasing trend**: Resource exhaustion; scale or optimize

## Throughput Metrics

### Requests Per Second (RPS)

- **Sustained RPS**: Average over 5-minute window
- **Peak RPS**: Maximum 1-second burst observed
- **Capacity RPS**: Maximum sustainable RPS before degradation

### Throughput vs Latency Tradeoff

| Load Level | Throughput | Latency | Action |
|-----------|-----------|---------|--------|
| < 50% capacity | Linear with load | Flat | Normal operation |
| 50-80% capacity | Sublinear growth | Gradual increase | Monitor closely |
| 80-100% capacity | Plateau | Exponential increase | Scale out |
| > 100% capacity | Declining | Unbounded | Shed load |

## Composite Metrics

### Apdex Score

Application Performance Index: single score from 0 to 1.

```
Apdex = (Satisfied + (Tolerating / 2)) / Total
```

| Classification | Threshold | Example (T=500ms) |
|---------------|-----------|-------------------|
| Satisfied | < T | < 500ms |
| Tolerating | T to 4T | 500ms - 2000ms |
| Frustrated | > 4T | > 2000ms |

### Goodput

- **Definition**: Rate of useful, non-error, non-retry requests
- **Formula**: `goodput = total_rps - error_rps - retry_rps`
- **Purpose**: Measures actual user-visible throughput
- **Target**: Goodput should scale linearly with load until capacity

## Dashboard Layout

### Recommended Panels (Top to Bottom)

1. **Overview**: Request rate, error rate, p50/p95/p99 latency (golden signals)
2. **Resource health**: CPU, memory, disk, network utilization (USE method)
3. **Application**: Thread pool, connection pool, GC metrics
4. **Database**: Query latency, connection count, slow queries
5. **Dependencies**: External service latency, circuit breaker state
6. **Business**: Transaction success rate, conversion rate, revenue impact
