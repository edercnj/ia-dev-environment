# patterns-outbox

> Transactional Outbox Pattern: reliable event publishing with polling publisher and CDC strategies. Covers the dual-write problem, outbox table design, SELECT FOR UPDATE SKIP LOCKED, Debezium CDC, and anti-patterns for event-driven systems.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-dev-implement`, `x-review` (Database/Event specialists), `architect` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- The dual-write problem and failure scenarios
- Transactional outbox solution and table schema design
- Polling publisher with SELECT FOR UPDATE SKIP LOCKED
- CDC with Debezium (connector configuration, CDC vs polling comparison)
- Anti-patterns (direct broker publish, missing partial index, no backoff, infinite retry, no lag monitoring)

## Key Concepts

This pack addresses the fundamental consistency problem in event-driven architectures: guaranteeing atomicity between database writes and event publishing. It details two relay strategies -- polling publisher (application-level, lower complexity) and CDC with Debezium (log-tailing, sub-second latency) -- with clear decision criteria for choosing between them. The anti-patterns section covers the five most common mistakes including infinite retry loops and missing outbox lag monitoring.

## See Also

- [resilience](../resilience/) — Dead letter queues, retry with backoff, and fallback patterns
- [protocols](../protocols/) — Event-driven messaging conventions and CloudEvents envelope standard
