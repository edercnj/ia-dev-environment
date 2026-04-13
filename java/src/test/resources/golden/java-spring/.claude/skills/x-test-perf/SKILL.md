---
name: x-test-perf
description: "Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[scenario: baseline|normal|peak|sustained|all] [--save-baseline] [--compare-baseline]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Performance / Load Tests

## Purpose

Run or implement performance tests to validate the application meets latency SLAs, throughput targets, and resource stability requirements under various load conditions. Measure p50/p95/p99 latency, transactions per second (TPS), error rates, and memory stability.

## Activation Condition

Include this skill for projects with performance testing requirements.

## Triggers

- `/x-test-perf baseline` -- single user, sequential requests
- `/x-test-perf normal` -- expected daily load
- `/x-test-perf peak` -- maximum expected concurrent load
- `/x-test-perf sustained` -- constant load over extended period (30 min+)
- `/x-test-perf all` -- run all scenarios
- `/x-test-perf normal --save-baseline` -- save results as baseline
- `/x-test-perf normal --compare-baseline` -- compare against stored baseline

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `scenario` | String | No | `all` | Scenario: `baseline`, `normal`, `peak`, `sustained`, `all` |
| `--save-baseline` | Flag | No | false | Save results to `results/performance/baseline.json` |
| `--compare-baseline` | Flag | No | false | Compare results against stored baseline |

## Prerequisites

- Performance test framework configured (e.g., Gatling, k6, JMeter, Locust)
- Application deployed and accessible (local or remote)
- Test data feeders/generators available
- Sufficient system resources for load generation

## Workflow

### Step 1 — Verify Prerequisites

- Performance test framework installed and configured
- Application running and healthy (check health endpoint)
- Test data generators available

### Step 2 — Select and Run Scenario

**Baseline** (single user warmup):
- 1 connection/user, 100 sequential requests
- Validates correctness and establishes latency baseline
- Target: p99 < 100ms, 0% errors

**Normal Load** (daily traffic):
- Ramp up to expected concurrent users over 30s
- Sustain for 2-5 minutes
- Target: p95 < 150ms, TPS > configured threshold, errors < 0.1%

**Peak Load** (traffic spike):
- Rapid ramp to maximum concurrent users
- Sustain for 1-2 minutes
- Target: p99 < 500ms, errors < 0.5%, no crashes

**Sustained Load** (stability / memory leak detection):
- Constant moderate load for 30+ minutes
- Monitor memory growth over time
- Target: p99 stable (no drift > 20%), memory growth < 10%, errors < 0.1%

### Step 3 — Collect Metrics

- Latency percentiles: p50, p75, p95, p99
- Throughput: requests/transactions per second
- Error rate: percentage of failed requests
- Resource usage: memory (RSS), CPU utilization
- Connection metrics: reuse rate, failures

### Step 4 — Regression Detection (if --compare-baseline)

Compare current test results against stored baseline:

| Metric | Regression Condition | Severity |
|--------|---------------------|----------|
| p99 latency | Current > baseline * 1.2 (20% degradation) | FAIL |
| p95 latency | Current > baseline * 1.15 (15% degradation) | WARN |
| throughput (RPS) | Current < baseline * 0.85 (15% drop) | FAIL |
| error rate | Current > baseline + 0.5% (absolute) | FAIL |

### Step 5 — Generate Report

Summary table with all metrics vs SLA targets:
- PASS/FAIL per SLA criterion
- Latency distribution chart (if framework supports)
- Recommendations for failures

## SLA Targets (Defaults)

| Metric | Baseline | Normal | Peak | Sustained |
|--------|----------|--------|------|-----------|
| p50 latency | < 10ms | < 20ms | < 50ms | < 25ms |
| p95 latency | < 50ms | < 150ms | < 300ms | < 150ms |
| p99 latency | < 100ms | < 300ms | < 500ms | < 200ms |
| Error rate | 0% | < 0.1% | < 0.5% | < 0.1% |
| Memory growth | N/A | N/A | N/A | < 10% |

## Data Generation

Performance tests require realistic data generators:
- Varied request payloads to avoid cache effects
- Unique identifiers per request to prevent conflicts
- Realistic distribution of operation types (e.g., 70% reads, 30% writes)
- Randomized think time between requests for realistic pacing

## Baseline Management

### Save Baseline

When `--save-baseline` is specified:
1. Execute the selected scenario
2. Collect all metrics
3. Write results to `results/performance/baseline.json`
4. Overwrite existing baseline (with backup to `baseline.prev.json`)

### baseline.json Structure

```json
{
  "version": "1.0",
  "timestamp": "ISO-8601",
  "scenario": "normal",
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

| Metric | Absolute Threshold | Relative Threshold (vs baseline) |
|--------|-------------------|--------------------------------|
| p99 latency | Scenario SLA (see table above) | +20% vs baseline |
| p95 latency | Scenario SLA (see table above) | +15% vs baseline |
| throughput | Scenario minimum TPS | -15% vs baseline |
| error rate | Scenario max error rate | +0.5% absolute vs baseline |

## Error Handling

| Scenario | Action |
|----------|--------|
| Performance framework not installed | Report missing tool with install instructions |
| Application health check fails | Abort with health check failure details |
| Baseline file not found for comparison | Warn and continue without regression detection |
| SLA threshold exceeded | Report FAIL with metric details and recommendations |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-perf-profile | calls | Use `/x-perf-profile cpu` for latency regressions, `memory` for throughput regressions |

## Review Checklist

- [ ] Baseline scenario establishes latency floor
- [ ] Normal load matches expected daily traffic pattern
- [ ] Peak load tests maximum concurrent capacity
- [ ] Sustained load runs 30+ minutes for stability validation
- [ ] Latency assertions defined per scenario (p50, p95, p99)
- [ ] Error rate thresholds enforced
- [ ] Memory growth monitored during sustained test
- [ ] Connections are reused (persistent, not per-request)
- [ ] Test data is varied and realistic
- [ ] Results report generated with metrics vs SLA comparison
- [ ] Rate limiting adjusted for test (not interfering with load)
- [ ] Baseline saved for regression detection (if first run)
- [ ] Regression comparison executed against stored baseline
- [ ] Threshold validation passed for p99 and throughput
