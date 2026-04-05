# Performance Baselines

## Measurement Guide

### Metrics

| Metric | Description | How to Measure |
| :--- | :--- | :--- |
| `latency_p50` | 50th percentile response time | Load test with representative payload; capture median from histogram |
| `latency_p95` | 95th percentile response time | Same load test; capture 95th percentile from histogram |
| `latency_p99` | 99th percentile response time | Same load test; capture 99th percentile from histogram |
| `throughput_rps` | Requests per second at stable load | Sustained load test for ≥60s; measure avg RPS after warm-up |
| `memory_mb` | RSS memory footprint in MB | Measure RSS after warm-up under steady-state load |
| `startup_ms` | Application cold-start time in ms | Time from process start to first successful health check response |

### Measurement Conditions

- **Environment:** Use a dedicated, isolated environment (not shared CI runners)
- **Load profile:** Constant rate matching expected production traffic
- **Warm-up:** Discard first 30s of measurements to exclude JIT/cache warm-up
- **Repetitions:** Run each measurement at least 3 times; report the median
- **Baseline machine:** Document CPU, memory, OS, and runtime version used for measurement
- **Concurrent connections:** Document the number of concurrent connections/threads used

### Tools by Stack

| Stack | Latency/Throughput | Memory | Startup |
| :--- | :--- | :--- | :--- |
| {{LANGUAGE}} / {{FRAMEWORK}} | `wrk`, `hey`, `k6`, `autocannon` | OS-native (`ps`, `/proc`), runtime profiler | Custom timer script |

## Baselines

| Feature/Story ID | Date | Metric | Before | After | Delta | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| _example-0001_ | _2025-01-15_ | _latency_p95_ | _45ms_ | _52ms_ | _+15.6%_ | _Added validation middleware_ |

### Delta Interpretation

| Delta Range | Severity | Action |
| :--- | :--- | :--- |
| <= +10% | Acceptable | No action needed |
| +10% to +25% | Warning | Document reason, consider optimization |
| > +25% | Investigation | Mandatory investigation and optimization plan |
