# Profiling Tools Matrix

## Language x Profiling Tool Matrix

Recommended profiling tools by language/runtime, type, and overhead.

| Language | Tool | Type | Overhead | Production-Safe | Notes |
|----------|------|------|----------|----------------|-------|
| Java | JFR (Java Flight Recorder) | CPU, Memory, GC, I/O | < 2% | Yes | Built-in since JDK 11; always-on capable |
| Java | async-profiler | CPU, Allocation, Lock | < 5% | Yes | Native agent; flamegraph output |
| Java | JVisualVM | CPU, Memory, Threads | 10-20% | No | GUI-based; development only |
| Java | Eclipse MAT | Heap analysis | N/A (offline) | N/A | Post-mortem heap dump analysis |
| Go | pprof | CPU, Memory, Goroutine, Block | < 5% | Yes | Built-in HTTP endpoints |
| Go | trace | Execution trace | 10-25% | No | Detailed scheduler/GC tracing |
| Go | fgprof | Wall-clock | < 5% | Yes | Combines CPU + off-CPU time |
| Python | py-spy | CPU, Wall-clock | < 5% | Yes | No code modification; attach to PID |
| Python | cProfile | CPU | 10-30% | No | Built-in; requires code instrumentation |
| Python | memory_profiler | Memory | 20-50% | No | Line-by-line memory tracking |
| Python | Scalene | CPU, Memory, GPU | < 10% | No | Statistical profiling with low overhead |
| Rust | perf | CPU, Hardware counters | < 2% | Yes | Linux kernel profiler |
| Rust | flamegraph (cargo) | CPU | < 5% | No | Cargo subcommand for flamegraphs |
| Rust | DHAT | Heap analysis | 10-20% | No | Valgrind-based heap profiler |
| TypeScript | Node.js --prof | CPU | 5-10% | No | V8 profiler output |
| TypeScript | clinic.js | CPU, I/O, Event loop | 10-20% | No | Doctor, Bubbleprof, Flame tools |
| TypeScript | 0x | CPU flamegraph | < 5% | No | Flamegraph generation for Node.js |
| Kotlin | JFR / async-profiler | Same as Java | Same as Java | Yes | JVM-based; same tooling as Java |
| C# | dotTrace | CPU, Memory | 5-15% | No | JetBrains profiler |
| C# | PerfView | CPU, GC, I/O | < 5% | Yes | ETW-based; production capable |
| C# | dotMemory | Memory | 10-20% | No | Heap analysis and leak detection |

## Profiling Type Selection Guide

| Symptom | Profiling Type | Recommended Tool |
|---------|---------------|-----------------|
| High CPU usage | CPU sampling | async-profiler, pprof, py-spy |
| Slow response times | Wall-clock profiling | async-profiler (wall), fgprof |
| Memory growth over time | Heap profiling | Eclipse MAT, pprof (heap) |
| High GC frequency | Allocation profiling | JFR, async-profiler (alloc) |
| Thread contention | Lock profiling | async-profiler (lock), pprof (block) |
| I/O bottlenecks | I/O tracing | JFR (I/O events), perf |

## Production Profiling Best Practices

1. **Always-on profiling**: Use JFR or pprof with low-overhead settings
2. **Triggered profiling**: Start recording when anomaly detected (CPU > 80%, latency spike)
3. **Sampling rate**: 100-1000 samples/second for CPU; lower for allocation
4. **Duration**: 60-300 seconds for meaningful data; longer for rare events
5. **Storage**: Rotate profiling data; keep last 24 hours of recordings
6. **Security**: Restrict profiling endpoint access; sanitize stack traces
7. **Comparison**: Always profile baseline and test scenarios identically
