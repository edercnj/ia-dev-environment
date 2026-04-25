# Load Testing Patterns

## Test Types and Configuration

### Baseline Test

Establish normal performance characteristics under expected load.

- Duration: 1 hour at expected peak load
- VU count: match expected concurrent users
- Ramp-up: 5-minute linear ramp to target load
- Metrics: record p50, p95, p99 latency and throughput

### Stress Test

Find the system breaking point by gradually increasing load.

- Start: expected peak load
- Increment: 10% increase every 5 minutes
- Stop criteria: error rate > 1% or p99 > 5 seconds
- Record: maximum sustainable RPS and VU count

### Soak Test

Detect memory leaks, resource drift, and degradation over time.

- Duration: 8-24 hours at 70-80% of maximum capacity
- Monitor: memory usage trend, GC frequency, connection count
- Alert: if any metric shows consistent upward trend
- Analysis: compare start-of-test vs end-of-test resource usage

### Spike Test

Validate auto-scaling and recovery behavior under sudden load.

- Baseline: 10% of expected peak for 5 minutes
- Spike: instant jump to 200-500% of peak for 2 minutes
- Recovery: return to baseline; measure recovery time
- Verify: auto-scaling triggers, no data loss, graceful degradation

## k6 Configuration Examples

### Basic Load Test

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 100 },   // ramp up
    { duration: '10m', target: 100 },   // steady state
    { duration: '2m', target: 0 },      // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
  },
};
```

### Spike Test

```javascript
export const options = {
  stages: [
    { duration: '1m', target: 10 },     // baseline
    { duration: '10s', target: 500 },    // spike
    { duration: '2m', target: 500 },     // hold spike
    { duration: '10s', target: 10 },     // recover
    { duration: '2m', target: 10 },      // verify recovery
  ],
};
```

### Soak Test

```javascript
export const options = {
  stages: [
    { duration: '5m', target: 200 },    // ramp up
    { duration: '8h', target: 200 },     // soak
    { duration: '5m', target: 0 },       // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(99)<1000'],
    http_req_failed: ['rate<0.001'],
  },
};
```

## Gatling Configuration Examples

### Simulation Setup

```scala
setUp(
  scn.inject(
    rampUsersPerSec(1).to(100).during(120),   // ramp up
    constantUsersPerSec(100).during(600),       // steady state
    rampUsersPerSec(100).to(0).during(120)      // ramp down
  )
).assertions(
  global.responseTime.percentile3.lt(500),      // p95 < 500ms
  global.responseTime.percentile4.lt(1000),     // p99 < 1000ms
  global.failedRequests.percent.lt(1.0)         // < 1% errors
)
```

## Locust Configuration Examples

### Load Shape

```python
class StagesShape(LoadTestShape):
    stages = [
        {"duration": 120, "users": 100, "spawn_rate": 10},
        {"duration": 720, "users": 100, "spawn_rate": 10},
        {"duration": 840, "users": 0, "spawn_rate": 10},
    ]

    def tick(self):
        run_time = self.get_run_time()
        for stage in self.stages:
            if run_time < stage["duration"]:
                return (stage["users"], stage["spawn_rate"])
        return None
```

## Test Data Strategies

| Strategy | Description | Best For |
|----------|-------------|----------|
| Static data | Pre-loaded fixed dataset | Reproducible baselines |
| Parameterized | CSV/JSON feed per VU | Realistic user behavior |
| Generated | Random data per request | Cache bypass testing |
| Production mirror | Anonymized production data | Realistic distribution |

## Result Analysis

### Key Metrics to Capture

- **Latency distribution**: p50, p90, p95, p99, max
- **Throughput**: requests per second (RPS) sustained
- **Error rate**: percentage and categorization (4xx vs 5xx)
- **Resource utilization**: CPU, memory, disk I/O, network
- **Connection metrics**: active connections, pool utilization
- **Application metrics**: GC pauses, thread count, queue depth

### Comparison Framework

1. Run baseline test (current production version)
2. Run candidate test (new version, same conditions)
3. Compare percentile distributions (not just averages)
4. Statistical significance: minimum 3 runs each for reliable comparison
5. Report: include confidence intervals for all metrics
