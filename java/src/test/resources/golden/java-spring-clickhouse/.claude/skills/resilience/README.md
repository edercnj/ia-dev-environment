# resilience

> Resilience patterns: circuit breaker, rate limiting, bulkhead isolation, timeout control, retry with exponential backoff + jitter, fallback/graceful degradation, backpressure, and resilience metrics.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-task-implement`, `x-review` (Performance specialist), `x-story-implement`, `architect` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Circuit breaker state machine (CLOSED/OPEN/HALF_OPEN)
- Rate limiting algorithms (token bucket, fixed window, sliding window)
- Bulkhead isolation (thread pool, semaphore, partitioning strategies)
- Timeout patterns (connection, read, write, deadline propagation)
- Retry with exponential backoff and jitter (full, equal, decorrelated)
- Fallback and graceful degradation levels
- Backpressure and flow control mechanisms
- Chaos engineering (experiment types, tools, game day planning, blast radius control)

## Key Concepts

This pack provides eight core resilience patterns that enable services to handle failures gracefully and prevent cascading outages. Each pattern includes configuration guidelines, monitoring metrics, and integration points with other patterns (e.g., retry interacting with circuit breaker and timeout). The chaos engineering section covers proactive resilience validation through controlled fault injection, with experiment types spanning network failure, latency injection, resource exhaustion, and dependency failure. Game day planning templates and blast radius control strategies ensure safe experimentation.

## See Also

- [observability](../observability/) — Resilience metrics dashboards and alert thresholds
- [infrastructure](../infrastructure/) — Graceful shutdown, health probes, and resource management
- [sre-practices](../sre-practices/) — Error budgets, incident management, and rollback criteria
