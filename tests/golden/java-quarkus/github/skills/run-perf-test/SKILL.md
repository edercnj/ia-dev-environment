---
name: run-perf-test
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
- `.claude/skills/testing/references/testing-philosophy.md` — test categories, data uniqueness
- `.claude/skills/resilience/references/resilience-principles.md` — timeout, circuit breaker, resource limits

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

## Detailed References

For in-depth guidance on performance testing, consult:
- `.claude/skills/run-perf-test/SKILL.md`
- `.claude/skills/testing/SKILL.md`
