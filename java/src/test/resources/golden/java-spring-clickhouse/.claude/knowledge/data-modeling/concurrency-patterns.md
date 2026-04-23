# Concurrency Patterns

> Patterns for managing concurrent data access in {{DB_TYPE}} applications. For connection pool configuration, see `database-patterns` KP.

## Optimistic Locking

Assumes conflicts are rare. Reads proceed without locks; writes verify version before committing.

### Implementation

```sql
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

### Update with Version Check

```sql
UPDATE orders
SET status = 'CONFIRMED', version = version + 1
WHERE id = 42 AND version = 5;
```

If affected rows = 0, another transaction modified the record. The application MUST retry or reject.

### Application-Level Pattern

```java
// Domain entity carries version field
public record Order(long id, String status, long version) {}

// Repository checks affected rows
int updated = jdbc.update(
    "UPDATE orders SET status = ?, version = version + 1 "
    + "WHERE id = ? AND version = ?",
    newStatus, order.id(), order.version());
if (updated == 0) {
    throw new OptimisticLockException(order.id());
}
```

### Retry Strategy

- Retry up to 3 times with exponential backoff
- Re-read entity before each retry
- Log conflict occurrences for monitoring
- Alert when retry rate exceeds threshold

### Trade-offs

| Pro | Con |
|-----|-----|
| No lock contention on reads | Retry overhead on conflicts |
| High throughput for read-heavy | Starvation under high contention |
| Simple implementation | Client must handle retry logic |

### When to Use

- Read-heavy workloads (read:write ratio > 10:1)
- Low probability of concurrent writes to same record
- Short transaction duration

---

## Pessimistic Locking

Acquires lock before reading, preventing concurrent modifications.

### SELECT FOR UPDATE

```sql
BEGIN;
SELECT * FROM orders WHERE id = 42 FOR UPDATE;
-- perform business logic
UPDATE orders SET status = 'CONFIRMED' WHERE id = 42;
COMMIT;
```

### NOWAIT and SKIP LOCKED

```sql
-- Fail immediately if locked
SELECT * FROM orders WHERE id = 42 FOR UPDATE NOWAIT;

-- Skip locked rows (useful for queue processing)
SELECT * FROM orders
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED;
```

### Lock Timeout

```sql
SET lock_timeout = '5s';
SELECT * FROM orders WHERE id = 42 FOR UPDATE;
```

### Trade-offs

| Pro | Con |
|-----|-----|
| Guaranteed data consistency | Reduced throughput |
| No retry logic needed | Deadlock risk |
| Simple mental model | Connection held during lock |

### When to Use

- High contention on same records
- Financial transactions requiring strict consistency
- Short critical sections with fast completion

### Deadlock Prevention

- Always acquire locks in consistent order (e.g., by ascending ID)
- Keep locked sections as short as possible
- Set lock timeouts to prevent indefinite waits
- Monitor deadlock frequency in database metrics

---

## Saga Pattern

Coordinates distributed transactions across multiple services using compensating actions.

### Choreography-Based Saga

Each service publishes events; other services react.

```
Order Service          Payment Service        Inventory Service
     |                       |                       |
     |-- OrderCreated ------>|                       |
     |                       |-- PaymentProcessed -->|
     |                       |                       |-- InventoryReserved
     |<-- SagaCompleted -----|<----------------------|
```

### Orchestration-Based Saga

A central coordinator directs the saga steps.

```
Saga Orchestrator
     |
     |-- CreateOrder -------> Order Service
     |<-- OrderCreated ------/
     |
     |-- ProcessPayment ----> Payment Service
     |<-- PaymentProcessed --/
     |
     |-- ReserveInventory --> Inventory Service
     |<-- InventoryReserved -/
     |
     |-- CompleteSaga
```

### Compensating Actions

Every saga step MUST have a compensating action for rollback:

| Step | Action | Compensation |
|------|--------|-------------|
| 1 | Create Order | Cancel Order |
| 2 | Process Payment | Refund Payment |
| 3 | Reserve Inventory | Release Inventory |

### Saga State Machine

```
STARTED -> STEP_1_PENDING -> STEP_1_COMPLETED
        -> STEP_2_PENDING -> STEP_2_COMPLETED
        -> STEP_3_PENDING -> STEP_3_COMPLETED -> COMPLETED
        -> STEP_N_FAILED -> COMPENSATING -> COMPENSATED
```

### Rules

- Each step must be idempotent (safe to retry)
- Compensating actions must also be idempotent
- Persist saga state for recovery after failures
- Set timeouts for each step with automatic compensation
- Log every state transition for debugging

### Choreography vs Orchestration

| Aspect | Choreography | Orchestration |
|--------|-------------|---------------|
| Coupling | Loose | Tighter to coordinator |
| Visibility | Distributed (harder to trace) | Centralized (easier to monitor) |
| Complexity | Grows with services | Centralized in orchestrator |
| Failure handling | Each service handles | Coordinator handles |

---

## Distributed Locks

Cross-service mutual exclusion for shared resources.

### Redis-Based Lock

```
SET lock:resource-42 owner-uuid NX EX 30
```

- `NX`: Only set if not exists (acquire)
- `EX 30`: Expire after 30 seconds (TTL prevents deadlock)

### Release Pattern

Only the owner can release:

```
if GET lock:resource-42 == owner-uuid then
    DEL lock:resource-42
```

### Rules

- ALWAYS set a TTL on distributed locks
- Use unique owner ID to prevent releasing another's lock
- Implement fencing tokens for critical operations
- Monitor lock acquisition time and contention rate
- Prefer optimistic locking when possible (simpler)

### Trade-offs

| Pro | Con |
|-----|-----|
| Cross-service coordination | External dependency (Redis/ZK) |
| Simple API | Clock skew risks with TTL |
| TTL prevents permanent deadlock | Split-brain in network partitions |

### When to Use

- Exactly-once processing across services
- Resource allocation that spans services
- Leader election for singleton processes
