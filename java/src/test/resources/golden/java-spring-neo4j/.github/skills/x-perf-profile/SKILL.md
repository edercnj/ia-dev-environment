---
name: x-perf-profile
description: >
  Automated profiling: detect language, select profiler, execute session,
  generate flamegraph, identify hotspots, suggest optimizations.
  Reference: `.github/skills/x-perf-profile/SKILL.md`
---

# Skill: Performance Profiling

## Purpose

Executes automated profiling sessions for {{PROJECT_NAME}}. Detects the project language/runtime, selects the appropriate profiler, configures and runs a profiling session, generates flamegraph or report output, identifies hotspots, and suggests optimizations referencing the performance-engineering knowledge pack.

## Triggers

- `/x-perf-profile cpu` -- CPU profiling with default duration
- `/x-perf-profile memory` -- Memory/heap profiling
- `/x-perf-profile io` -- I/O profiling (disk, network)
- `/x-perf-profile all` -- Combined profiling (CPU + memory + I/O)
- `/x-perf-profile cpu --duration 60s` -- CPU profiling for 60 seconds
- `/x-perf-profile cpu --output flamegraph` -- Generate flamegraph SVG
- `/x-perf-profile cpu --output report` -- Generate Markdown report
- `/x-perf-profile cpu --output raw` -- Output native profiler format

## Workflow

```
1. DETECT     -> Detect language/runtime from project files
2. SELECT     -> Select appropriate profiler for the stack
3. CONFIGURE  -> Configure profiling session parameters
4. EXECUTE    -> Execute profiler with configured parameters
5. GENERATE   -> Generate flamegraph/report from profiler output
6. IDENTIFY   -> Identify hotspots from profiling results
7. SUGGEST    -> Suggest optimizations (ref: performance-engineering KP)
```

### Step 1 -- Detect Language/Runtime

Analyze project root for build/dependency files:

| File | Language/Runtime |
|------|-----------------|
| `pom.xml` | Java (Maven) |
| `build.gradle` / `build.gradle.kts` | Java/Kotlin (Gradle) |
| `go.mod` | Go |
| `Cargo.toml` | Rust |
| `pyproject.toml` / `requirements.txt` | Python |
| `package.json` | Node.js/TypeScript |

### Step 2 -- Select Appropriate Profiler

| Language | CPU Profiler | Memory Profiler | I/O Profiler |
|----------|-------------|-----------------|--------------|
| **Java** | JFR / async-profiler | JFR heap profiling | JFR I/O events |
| **Go** | pprof (CPU) | pprof (heap) | pprof (block/goroutine) |
| **Python** | py-spy (sampling) | memray / tracemalloc | cProfile + I/O tracing |
| **Rust** | perf / flamegraph-rs | DHAT / heaptrack | perf I/O events |
| **Node.js** | clinic.js / 0x | --inspect + DevTools | clinic.js doctor |

### Step 3-4 -- Configure and Execute

Configure session parameters (type, duration, sampling rate, output format) and execute the profiler against the running application.

### Step 5 -- Generate Output

- **flamegraph**: Interactive SVG/HTML flamegraph visualization
- **report**: Structured Markdown report with hotspot table and recommendations
- **raw**: Native profiler output format (JFR, pprof, speedscope JSON)

### Step 6-7 -- Identify Hotspots and Suggest Optimizations

Analyze results for CPU hotspots (>10% total time), memory allocation hotspots, I/O blocking operations, and lock contention. Suggest optimizations referencing the performance-engineering knowledge pack.

## Error Handling

| Scenario | Action |
|----------|--------|
| Language not detected | List supported languages, ask user to specify |
| Profiler not installed | Provide installation instructions |
| Application not running | Instruct user to start application first |
| Permission denied | Suggest appropriate privileges |

## Integration Notes

- Uses `performance-engineer` agent for in-depth analysis
- References performance-engineering KP for optimization patterns
- Results can feed into `x-test-perf --save-baseline`

## Detailed References

For in-depth guidance on profiling, consult:
- `.github/skills/x-perf-profile/SKILL.md`
- `.github/skills/performance-engineering/SKILL.md`
