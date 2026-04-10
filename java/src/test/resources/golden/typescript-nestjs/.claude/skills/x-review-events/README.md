# x-review-events

> Event-Driven Review -- validates event schemas, producer/consumer patterns, error handling, dead letter topics, and operational readiness.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `interfaces` contains event-consumer or event-producer |
| **Invocation** | `/x-review-events [event-name or consumer/producer class]` |
| **Reads** | protocols (references: event-driven-conventions) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when the project uses event-driven interfaces (event-consumer or event-producer types in the interface configuration).

## What It Does

Reviews event-driven patterns including event schema design, producer/consumer implementation, error handling, dead letter topics, and operational readiness. Validates CloudEvents envelope compliance, schema registry integration, saga patterns, idempotency mechanisms, and consumer group configuration. Covers the full event lifecycle from producer publishing through consumer processing and error recovery.

## Usage

```
/x-review-events
/x-review-events OrderCreatedEvent
/x-review-events PaymentConsumer
```

## See Also

- [x-review-api](../x-review-api/) -- REST API design review
- [x-test-contract-lint](../x-test-contract-lint/) -- AsyncAPI contract validation
- [x-obs-instrument](../x-obs-instrument/) -- OpenTelemetry instrumentation for event tracing
