---
name: resilience
description: "Resilience patterns: circuit breaker, rate limiting, bulkhead isolation, timeout control, retry with exponential backoff + jitter, fallback/graceful degradation, backpressure, and resilience metrics."
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Resilience

## Purpose

Provides comprehensive resilience patterns for {{LANGUAGE}} {{FRAMEWORK}}, enabling services to gracefully handle failures, prevent cascading outages, and recover quickly. Includes circuit breaker state machines, rate limiting strategies, bulkhead partitioning, timeout coordination, intelligent retry logic, and degradation strategies.

## Quick Reference (always in context)

See `references/resilience-principles.md` for the essential resilience summary (6 core patterns: rate limiting, circuit breaker, bulkhead, timeout, retry, fallback).

## Detailed References

Read these files for detailed pattern implementations:

| Reference | Content |
|-----------|---------|
| `patterns/resilience/circuit-breaker.md` | State machine (CLOSED/OPEN/HALF_OPEN), configuration (failure threshold, wait duration, success threshold), monitoring metrics, fallback strategies, per-dependency circuits |
| `patterns/resilience/rate-limiting.md` | Token bucket, fixed window, sliding window algorithms; per-client, per-endpoint, global scopes; token bucket properties (capacity, refill rate, burst); response to limit (429 with Retry-After) |
| `patterns/resilience/bulkhead.md` | Thread pool isolation, semaphore isolation, partitioning strategies (by downstream service, operation type, tenant, protocol); sizing guidelines; rejection handling; metrics and monitoring |
| `patterns/resilience/timeout-patterns.md` | Timeout types (connection, read, write, overall); per-operation configurations; deadline propagation across services; timeout hierarchy (inner < outer); cancellation on timeout |
| `patterns/resilience/retry-with-backoff.md` | Exponential backoff, linear backoff; mandatory jitter (full, equal, decorrelated); retryable vs non-retryable error classification; retry budgets; interaction with deadlines |
| `patterns/resilience/fallback-degradation.md` | Graceful degradation levels (NORMAL, WARNING, CRITICAL, EMERGENCY); fallback strategies (cached data, default, error); fail-secure principle; degradation triggers and transitions |
| `patterns/resilience/backpressure.md` | Flow control mechanisms, pause/resume protocols, connection-level backpressure, message queue depth limits, timeout-based resumption |
| `patterns/resilience/resilience-metrics.md` | Metric types per pattern, naming conventions, alert thresholds, dashboards, SLA tracking |
| `references/chaos-engineering-experiments.md` | Catalog of chaos experiments by type (network, latency, resource, dependency) with setup instructions |

## Chaos Engineering

Proactive resilience validation through controlled fault injection for {{LANGUAGE}} {{FRAMEWORK}}.

### Principles

- **Steady-State Hypothesis**: Define measurable baseline behavior (latency p99, error rate, throughput) before experiments
- **Vary Real-World Events**: Simulate failures that actually occur in production (network partitions, disk full, OOM)
- **Run in Production**: Validate resilience where it matters most; staging cannot replicate production complexity
- **Automate Experiments**: Repeatable, scheduled experiments integrated into CI/CD pipelines
- **Minimize Blast Radius**: Start with smallest scope (single instance), expand gradually with automated kill switches

### Experiment Types

#### Network Failure

- Packet loss injection (5%, 25%, 50%, 100%)
- Latency injection (50ms, 200ms, 1s, 5s added delay)
- DNS resolution failure (NXDOMAIN, timeout)
- Network partition simulation (split-brain between services)

#### Latency Injection

- Response delay on specific endpoints
- Slow connection establishment (TCP handshake delay)
- Timeout simulation (response arrives after client timeout)

#### Resource Exhaustion

- CPU stress (saturate cores to validate degradation behavior)
- Memory pressure (allocate until near-OOM to test GC behavior)
- Disk full simulation (verify graceful handling of write failures)
- Thread pool exhaustion (saturate worker threads)

#### Dependency Failure

- Downstream service unavailability (connection refused)
- Degraded responses (slow responses, partial data)
- Malformed responses (invalid JSON, unexpected status codes)

### Tools

| Tool | Scope | Use Case |
|------|-------|----------|
| Chaos Monkey (Netflix) | Instance | Random instance termination in production |
| Litmus | Kubernetes | K8s-native chaos experiments with CRDs |
| Gremlin | SaaS | Enterprise chaos platform with safety controls |
| Toxiproxy | Network | TCP-level proxy for latency/partition injection |
| Chaos Mesh | Kubernetes | K8s chaos with dashboard and scheduling |

### Game Day Planning

1. **Objectives**: Define what resilience property to validate (e.g., "circuit breaker opens within 5s of downstream failure")
2. **Scope**: Identify target services, blast radius, and rollback criteria
3. **Participants**: Engineers, SRE, on-call team, stakeholders
4. **Communication Plan**: Notify affected teams, establish war-room channel
5. **Rollback Procedures**: Automated kill switch, manual rollback steps, escalation path

### Blast Radius Control

- Start small: single instance or single dependency
- Expand gradually: increase scope only after successful smaller experiments
- Automated kill switch: monitoring thresholds that auto-terminate experiments
- Monitoring thresholds: abort if error rate exceeds 2x baseline or latency exceeds 3x p99

### Experiment Runbook Template

```
## Experiment: [Name]
### Hypothesis
[If X failure occurs, then Y behavior is expected because Z resilience pattern is in place]
### Steady-State Metrics
- Latency p99: [baseline]
- Error rate: [baseline]
- Throughput: [baseline]
### Experiment Steps
1. [Step with specific tool/command]
2. [Observation window]
3. [Rollback trigger conditions]
### Expected vs Actual Results
| Metric | Expected | Actual | Pass/Fail |
|--------|----------|--------|-----------|
### Findings
[Document unexpected behaviors]
### Action Items
- [ ] [Fix/improvement with owner and deadline]
```
