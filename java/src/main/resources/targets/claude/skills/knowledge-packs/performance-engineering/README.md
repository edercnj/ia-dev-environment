# performance-engineering

> Performance engineering patterns: profiling, benchmarking, optimization, regression detection, and memory management for projects using various languages and build tools.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-review` (Performance specialist), `x-perf-profile`, `performance-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Profiling tools (JFR, async-profiler, pprof, perf, py-spy) and flamegraph interpretation
- Benchmarking frameworks (JMH, criterion, hyperfine, k6/Gatling/Locust)
- Performance anti-patterns (N+1 queries, unbounded collections, missing connection pooling)
- Optimization strategies (hot path, lazy init, caching, batch processing, async I/O, pagination)
- Load testing patterns (ramp-up, steady-state, saturation, soak testing)
- Performance regression detection (baseline, thresholds, automated comparison)
- Memory management (leak detection, GC tuning, allocation rate monitoring)

## Key Concepts

This pack covers the full performance engineering lifecycle from profiling and measurement through optimization and regression prevention. It provides a multi-language profiling tools matrix, benchmarking framework guidance with statistical reliability requirements, and six categories of performance anti-patterns with detection and fix strategies. Load testing patterns include percentile-based SLOs, saturation testing for capacity planning, and soak testing for leak detection. The regression detection section covers automated baseline comparison with CI pipeline integration.

## See Also

- [observability](../observability/) — Metrics collection, SLO/SLI framework, and burn rate alerts
- [sre-practices](../sre-practices/) — Capacity planning, load testing methodology, and deployment velocity metrics
- [resilience](../resilience/) — Timeout patterns, backpressure, and bulkhead isolation for performance stability
