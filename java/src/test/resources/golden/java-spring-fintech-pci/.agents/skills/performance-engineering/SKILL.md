---
name: performance-engineering
description: "Performance engineering patterns: profiling, benchmarking, optimization, regression detection, and memory management for {{LANGUAGE}} {{FRAMEWORK}} projects using {{BUILD_TOOL}}."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Performance Engineering

## Purpose

Provides comprehensive performance engineering patterns for {{LANGUAGE}} {{FRAMEWORK}} projects using {{BUILD_TOOL}}, enabling profiling, benchmarking, optimization, regression detection, and memory management. Covers profiling tools, benchmarking frameworks, performance anti-patterns, optimization strategies, load testing patterns, regression detection, and memory management.

## Quick Reference (always in context)

See `references/profiling-tools-matrix.md` for profiling tool recommendations by language/runtime, `references/load-testing-patterns.md` for load testing patterns with configuration examples, and `references/performance-metrics-guide.md` for performance metrics guide (latency, throughput, saturation).

## Detailed References

Read these files for comprehensive performance engineering guidance:

| Reference | Content |
|-----------|---------|
| `references/profiling-tools-matrix.md` | Language x profiling tool x type (CPU/Memory/IO) x overhead matrix with recommended tools per runtime |
| `references/load-testing-patterns.md` | Load testing patterns with configuration examples for k6, Gatling, and Locust including ramp-up, soak, and spike scenarios |
| `references/performance-metrics-guide.md` | Performance metrics guide covering RED (Rate, Errors, Duration) and USE (Utilization, Saturation, Errors) methodologies |

## Profiling Tools & Patterns

### JFR (Java Flight Recorder)

- Continuous profiling with low overhead (< 2% in production)
- Event-based recording: CPU, memory allocation, GC, I/O, thread contention
- Production-safe: always-on profiling with configurable event thresholds
- JDK Mission Control for analysis and visualization

### async-profiler (JVM)

- CPU sampling: identify hot methods and call paths
- Allocation profiling: track object allocation sites and rates
- Wall-clock profiling: includes blocked and waiting time
- Lock contention profiling: detect thread synchronization bottlenecks
- Native frame support: profiles JNI and native code alongside Java

### pprof (Go)

- CPU profiling: goroutine scheduling, system calls, GC overhead
- Memory profiling: heap allocations, in-use objects, allocation sites
- Goroutine profiling: goroutine counts, stack traces, leak detection
- Block profiling: channel operations, mutex contention
- HTTP endpoints: `/debug/pprof/` for production profiling

### perf (Linux)

- Hardware counters: cache misses, branch mispredictions, IPC
- Kernel tracing: system calls, scheduler events, I/O latency
- Software events: page faults, context switches, CPU migrations
- Flamegraph generation from perf data for visualization

### py-spy (Python)

- Sampling profiler: no code modification required
- Production-safe: attaches to running process without restart
- Wall-clock and CPU time modes
- GIL analysis: detect Global Interpreter Lock contention
- Flamegraph and speedscope output formats

### Flamegraph Interpretation

- **Wide bars**: High cumulative time (hot paths) — optimize first
- **Tall stacks**: Deep call chains — consider flattening
- **Plateaus**: Uniform time distribution — indicates balanced load
- **Spikes**: Disproportionate time in single function — optimization target
- Compare flamegraphs before/after optimization for validation

## Benchmarking Frameworks

### JMH (Java Microbenchmark Harness)

- Benchmark modes: throughput, average time, sample time, single shot
- Warmup iterations: minimum 5 warmup, 10 measurement iterations
- Fork configuration: minimum 2 forks for statistical reliability
- State management: `@State` scope (Thread, Benchmark, Group)
- Blackhole: prevent dead code elimination with `Blackhole.consume()`

### criterion (Rust)

- Statistical benchmarking with confidence intervals
- Automatic regression detection between runs
- HTML report generation with charts and analysis
- Parameterized benchmarks for input size sweeps
- Integration with cargo bench workflow

### hyperfine (CLI)

- Command-line benchmarking with warmup support
- Statistical analysis: mean, min, max, standard deviation
- Parameter sweeps: `--parameter-scan` for input range testing
- Export formats: JSON, CSV, Markdown for CI integration
- Shell command comparison: A/B testing of implementations

### k6 / Gatling / Locust (Load Testing)

- Virtual users (VUs): simulate concurrent user sessions
- Scenarios: open model (arrival rate) vs closed model (concurrent VUs)
- Thresholds: p95 latency, error rate, throughput gates
- Ramping stages: gradual load increase for realistic simulation
- Real-time metrics: request rate, response time, error rate

## Performance Anti-Patterns

### N+1 Queries

- **Detection**: Enable query logging; count queries per request
- **Fix**: Eager loading (JOIN FETCH), batch loading (`IN` clause)
- **Prevention**: Query count assertions in integration tests

### Unbounded Collections

- **Detection**: Monitor response payload sizes and memory allocation
- **Fix**: Pagination enforcement on all collection endpoints
- **Alternative**: Streaming with backpressure for large datasets

### Synchronous I/O in Hot Paths

- **Detection**: Thread dump analysis showing blocked threads on I/O
- **Fix**: Async alternatives, non-blocking I/O, virtual threads (Java 21)
- **Prevention**: Identify hot paths via profiling before optimization

### Excessive Object Allocation

- **Detection**: Allocation profiling showing high young gen churn
- **Fix**: Object pooling, value types, flyweight pattern
- **Prevention**: Reuse builders, use primitive collections

### String Concatenation in Loops

- **Detection**: Allocation profiler showing String/char[] hotspots
- **Fix**: StringBuilder, StringJoiner, template engines
- **Prevention**: Static analysis rules flagging concatenation in loops

### Missing Connection Pooling

- **Detection**: High connection creation time in traces
- **Fix**: Connection pool (HikariCP, pgbouncer) with proper sizing
- **Sizing formula**: `pool_size = (core_count * 2) + effective_spindle_count`
- **Monitoring**: Pool utilization, wait time, leak detection

## Optimization Strategies

### Hot Path Optimization

- Identify via profiling (top 10% of execution time)
- Minimize allocations in hot paths (reuse objects, avoid boxing)
- Reduce branching: use lookup tables, polymorphism over conditionals
- Inline small methods in critical paths (JIT-friendly patterns)

### Lazy Initialization

- Deferred computation: compute only when first accessed
- Lazy collections: defer loading until iteration
- On-demand loading: load resources only when needed
- Thread-safe lazy init: double-checked locking or holder pattern

### Caching Strategies

| Level | Scope | Latency | Invalidation |
|-------|-------|---------|-------------|
| L1 (in-process) | Single instance | Nanoseconds | TTL, size-based eviction |
| L2 (distributed) | Cluster-wide | Milliseconds | TTL, event-based |
| L3 (CDN) | Edge locations | Variable | TTL, purge API |

- Cache invalidation patterns: write-through, write-behind, cache-aside
- Key design: deterministic, collision-resistant, include version
- Eviction policies: LRU, LFU, TTL-based expiry

### Connection Pooling

- HikariCP (Java): `minimumIdle = maximumPoolSize` for fixed pool
- pgbouncer (PostgreSQL): transaction pooling for high concurrency
- Sizing: start with `(2 * CPU cores) + 1`, tune based on monitoring
- Idle timeout: close idle connections after 10 minutes
- Leak detection: enable with 30-second threshold

### Batch Processing

- Chunk-based processing: process N items per batch (typical: 100-1000)
- Bulk operations: batch INSERT/UPDATE instead of row-by-row
- Write-behind patterns: accumulate writes and flush periodically
- Parallel batch execution with controlled concurrency

### Async I/O

- Reactive streams: backpressure-aware processing
- Virtual threads (Java 21): lightweight threads for I/O-bound work
- Goroutines (Go): multiplexed onto OS threads by runtime scheduler
- async/await: cooperative multitasking for I/O operations

### Pagination

- Cursor-based: deterministic ordering, no count needed, scales well
- Offset-based: simple but slow for large offsets (O(n) skip)
- Keyset pagination: WHERE clause on last seen key, index-friendly
- Avoid total count queries on large tables (use estimates)

## Load Testing Patterns

### Ramp-Up Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| Linear ramp | Steady increase over time | Capacity planning |
| Stepped ramp | Discrete load levels held for duration | Finding thresholds |
| Spike test | Sudden burst of traffic | Auto-scaling validation |
| Wave pattern | Oscillating load levels | Sustained load behavior |

### Steady-State Duration

- Minimum 10 minutes for stable percentile calculation
- 30 minutes recommended for production-like validation
- Monitor for drift: increasing latency or error rates over time
- Ensure test data variety to avoid cache warming bias

### Percentile-Based SLOs

| Metric | Target | Measurement |
|--------|--------|-------------|
| p50 latency | < 100ms | Median response time |
| p95 latency | < 500ms | Tail latency baseline |
| p99 latency | < 1000ms | Worst-case user experience |
| Error rate | < 0.1% | Percentage of failed requests |
| Throughput | > baseline | Requests per second sustained |

### Saturation Testing

- Gradually increase load until error rate exceeds threshold
- Record breaking point: VU count, RPS, resource utilization
- Identify bottleneck: CPU, memory, I/O, network, database
- Establish maximum capacity for capacity planning

### Soak Testing

- Extended duration: 8-24 hours at expected peak load
- Memory leak detection: heap usage trend over time
- Resource drift: file handles, connections, thread count
- GC pressure: increasing GC frequency or pause time

## Performance Regression Detection

### Baseline Establishment

- Golden run: record metrics under controlled conditions
- Statistical significance: minimum 10 runs for reliable baseline
- Environment parity: same hardware, OS, JVM flags, data volume
- Version pin: lock all dependencies during baseline

### Threshold Definition

| Approach | Example | Use Case |
|----------|---------|----------|
| Percentage-based | p99 < baseline * 1.1 | General regression |
| Absolute | p99 < 200ms | SLO compliance |
| Statistical | Mean outside 2 sigma | Noise-tolerant detection |

### Automated Comparison

- Store benchmark results in CI artifacts or dedicated database
- Trend analysis: plot metrics over last N builds
- Automated diff: compare current vs baseline with threshold
- Blame detection: identify commit range causing regression

### Alerting

- CI pipeline failure on regression detection
- Dashboard integration: Grafana annotations for deployments
- Notification: Slack/email alert with regression details
- Auto-bisect: identify exact commit causing regression

## Memory Management

### Leak Detection

- Heap dump analysis: identify objects preventing garbage collection
- Allocation tracking: monitor allocation rate and promotion rate
- Reference chain analysis: find GC roots holding leaked objects
- Tools: Eclipse MAT, VisualVM, jmap (JVM); valgrind (native)

### GC Tuning (JVM)

| Collector | Pause Target | Throughput | Use Case |
|-----------|-------------|-----------|----------|
| G1GC | < 200ms | High | General purpose (default) |
| ZGC | < 1ms | Medium | Ultra-low latency |
| Shenandoah | < 10ms | Medium | Low-latency, large heaps |

- Heap sizing: `-Xms` = `-Xmx` for predictable behavior
- Young gen ratio: default 1/3 of heap, tune based on allocation rate
- GC logging: always enable in production (`-Xlog:gc*`)
- Avoid explicit `System.gc()` calls

### Memory Profiling

- Resident Set Size (RSS): actual physical memory used
- Virtual memory: address space reserved (not always committed)
- Off-heap tracking: direct buffers, memory-mapped files, JNI
- Native memory tracking: `-XX:NativeMemoryTracking=summary`

### Allocation Rate Monitoring

- Young gen churn: high allocation rate causes frequent minor GC
- Promotion rate: objects surviving to old gen; high rate causes major GC
- Tenuring threshold: adjust `-XX:MaxTenuringThreshold` based on object lifetimes
- Allocation flamegraph: visualize allocation sites and rates

## Related Knowledge Packs

- `skills/observability/` — metrics collection, distributed tracing, and SLO/SLI framework
- `skills/resilience/` — timeout, circuit breaker, and rate limiting patterns that affect performance
- `skills/sre-practices/` — capacity planning, load testing methodology, and error budgets
