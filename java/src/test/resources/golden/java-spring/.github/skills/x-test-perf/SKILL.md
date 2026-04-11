---
name: x-test-perf
description: >
  Skill: Performance/Load Tests — Runs performance tests to validate latency
  SLAs, throughput targets, and resource stability under load. Supports
  baseline, normal, peak, and sustained scenarios with p50/p95/p99 latency
  metrics.
---

# Skill: Performance / Load Tests

## Description

Runs or implements performance tests to validate the application meets latency SLAs, throughput targets, and resource stability requirements under various load conditions. Tests measure p50/p95/p99 latency, transactions per second (TPS), error rates, and memory stability.

**Condition**: This skill applies to projects with performance testing requirements.

## Prerequisites

- Performance test framework configured (e.g., Gatling, k6, JMeter, Locust)
- Application deployed and accessible (local or remote)
- Test data feeders/generators available
- Sufficient system resources for load generation

## Knowledge Pack References

Before running performance tests, read:
- `.github/skills/testing/SKILL.md` — test categories, data uniqueness
- `.github/skills/resilience/SKILL.md` — timeout, circuit breaker, resource limits

## Test Scenarios

| Scenario | Description | Key Metrics |
|----------|-------------|-------------|
| Baseline | Single user, sequential requests | p99 latency, correctness |
| Normal | Expected daily load | p95 latency, TPS, error rate |
| Peak | Maximum expected concurrent load | p99 latency, error rate < 0.5% |
| Sustained | Constant load over 30+ minutes | Memory stability, latency drift |

## Execution Flow

1. **Verify prerequisites** — Framework installed, app healthy, data generators available
2. **Select and run scenario** — Baseline, Normal, Peak, or Sustained
3. **Collect metrics** — Latency percentiles, throughput, error rate, resource usage
4. **Generate report** — Summary table with metrics vs SLA targets, PASS/FAIL per criterion

## SLA Targets (Defaults)

| Metric | Baseline | Normal | Peak | Sustained |
|--------|----------|--------|------|-----------|
| p50 latency | < 10ms | < 20ms | < 50ms | < 25ms |
| p95 latency | < 50ms | < 150ms | < 300ms | < 150ms |
| p99 latency | < 100ms | < 300ms | < 500ms | < 200ms |
| Error rate | 0% | < 0.1% | < 0.5% | < 0.1% |
| Memory growth | N/A | N/A | N/A | < 10% |

## Data Generation

- Varied request payloads to avoid cache effects
- Unique identifiers per request to prevent conflicts
- Realistic distribution of operation types (e.g., 70% reads, 30% writes)
- Randomized think time between requests for realistic pacing

## Review Checklist

- [ ] Baseline scenario establishes latency floor
- [ ] Normal load matches expected daily traffic pattern
- [ ] Peak load tests maximum concurrent capacity
- [ ] Sustained load runs 30+ minutes for stability validation
- [ ] Latency assertions defined per scenario (p50, p95, p99)
- [ ] Error rate thresholds enforced
- [ ] Memory growth monitored during sustained test
- [ ] Test data is varied and realistic

## Regression Detection

Compare current test results against a stored baseline to detect performance regressions. Use `--compare-baseline` to load `results/performance/baseline.json` and compare each metric.

| Metric | Regression Condition | Severity |
|--------|---------------------|----------|
| p99 latency | Current > baseline * 1.2 (20% degradation) | FAIL |
| p95 latency | Current > baseline * 1.15 (15% degradation) | WARN |
| throughput (RPS) | Current < baseline * 0.85 (15% drop) | FAIL |
| error rate | Current > baseline + 0.5% (absolute) | FAIL |

## Baseline Management

Save and compare performance baselines:

- `--save-baseline` — Execute scenario and save results to `results/performance/baseline.json`
- `--compare-baseline` — Execute scenario and compare against stored baseline

### baseline.json Structure

```json
{
  "version": "1.0",
  "timestamp": "ISO-8601",
  "metrics": {
    "endpoint_or_operation": {
      "p50_ms": 10,
      "p95_ms": 45,
      "p99_ms": 95,
      "throughput_rps": 1200,
      "error_rate_pct": 0.1
    }
  }
}
```

## Threshold Validation

Enforces hard limits on performance metrics. When any threshold is exceeded, the test reports FAIL.

- p99 exceeding scenario SLA or +20% vs baseline triggers FAIL
- throughput dropping below baseline by 15% triggers FAIL
- Error rate exceeding baseline by 0.5% absolute triggers FAIL

## Comparison Report

When `--compare-baseline` runs, generates a structured report with delta percentages per endpoint/operation showing PASS/FAIL/WARN status for each metric.

When regressions are detected, recommends using `/x-perf-profile` to investigate the affected endpoints.

## Detailed References

For in-depth guidance on performance testing, consult:
- `.github/skills/x-test-perf/SKILL.md`
- `.github/skills/testing/SKILL.md`
- `.github/skills/performance-engineering/SKILL.md`
