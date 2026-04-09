---
name: x-perf-profile
description: "Automated profiling: detect language/runtime, select appropriate profiler, execute session, generate flamegraph, identify hotspots, and suggest optimizations referencing the performance-engineering knowledge pack."
user-invocable: true
allowed-tools: Read, Bash, Glob, Grep, Agent
argument-hint: "[cpu|memory|io|all] [--duration 30s] [--output flamegraph|report|raw]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Performance Profiling

## Purpose

Execute automated profiling sessions for {{PROJECT_NAME}}. Detect the project language/runtime, select the appropriate profiler, configure and run a profiling session, generate flamegraph or report output, identify hotspots, and suggest optimizations referencing the performance-engineering knowledge pack.

## Triggers

- `/x-perf-profile cpu` — CPU profiling with default duration
- `/x-perf-profile memory` — memory/heap profiling
- `/x-perf-profile io` — I/O profiling (disk, network)
- `/x-perf-profile all` — combined profiling (CPU + memory + I/O)
- `/x-perf-profile cpu --duration 60s` — CPU profiling for 60 seconds
- `/x-perf-profile cpu --output flamegraph` — generate flamegraph SVG
- `/x-perf-profile cpu --output report` — generate Markdown report
- `/x-perf-profile cpu --output raw` — output native profiler format

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `type` | Enum | No | `cpu` | Profiling type: `cpu`, `memory`, `io`, `all` |
| `--duration` | Duration | No | `30s` | Recording duration |
| `--output` | Enum | No | `flamegraph` | Output format: `flamegraph`, `report`, `raw` |

## Workflow

### Step 1 — Detect Language/Runtime

Analyze project root for build/dependency files to identify the technology stack:

| File | Language/Runtime | Detection Priority |
|------|-----------------|-------------------|
| `pom.xml` | Java (Maven) | 1 |
| `build.gradle` / `build.gradle.kts` | Java/Kotlin (Gradle) | 1 |
| `go.mod` | Go | 1 |
| `Cargo.toml` | Rust | 1 |
| `pyproject.toml` / `requirements.txt` | Python | 1 |
| `package.json` | Node.js/TypeScript | 1 |
| `mix.exs` | Elixir | 2 |
| `Gemfile` | Ruby | 2 |

```bash
# Auto-detect language from project root
ls -la pom.xml build.gradle* go.mod Cargo.toml pyproject.toml package.json 2>/dev/null
```

### Step 2 — Select Appropriate Profiler

Based on detected language, select the best profiler:

| Language | CPU Profiler | Memory Profiler | I/O Profiler |
|----------|-------------|-----------------|--------------|
| **Java** | JFR (Java Flight Recorder) / async-profiler | JFR heap profiling | JFR I/O events |
| **Go** | pprof (CPU profile) | pprof (heap profile) | pprof (block/goroutine) |
| **Python** | py-spy (sampling) | memray / tracemalloc | cProfile + I/O tracing |
| **Rust** | perf / flamegraph-rs | DHAT / heaptrack | perf I/O events |
| **Node.js** | clinic.js / 0x | --inspect + Chrome DevTools | clinic.js doctor |
| **Other** | perf (Linux) / dtrace (macOS) | Valgrind massif | strace/perf |

#### 2.1 — Java JFR Configuration

```bash
# Start JFR recording
java -XX:StartFlightRecording=duration=30s,filename=profile.jfr -jar app.jar

# Using async-profiler for flamegraph
./asprof -d 30 -f flamegraph.html <pid>

# Analyze JFR recording
jfr print --events jdk.CPULoad,jdk.ThreadAllocationStatistics profile.jfr
```

#### 2.2 — Go pprof Configuration

```bash
# CPU profiling (requires net/http/pprof import)
go tool pprof http://localhost:6060/debug/pprof/profile?seconds=30

# Memory profiling
go tool pprof http://localhost:6060/debug/pprof/heap

# Generate flamegraph
go tool pprof -http=:8080 profile.pb.gz
```

#### 2.3 — Python py-spy Configuration

```bash
# CPU profiling with py-spy
py-spy record -o flamegraph.svg --pid <pid> --duration 30

# Memory profiling with memray
memray run --output profile.bin script.py
memray flamegraph profile.bin -o flamegraph.html

# Top-like real-time profiling
py-spy top --pid <pid>
```

### Step 3 — Configure Profiling Session

Configure session parameters based on arguments:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `type` | `cpu` | Profiling type: cpu, memory, io, all |
| `duration` | `30s` | Recording duration |
| `sampling-rate` | Profiler default | Sampling frequency (Hz) |
| `output` | `flamegraph` | Output format: flamegraph, report, raw |

**Session configuration checklist:**

- [ ] Profiler installed and accessible
- [ ] Target application running and healthy
- [ ] Sufficient disk space for profiling data
- [ ] JVM flags configured (for Java: `-XX:+UnlockDiagnosticVMOptions`)
- [ ] Profiling port accessible (for remote profiling)

### Step 4 — Execute Profiling

Run the selected profiler with configured parameters. Monitor for:

- Profiler overhead (should be < 5% for sampling profilers)
- Disk space consumption
- Application stability during profiling
- Recording completion without errors

### Step 5 — Generate Flamegraph/Report

#### 5.1 — Flamegraph Output (SVG/HTML)

Generate interactive flamegraph from profiler output:

- **Java JFR**: Use `jfr` tool or async-profiler to convert to flamegraph
- **Go pprof**: Use `go tool pprof -http=:8080` for web-based flamegraph
- **Python py-spy**: Direct SVG output with `--format flamegraph`
- **Generic**: Use Brendan Gregg's FlameGraph scripts

#### 5.2 — Markdown Report Output

Generate structured report with:

```markdown
# Profiling Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Duration:** Ns
**Type:** cpu|memory|io

## Top Hotspots

| Rank | Function | Self Time | Total Time | % |
|------|----------|-----------|------------|---|
| 1 | com.example.Service.process() | 450ms | 1200ms | 37.5% |
| 2 | com.example.Repo.query() | 320ms | 800ms | 26.7% |

## Call Tree (Top 10)

[Hierarchical call tree with timing]

## Recommendations

[Optimization suggestions based on hotspots]
```

#### 5.3 — Raw Output

Output the native profiler format without transformation:
- JFR: `.jfr` file
- pprof: `.pb.gz` file
- py-spy: speedscope JSON

### Step 6 — Identify Hotspots

Analyze profiling results to identify performance hotspots:

**CPU Hotspots:**
- Functions consuming > 10% of total CPU time
- Recursive call patterns with deep stack depth
- Lock contention points (high wait time)

**Memory Hotspots:**
- Objects with highest allocation rate
- Memory leak candidates (growing heap over time)
- Large object allocations (> 1MB per allocation)

**I/O Hotspots:**
- Blocking I/O operations on hot paths
- Excessive network round-trips
- Unbuffered file operations

### Step 7 — Suggest Optimizations

Reference the performance-engineering knowledge pack for contextualized optimization suggestions:

| Hotspot Type | Common Optimizations |
|-------------|---------------------|
| CPU-bound loops | Algorithm optimization, caching, parallelization |
| Memory allocation | Object pooling, value types, lazy initialization |
| I/O blocking | Async I/O, connection pooling, batching |
| Lock contention | Lock-free data structures, read-write locks, partitioning |
| GC pressure | Reduce allocation rate, tune GC parameters, off-heap storage |
| Serialization | Binary formats (protobuf), zero-copy, pre-computed serialization |

## Error Handling

| Scenario | Action |
|----------|--------|
| Language not detected | List supported languages, ask user to specify |
| Profiler not installed | Provide installation instructions for detected stack |
| Application not running | Instruct user to start the application first |
| Permission denied | Suggest running with appropriate privileges (sudo/ptrace) |
| Recording failed | Check disk space, profiler compatibility, retry once |
| Empty profiling data | Verify application is under load during profiling |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| run-perf-test | complements | Performance tests generate load; profiling captures behavior |
| x-ops-troubleshoot | calls | Troubleshooting may invoke profiling for performance issues |
| x-review | called-by | Performance review specialist may reference profiling results |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| performance-engineering | Optimization patterns, profiler selection, hotspot classification |
