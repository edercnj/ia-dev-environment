# x-perf-profile

> Automated profiling: detect language, select profiler, execute session, generate flamegraph, identify hotspots, suggest optimizations.

| | |
|---|---|
| **Category** | Operations |
| **Invocation** | `/x-perf-profile [cpu\|memory\|io\|all] [--duration 30s] [--output flamegraph]` |
| **Reads** | performance-engineering |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Executes automated profiling sessions by detecting the project language and runtime, selecting the appropriate profiler (JFR, pprof, py-spy, perf, etc.), configuring and running a profiling session, and generating flamegraph or report output. Identifies performance hotspots for CPU, memory, and I/O, and suggests optimizations referencing the performance-engineering knowledge pack.

## Usage

```
/x-perf-profile cpu
/x-perf-profile memory --duration 60s
/x-perf-profile all --output report
```

## Workflow

1. **Detect** -- Detect language/runtime from project build files
2. **Select** -- Select appropriate profiler for the detected stack
3. **Configure** -- Configure profiling session parameters (type, duration, sampling rate)
4. **Execute** -- Run profiler with configured parameters
5. **Generate** -- Generate flamegraph (SVG/HTML) or Markdown report from profiler output
6. **Identify** -- Identify hotspots from profiling results (CPU, memory, I/O)
7. **Suggest** -- Suggest optimizations based on hotspot analysis

## Outputs

| Artifact | Path |
|----------|------|
| Flamegraph | `flamegraph.svg` or `flamegraph.html` |
| Profiling report | Markdown report (when `--output report`) |
| Raw profile | Native profiler format (when `--output raw`) |

## See Also

- [x-ops-troubleshoot](../x-ops-troubleshoot/) -- Diagnoses performance issues and other failures
- [x-test-run](../x-test-run/) -- Coverage and test execution with threshold validation
