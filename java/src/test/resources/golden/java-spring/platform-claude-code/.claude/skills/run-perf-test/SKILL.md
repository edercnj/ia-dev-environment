---
name: run-perf-test
description: "Skill: Performance/Load Tests — Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[scenario: baseline|normal|peak|sustained|all]"
---

## Global Output Policy

- **Language**: English ONLY. (Ignore input language, always respond in English).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.
- **Preservation**: All existing technical constraints below must be followed strictly.

# Skill: Performance / Load Tests

## Description

Runs or implements performance tests to validate the application meets latency SLAs, throughput targets, and resource stability requirements under various load conditions. Tests measure p50/p95/p99 latency, transactions per second (TPS), error rates, and memory stability.

**Condition**: This skill applies to projects with performance testing requirements.

## Prerequisites

- Performance test framework configured (e.g., Gatling, k6, JMeter, Locust)
- Application deployed and accessible (local or remote)
- Test data feeders/generators available
- Sufficient system resources for load generation

## Test Scenarios

| Scenario   | Description                                  | Key Metrics                       |
| ---------- | -------------------------------------------- | --------------------------------- |
| Baseline   | Single user, sequential requests             | p99 latency, correctness          |
| Normal     | Expected daily load                          | p95 latency, TPS, error rate      |
| Peak       | Maximum expected concurrent load             | p99 latency, error rate < 0.5%    |
| Sustained  | Constant load over extended period (30 min+) | Memory stability, latency drift   |

## Execution Flow

1. **Verify prerequisites**:
   - Performance test framework installed and configured
   - Application running and healthy (check health endpoint)
   - Test data generators available

2. **Select and run scenario**:

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

3. **Collect metrics**:
   - Latency percentiles: p50, p75, p95, p99
   - Throughput: requests/transactions per second
   - Error rate: percentage of failed requests
   - Resource usage: memory (RSS), CPU utilization
   - Connection metrics: reuse rate, failures

4. **Generate report**:
   - Summary table with all metrics vs SLA targets
   - PASS/FAIL per SLA criterion
   - Latency distribution chart (if framework supports)
   - Recommendations for failures

## SLA Targets (Defaults)

| Metric        | Baseline | Normal  | Peak    | Sustained |
| ------------- | -------- | ------- | ------- | --------- |
| p50 latency   | < 10ms   | < 20ms  | < 50ms  | < 25ms    |
| p95 latency   | < 50ms   | < 150ms | < 300ms | < 150ms   |
| p99 latency   | < 100ms  | < 300ms | < 500ms | < 200ms   |
| Error rate    | 0%       | < 0.1%  | < 0.5%  | < 0.1%    |
| Memory growth | N/A      | N/A     | N/A     | < 10%     |

## Usage Examples

```
/run-perf-test baseline
/run-perf-test normal
/run-perf-test peak
/run-perf-test sustained
/run-perf-test all
```

## Data Generation

Performance tests require realistic data generators:
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
- [ ] Connections are reused (persistent, not per-request)
- [ ] Test data is varied and realistic
- [ ] Results report generated with metrics vs SLA comparison
- [ ] Rate limiting adjusted for test (not interfering with load)
- [ ] Baseline saved for regression detection (if first run)
- [ ] Regression comparison executed against stored baseline
- [ ] Threshold validation passed for p99 and throughput

## Regression Detection

Compare current test results against a stored baseline to detect performance regressions automatically. When `--compare-baseline` is specified, the skill loads `results/performance/baseline.json` and compares each metric.

### Detection Rules

| Metric | Regression Condition | Severity |
|--------|---------------------|----------|
| p99 latency | Current > baseline * 1.2 (20% degradation) | FAIL |
| p95 latency | Current > baseline * 1.15 (15% degradation) | WARN |
| throughput (RPS) | Current < baseline * 0.85 (15% drop) | FAIL |
| error rate | Current > baseline + 0.5% (absolute) | FAIL |

### Detection Flow

```
1. Execute performance tests (normal scenario)
2. Collect metrics (p50, p95, p99, throughput, error_rate)
3. Load baseline from results/performance/baseline.json
4. Compare each metric per endpoint/operation
5. Classify: PASS (within threshold) or FAIL (regression detected)
6. Generate comparison report with delta percentages
```

## Baseline Management

Manage performance baselines for regression detection.

### Save Baseline

```
/run-perf-test baseline --save-baseline
/run-perf-test normal --save-baseline
```

When `--save-baseline` is specified:

1. Execute the selected scenario
2. Collect all metrics
3. Write results to `results/performance/baseline.json`
4. Overwrite existing baseline (with backup to `baseline.prev.json`)

### Compare Against Baseline

```
/run-perf-test normal --compare-baseline
/run-perf-test peak --compare-baseline
```

When `--compare-baseline` is specified:

1. Execute the selected scenario
2. Load `results/performance/baseline.json`
3. Compare metrics per endpoint/operation
4. Report PASS/FAIL with delta percentages

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

Threshold validation enforces hard limits on performance metrics. When any threshold is exceeded, the test run reports FAIL.

### Default Thresholds

| Metric | Absolute Threshold | Relative Threshold (vs baseline) |
|--------|-------------------|--------------------------------|
| p99 latency | Scenario SLA (see table above) | +20% vs baseline |
| p95 latency | Scenario SLA (see table above) | +15% vs baseline |
| throughput | Scenario minimum TPS | -15% vs baseline |
| error rate | Scenario max error rate | +0.5% absolute vs baseline |

### Validation Logic

```
FOR each endpoint/operation in results:
  IF baseline exists AND --compare-baseline:
    IF p99_current > p99_baseline * 1.20:
      FAIL "p99 regression: {delta}% increase"
    IF throughput_current < throughput_baseline * 0.85:
      FAIL "throughput regression: {delta}% decrease"
    IF error_rate_current > error_rate_baseline + 0.5:
      FAIL "error rate regression: {delta}% increase"
  ALWAYS:
    IF p99_current > scenario_sla_p99:
      FAIL "p99 exceeds SLA: {value}ms > {sla}ms"
```

## Comparison Report

When regression detection runs, generate a structured comparison report:

### Report Format

```markdown
# Performance Comparison Report

**Date:** YYYY-MM-DD
**Scenario:** normal
**Baseline:** YYYY-MM-DD (from baseline.json)

## Summary

| Status | Count |
|--------|-------|
| PASS   | N     |
| FAIL   | N     |
| WARN   | N     |

## Results by Endpoint

| Endpoint | Metric | Baseline | Current | Delta | Status |
|----------|--------|----------|---------|-------|--------|
| /api/orders | p50_ms | 10 | 12 | +20.0% | WARN |
| /api/orders | p95_ms | 45 | 43 | -4.4% | PASS |
| /api/orders | p99_ms | 95 | 142 | +49.5% | FAIL |
| /api/orders | throughput_rps | 1200 | 1150 | -4.2% | PASS |
| /api/orders | error_rate_pct | 0.1 | 0.1 | +0.0% | PASS |

## Regressions Detected

### /api/orders -- p99 Latency

- **Baseline:** 95ms
- **Current:** 142ms
- **Delta:** +49.5%
- **Threshold:** +20%
- **Action Required:** Investigate latency increase

## Recommendations

- Profile the regressed endpoints using `/x-perf-profile cpu`
- Check recent code changes affecting the hot path
- Verify infrastructure resources (CPU, memory, network)
```

### Integration with x-perf-profile

When regressions are detected, the comparison report recommends using `/x-perf-profile` to investigate:

- `/x-perf-profile cpu` -- for latency regressions
- `/x-perf-profile memory` -- for throughput regressions with memory pressure
- `/x-perf-profile io` -- for I/O-related performance degradation
