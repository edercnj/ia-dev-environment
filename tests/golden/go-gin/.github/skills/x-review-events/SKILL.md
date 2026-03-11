---
name: x-review-events
description: >
  Skill: Event-Driven Review -- Validates event schemas, producer/consumer
  patterns, error handling, dead letter topics, CloudEvents envelope compliance,
  and operational readiness. Use when reviewing event producers or consumers.
---

# Skill: Event-Driven Review

## Description

Reviews event-driven patterns: event schema design, producer/consumer implementation, error handling, dead letter topics, and operational readiness. Covers CloudEvents envelope, schema registry integration, and saga patterns.

**Condition**: This skill applies when the project uses event-driven interfaces.

## Prerequisites

- Event definitions exist (event classes, Avro/Protobuf schemas, or JSON Schema)
- Message broker dependency configured (Kafka, RabbitMQ, SQS, etc.)
- Producer and/or consumer implementations exist

## Knowledge Pack References

Before reviewing, read the event-driven conventions:
- `.github/skills/protocols/SKILL.md` -- CloudEvents envelope, event naming, schema registry, ordering guarantees, broker patterns

## Execution Flow

1. **Discover event definitions** -- Scan for event classes, schemas, Avro/Protobuf definitions:
   - List all event types with their schemas
   - Identify event envelope structure

2. **Discover producers** -- Scan for classes publishing events:
   - Map producers to topics and event types
   - Identify publishing patterns (outbox, direct)

3. **Discover consumers** -- Scan for classes consuming events:
   - Map consumers to topics and consumer groups
   - Identify idempotency mechanisms

4. **Validate event design** -- Check naming, envelope, versioning:
   - Past tense event names
   - CloudEvents spec compliance
   - Schema registry integration
   - Data minimization

5. **Validate producer patterns** -- Check reliability:
   - Outbox or at-least-once delivery
   - Acknowledgment levels
   - Error handling and retry

6. **Validate consumer patterns** -- Check processing:
   - Idempotency implementation
   - Offset commit strategy
   - Dead letter topic configuration
   - Graceful shutdown

7. **Validate operational readiness** -- Check monitoring:
   - Consumer lag monitoring
   - Dead letter topic alerts
   - Schema registry health

8. **Generate report** -- Summarize findings as checklist

## Event Design Checklist (10 points)

### Schema & Naming (1-4)
1. Event names use past tense (`OrderCreated`, `PaymentProcessed`, `InventoryReserved`)
2. Event envelope follows CloudEvents spec or project-standard envelope (type, source, id, time, data)
3. Schema registered in schema registry (Avro, Protobuf, or JSON Schema)
4. Event versioning strategy defined: additive-only changes in same version, new event type for breaking changes

### Data Design (5-7)
5. Events contain only necessary data (data minimization, no full entity dump)
6. Correlation ID present for distributed tracing
7. No sensitive data in event payload (or encrypted if absolutely required)

### Topic/Queue Design (8-10)
8. Topic naming follows convention: `{domain}.{entity}.{event}`
9. Partition key chosen for ordering guarantees (e.g., entity ID for per-entity ordering)
10. Message retention and compaction policy documented

## Producer Checklist (8 points)

11. Transactional outbox pattern used for reliable publishing
12. OR: at-least-once delivery with consumer idempotency (if outbox not used, document why)
13. Producer acknowledgment level appropriate (all replicas for critical events)
14. Serialization uses registered schema (not ad-hoc JSON serialization)
15. Error handling: failed publishes logged, retried with backoff, dead-lettered after max retries
16. Idempotency key on producer if exactly-once semantics required
17. Event published AFTER successful business operation (not before)
18. Trace context propagated in event headers (W3C Trace Context)

## Consumer Checklist (10 points)

19. Consumer is idempotent (processing same event twice produces same result)
20. Idempotency key stored and checked (event ID + consumer group, or business key)
21. Consumer group ID follows convention: `{service}.{purpose}`
22. Offset commit strategy: commit after successful processing (not auto-commit)
23. Error handling: failed processing -> retry with backoff -> dead letter topic after N retries
24. Dead letter topic monitored with alerts
25. Consumer lag monitored (alert if lag exceeds threshold)
26. Deserialization errors handled gracefully (poison pill protection)
27. Processing timeout configured (prevent infinite blocking)
28. Graceful shutdown: stop consuming, finish in-flight, commit offsets, then exit

## Output Format

```
## Event-Driven Review -- [Event/Change Description]

### Event Design: HIGH / MEDIUM / LOW
### Producer Quality: HIGH / MEDIUM / LOW
### Consumer Quality: HIGH / MEDIUM / LOW

### Event Design Findings
1. [Finding with file, line, issue, fix]

### Producer Findings
1. [Finding with file, line, issue, fix]

### Consumer Findings
1. [Finding with file, line, issue, fix]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on event-driven patterns, consult:
- `.github/skills/x-review-events/SKILL.md`
- `.github/skills/protocols/SKILL.md`
