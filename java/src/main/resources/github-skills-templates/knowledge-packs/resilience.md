---
name: resilience
description: >
  Knowledge Pack: Resilience -- Circuit breaker, rate limiting, bulkhead
  isolation, timeout control, retry with exponential backoff, fallback,
  graceful degradation, backpressure, and resilience metrics for {project_name}.
---

# Knowledge Pack: Resilience

## Summary

Resilience patterns for {project_name} using {language_name} {language_version} with {framework_name}.

### Circuit Breaker

- States: CLOSED (normal) → OPEN (failing) → HALF-OPEN (probing)
- Track failure rate over a sliding window
- Open circuit on threshold breach, redirect to fallback
- Half-open: allow limited probe requests to test recovery

### Retry with Backoff

- Exponential backoff with jitter to prevent thundering herd
- Max retries: 3 (configurable per operation)
- Retry only on transient errors (network, 503, timeout)
- Never retry non-idempotent operations without explicit design

### Additional Patterns

- **Bulkhead**: Limit concurrent calls per dependency, isolate failure domains
- **Timeout**: Set timeouts on every external call, propagate deadlines across boundaries
- **Rate Limiting**: Token bucket or sliding window, return HTTP 429 with `Retry-After`
- **Backpressure**: Bounded queues with rejection policy, flow control in consumers

### Chaos Engineering

Proactive resilience validation through controlled fault injection.

- **Principles**: Steady-state hypothesis, vary real-world events, run in production, automate experiments, minimize blast radius
- **Experiment Types**:
  - Network failure: packet loss, latency injection, DNS failure, partition simulation
  - Latency injection: response delay, slow connections, timeout simulation
  - Resource exhaustion: CPU stress, memory pressure, disk full, thread pool exhaustion
  - Dependency failure: downstream unavailability, degraded responses, malformed responses
- **Tools**: Chaos Monkey (Netflix), Litmus (K8s), Gremlin (SaaS), Toxiproxy (network), Chaos Mesh (K8s)
- **Game Day**: Define objectives, scope blast radius, establish communication plan, prepare rollback
- **Blast Radius Control**: Start small, expand gradually, automated kill switch, monitoring thresholds

## References

- [Release It! — Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/) -- Stability patterns: circuit breaker, bulkhead, timeout
- [Microsoft Cloud Design Patterns](https://learn.microsoft.com/en-us/azure/architecture/patterns/) -- Retry, circuit breaker, throttling, and bulkhead patterns
- [Google SRE Book — Handling Overload](https://sre.google/sre-book/handling-overload/) -- Load shedding, backpressure, and graceful degradation
- [Principles of Chaos Engineering](https://principlesofchaos.org/) -- Foundational principles for chaos engineering practice
