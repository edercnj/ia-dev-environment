---
name: resilience
description: >
  Knowledge Pack: Resilience -- Circuit breaker, rate limiting, bulkhead
  isolation, timeout control, retry with exponential backoff, fallback,
  graceful degradation, backpressure, and resilience metrics for my-spring-service.
---

# Knowledge Pack: Resilience

## Summary

Resilience patterns for my-spring-service using java 21 with spring-boot.

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

## References

- `.github/skills/resilience/SKILL.md` -- Full resilience reference
