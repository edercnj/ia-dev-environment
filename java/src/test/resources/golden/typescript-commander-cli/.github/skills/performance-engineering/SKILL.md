---
name: performance-engineering
description: >
  Knowledge Pack: Performance Engineering -- Profiling, benchmarking, optimization,
  regression detection, and memory management for ia-dev-environment.
---

# Knowledge Pack: Performance Engineering

## Summary

Performance engineering patterns for ia-dev-environment using typescript 5 with commander.

### Profiling Tools & Patterns

- JFR (Java Flight Recorder): continuous, low-overhead (< 2%) production profiling
- async-profiler (JVM): CPU sampling, allocation profiling, lock contention
- pprof (Go): CPU, memory, goroutine, block profiling via HTTP endpoints
- perf (Linux): hardware counters, cache misses, branch mispredictions
- py-spy (Python): sampling profiler, no code modification, production-safe
- Flamegraph interpretation: wide bars = hot paths, tall stacks = deep calls

### Benchmarking Frameworks

- JMH (Java): throughput/avg time modes, warmup iterations, fork config
- criterion (Rust): statistical benchmarking, automatic regression detection
- hyperfine (CLI): command-line benchmarking, parameter sweeps, A/B testing
- k6/Gatling/Locust: load testing with VUs, scenarios, thresholds

### Performance Anti-Patterns

- N+1 queries: eager loading, batch loading, query count assertions
- Unbounded collections: pagination enforcement, streaming with backpressure
- Synchronous I/O in hot paths: async alternatives, virtual threads
- Excessive object allocation: pooling, value types, flyweight pattern
- String concatenation in loops: StringBuilder, StringJoiner
- Missing connection pooling: pool sizing formula, leak detection

### Optimization Strategies

- Hot path optimization: identify via profiling, minimize allocations
- Lazy initialization: deferred computation, on-demand loading
- Caching: L1 (in-process), L2 (distributed), L3 (CDN), invalidation patterns
- Connection pooling: HikariCP, pgbouncer, sizing = (2 * cores) + 1
- Batch processing: chunk-based, bulk operations, write-behind
- Async I/O: reactive streams, virtual threads, goroutines, async/await
- Pagination: cursor-based > offset-based for large datasets

### Load Testing Patterns

- Ramp-up: linear, stepped, spike, wave pattern strategies
- Steady-state: minimum 10 minutes for stable percentiles
- Percentile-based SLOs: p50 < 100ms, p95 < 500ms, p99 < 1000ms
- Saturation testing: find breaking point, identify bottleneck
- Soak testing: 8-24 hours, memory leak detection, resource drift

### Performance Regression Detection

- Baseline: golden run metrics, minimum 10 runs for reliability
- Thresholds: percentage-based (p99 < baseline * 1.1), absolute (p99 < 200ms)
- CI integration: store results, trend analysis, automated comparison
- Alerting: pipeline failure on regression, dashboard annotations

### Memory Management

- Leak detection: heap dumps, allocation tracking, reference chain analysis
- GC tuning (JVM): G1GC (general), ZGC (< 1ms pauses), Shenandoah (large heaps)
- Memory profiling: RSS, virtual memory, off-heap tracking
- Allocation rate: young gen churn, promotion rate, tenuring threshold

## References

- `references/profiling-tools-matrix.md` -- Language x profiling tool x type x overhead matrix
- `references/load-testing-patterns.md` -- Load testing patterns with k6/Gatling/Locust configs
- `references/performance-metrics-guide.md` -- RED and USE methodology metrics guide
