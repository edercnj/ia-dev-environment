---
name: patterns-outbox
description: "Transactional Outbox Pattern: reliable event publishing with polling publisher and CDC strategies, outbox table design, and anti-patterns."
---

# Knowledge Pack: Transactional Outbox Pattern

## Purpose

Provides structured guidance for implementing the Transactional Outbox Pattern, ensuring reliable event delivery in event-driven systems. Covers the dual-write problem, outbox table design, SELECT FOR UPDATE SKIP LOCKED, Debezium CDC, and anti-patterns. Included when `architecture.outbox_pattern = true`.

---

## 1. The Problem

### Why Save + Publish Does Not Guarantee Consistency

Publishing events directly to a message broker inside a database transaction creates a **dual-write problem**. Two independent systems (database and broker) cannot participate in a single atomic transaction without a distributed transaction protocol (2PC), which is impractical in microservice architectures.

### Failure Scenarios

| Scenario | What Happens | Consequence |
|----------|-------------|-------------|
| Commit succeeds, publish fails | Business data is persisted but the event never reaches consumers | **Lost event** — downstream services never learn about the state change |
| Publish succeeds, commit fails (rollback) | The event is delivered to consumers but the originating transaction rolled back | **Phantom event** — consumers react to a change that never happened |
| Publish before commit, crash mid-transaction | Event is delivered; transaction never commits | **Phantom event** — same as above |
| Network partition during publish | Timeout or partial delivery; retries may cause duplicates | **Duplicate or lost event** depending on retry policy |

### Production Consequences

- **Lost events** cause data inconsistency across services. A payment service commits a charge but the order service never receives the confirmation event, leaving the order stuck in "pending" state.
- **Phantom events** trigger downstream actions for operations that were rolled back. An inventory service decrements stock for an order that was cancelled at the database level.
- **Silent data drift** accumulates over time and is extremely difficult to detect or reconcile after the fact.

### The Root Cause

```
// ANTI-PATTERN: dual-write inside a transaction
@Transactional
public void processOrder(Order order) {
    orderRepository.save(order);       // write 1: database
    eventPublisher.publish(             // write 2: broker
        new OrderCreatedEvent(order));  // NOT atomic with write 1
}
```

The two writes target independent systems. No single transaction boundary can guarantee both succeed or both fail.

---

## 2. Transactional Outbox Solution

### Core Principle

Instead of publishing directly to the broker, write the event to an **outbox table** in the **same database transaction** as the business data change. A separate relay process reads from the outbox table and publishes to the broker asynchronously.

This guarantees **atomicity**: the event is persisted if and only if the business transaction commits.

### Outbox Table Schema

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_pending ON outbox_events (created_at)
    WHERE status = 'PENDING';
```

### Column Descriptions

| Column | Type | Purpose |
|--------|------|---------|
| `id` | UUID | Unique event identifier; used by consumers for deduplication |
| `topic` | VARCHAR(255) | Target broker topic or routing key |
| `payload` | JSONB | Serialized event data (domain event content) |
| `status` | VARCHAR(20) | Processing status: PENDING, PROCESSING, SENT, FAILED, DEAD_LETTER |
| `retry_count` | INTEGER | Number of publication attempts; incremented on each failure |
| `created_at` | TIMESTAMP | Event creation time; used for ordering in polling queries |
| `updated_at` | TIMESTAMP | Last modification time; updated on status transitions |

### Partial Index Rationale

The `idx_outbox_pending` index is a **partial index** filtered on `status = 'PENDING'`. This design choice provides:

- **Reduced index size**: Only PENDING rows are indexed; SENT and DEAD_LETTER rows do not consume index space
- **Faster polling queries**: The relay process queries only PENDING events, and the partial index makes this query an index-only scan
- **Lower write amplification**: Fewer index entries to maintain on INSERT and UPDATE

### Status Transitions

```
PENDING --> PROCESSING --> SENT
                |
                +--> FAILED --> PENDING (retry)
                       |
                       +--> DEAD_LETTER (max retries exceeded)
```

### Application-Side Usage

```java
// CORRECT: single transaction, single database
@Transactional
public void processOrder(Order order) {
    orderRepository.save(order);
    outboxRepository.save(new OutboxEvent(
        "order.created",
        serializeEvent(new OrderCreatedEvent(order))
    ));
    // Both writes committed atomically
}
```

---

## 3. Polling Publisher

### Overview

The Polling Publisher is a background process that periodically queries the outbox table for PENDING events, publishes them to the message broker, and updates their status.

### Locking Strategy: SELECT FOR UPDATE SKIP LOCKED

```sql
SELECT id, topic, payload, retry_count
FROM outbox_events
WHERE status = 'PENDING'
ORDER BY created_at ASC
FOR UPDATE SKIP LOCKED
LIMIT :batch_size;
```

**Why SKIP LOCKED:**

- Multiple relay instances can run concurrently without blocking each other
- Each instance locks and processes a non-overlapping subset of rows
- Rows locked by another instance are silently skipped, not waited on
- Provides horizontal scalability for the relay process

### Processing Flow

1. **Select batch**: Query PENDING events with FOR UPDATE SKIP LOCKED
2. **Mark PROCESSING**: Update status to PROCESSING within the same transaction
3. **Publish**: Send each event to the broker, awaiting acknowledgment
4. **Mark SENT or FAILED**: Update status based on broker response
5. **Handle failures**: Increment retry_count; move to DEAD_LETTER if max retries exceeded

### Exponential Backoff

The polling interval increases exponentially when no events are found, reducing database load during idle periods:

```
interval = min(base_interval * 2^consecutive_empty_polls, max_interval)
```

| Parameter | Recommended Value |
|-----------|------------------|
| `base_interval` | 1 second |
| `max_interval` | 30 seconds |
| `reset_on_events` | true (reset to base when events found) |

### Batch Processing

| Parameter | Recommended Value | Rationale |
|-----------|------------------|-----------|
| `batch_size` | 100 | Balance between throughput and transaction duration |
| `max_processing_time` | 30 seconds | Prevent long-running transactions that hold locks |
| `commit_interval` | Per batch | Commit after each batch to release locks promptly |

### Retry and Dead Letter

```java
if (retryCount >= MAX_RETRIES) {
    updateStatus(eventId, "DEAD_LETTER");
    alertOps("Event exceeded max retries", eventId);
} else {
    updateStatusAndRetry(eventId, "PENDING",
        retryCount + 1);
}
```

| Parameter | Recommended Value |
|-----------|------------------|
| `max_retries` | 5 |
| `retry_backoff` | Exponential (1s, 2s, 4s, 8s, 16s) |
| `dead_letter_alert` | Mandatory — alert operations team |

---

## 4. CDC with Debezium

### Overview

Change Data Capture (CDC) is an alternative relay strategy that tails the database transaction log (WAL in PostgreSQL, binlog in MySQL) instead of polling the outbox table. Debezium is the most widely adopted open-source CDC platform.

### How CDC Works

1. Debezium connector monitors the database transaction log
2. When a new row is inserted into `outbox_events`, Debezium captures the change
3. Debezium publishes the event payload directly to the configured Kafka topic
4. The outbox row can be deleted immediately after capture (or retained for audit)

### Basic Debezium Connector Configuration

```json
{
  "name": "outbox-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "db-host",
    "database.port": "5432",
    "database.dbname": "myservice",
    "database.user": "debezium",
    "database.password": "${DEBEZIUM_DB_PASSWORD}",
    "table.include.list": "public.outbox_events",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "${routedByValue}",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.key": "id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.route.by.field": "topic"
  }
}
```

### CDC vs Polling Publisher

| Criterion | Polling Publisher | CDC (Debezium) |
|-----------|-----------------|----------------|
| Latency | Seconds (poll interval) | Milliseconds (log tailing) |
| Database load | Periodic queries + row locking | Reads transaction log (minimal query load) |
| Operational complexity | Low (application-level code) | Medium (requires Kafka Connect, Debezium, connector management) |
| Ordering guarantee | Per-batch ordering by created_at | Strict commit-order from transaction log |
| Horizontal scaling | Multiple instances with SKIP LOCKED | Single connector per database (or partitioned) |
| Infrastructure | Application only | Kafka Connect cluster + Debezium |
| Failure recovery | Application retry logic | Connector offset tracking in Kafka |

### When to Use CDC vs Polling

**Use Polling Publisher when:**
- Low operational complexity is a priority
- Event latency of 1-5 seconds is acceptable
- No existing Kafka Connect infrastructure
- Team has limited CDC experience

**Use CDC (Debezium) when:**
- Sub-second event latency is required
- High event throughput demands minimal database polling load
- Kafka Connect infrastructure already exists
- Strict commit-order event publishing is required

---

## 5. Anti-patterns of the Outbox

### 5.1 Publishing Directly to the Broker Inside a Transaction

```java
// ANTI-PATTERN
@Transactional
public void process(Order order) {
    repo.save(order);
    broker.publish(event);  // dual-write: NOT atomic
}
```

**Consequence:** Lost events when publish fails after commit, or phantom events when publish succeeds but transaction rolls back.

### 5.2 Outbox Table Without a Partial Index

```sql
-- ANTI-PATTERN: full index on all rows
CREATE INDEX idx_outbox_status ON outbox_events (status);
```

**Consequence:** The index grows with the total row count including SENT and DEAD_LETTER rows. Polling queries scan a bloated index, degrading performance as the table grows.

**Correct approach:** Use a partial index on `status = 'PENDING'` to keep the index small and fast.

### 5.3 Polling Without Exponential Backoff

```java
// ANTI-PATTERN: fixed interval polling
while (true) {
    List<Event> events = pollOutbox();
    publish(events);
    Thread.sleep(1000);  // always 1 second
}
```

**Consequence:** Constant database load even when no events exist. Wastes CPU, I/O, and database connections during idle periods.

**Correct approach:** Use exponential backoff that increases the interval when no events are found and resets when events are discovered.

### 5.4 No Dead Letter for Events Exceeding Max Retries

```java
// ANTI-PATTERN: infinite retry
if (publishFailed) {
    event.setStatus("PENDING");  // retry forever
}
```

**Consequence:** Poison events (e.g., malformed payload, permanently unreachable topic) block the outbox indefinitely. Other events behind the poison event are delayed or never processed.

**Correct approach:** After a configurable number of retries, move the event to DEAD_LETTER status and alert the operations team. Provide tooling to inspect and replay dead-lettered events.

### 5.5 No Monitoring of Lag Between Outbox and Broker

**Consequence:** Events accumulate in the outbox table without detection. By the time the lag is noticed, downstream services have stale data, SLA violations have occurred, and manual reconciliation is required.

**Correct approach:** Monitor the following metrics and set alerts:

| Metric | Alert Threshold | Description |
|--------|----------------|-------------|
| PENDING event count | > 1000 | Number of unprocessed events in the outbox |
| Oldest PENDING event age | > 5 minutes | Time since the oldest unprocessed event was created |
| FAILED event count | > 0 | Any event that failed to publish |
| DEAD_LETTER count (24h) | > 0 | Events moved to dead letter in the last 24 hours |
| Publish success rate | < 99.9% | Ratio of successful publishes to total attempts |

---

## Related Knowledge Packs

| Pack | Relationship |
|------|-------------|
| `architecture-patterns` | Outbox, saga, dead letter queue, event sourcing, and idempotency pattern references |
| `resilience` | Retry and dead letter handling patterns |
| `architecture-cqrs` | Event sourcing as an alternative where the event store IS the outbox |
