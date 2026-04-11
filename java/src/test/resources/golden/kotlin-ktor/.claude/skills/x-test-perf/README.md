# x-test-perf

> Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | Performance testing requirements configured |
| **Invocation** | `/x-test-perf [scenario: baseline\|normal\|peak\|sustained\|all]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when performance testing requirements are defined in the project configuration.

## What It Does

Runs or implements performance tests to validate the application meets latency SLAs, throughput targets, and resource stability requirements under various load conditions. Measures p50/p95/p99 latency, transactions per second (TPS), error rates, and memory stability across four scenarios: baseline (single user), normal (expected daily load), peak (maximum concurrent load), and sustained (constant load over 30+ minutes for stability).

## Usage

```
/x-test-perf
/x-test-perf baseline
/x-test-perf peak
/x-test-perf all
```

## See Also

- [x-test-e2e](../x-test-e2e/) -- End-to-end integration tests
- [x-test-smoke-api](../x-test-smoke-api/) -- REST API smoke tests
- [x-obs-instrument](../x-obs-instrument/) -- OpenTelemetry instrumentation for performance metrics
