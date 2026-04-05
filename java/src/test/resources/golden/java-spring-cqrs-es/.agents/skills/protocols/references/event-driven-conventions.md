# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Message Broker Conventions

## Topic Naming

### Format: `{domain}.{entity}.{event}`

| Segment | Convention | Example |
|---------|-----------|---------|
| Domain | Lowercase bounded context | `orders`, `payments`, `inventory` |
| Entity | Lowercase entity name | `order`, `payment`, `stock-item` |
| Event | Past tense, lowercase, kebab-case | `created`, `status-changed`, `cancelled` |
| Full topic | Dot-separated segments | `orders.order.created` |

### Additional Naming Patterns

| Pattern | Format | Use Case |
|---------|--------|----------|
| Internal events | `{domain}.{entity}.{event}` | Standard domain events |
| Integration events | `integration.{source}.{target}.{event}` | Cross-domain contracts |
| System events | `system.{component}.{event}` | Infrastructure events (deploy, health) |
| DLT topics | `{original-topic}.dlt` | Dead letter topics |
| Retry topics | `{original-topic}.retry.{attempt}` | Staged retry topics |
| Compacted topics | `{domain}.{entity}.snapshot` | State snapshot topics |

**Rules:**
- All lowercase; no uppercase characters
- Dot (`.`) as segment separator; hyphen (`-`) for multi-word within a segment
- Maximum topic name length: 249 characters (Kafka limit)
- Topic names MUST be descriptive; NEVER use abbreviations (`ord.ord.crt` is forbidden)
- Environment prefix is handled by cluster/namespace, NOT in the topic name
- NEVER include environment names in topic names (`prod.orders.order.created` is forbidden)

## Partition Strategy

### Choosing a Partition Key

| Strategy | Partition Key | Ordering Guarantee | Use Case |
|----------|---------------|-------------------|----------|
| By entity ID | `orderId`, `customerId` | Per-entity | Default; most common |
| By tenant ID | `tenantId` | Per-tenant | Multi-tenant SaaS |
| By region | `regionCode` | Per-region | Geographically distributed systems |
| By aggregate root | `aggregateId` | Per-aggregate | Event sourcing / DDD |
| Round-robin | None (null key) | None | Maximum throughput, no ordering needed |

**Rules:**
- ALWAYS set a partition key unless ordering is explicitly not needed
- Partition key MUST produce even distribution; avoid hot partitions
- Monitor partition skew; alert if any partition handles more than 3x the average load
- NEVER use high-cardinality keys that change over time (e.g., timestamp)
- NEVER use low-cardinality keys for high-throughput topics (e.g., boolean status)
- Document the partition key for every topic in the schema registry or topic catalog

### Partition Count Guidelines

| Topic Throughput | Recommended Partitions | Notes |
|-----------------|:----------------------:|-------|
| Low (< 1K msg/s) | 6 | Minimum for basic parallelism |
| Medium (1K-10K msg/s) | 12-24 | Scale with consumer count |
| High (10K-100K msg/s) | 24-64 | Match consumer instance count |
| Very high (> 100K msg/s) | 64-128 | Requires careful key distribution |

Partition count can be increased but NEVER decreased. Plan for growth with headroom.

## Consumer Lag Monitoring

### Metrics and Thresholds

| Metric | Warning Threshold | Critical Threshold | Action |
|--------|:-----------------:|:------------------:|--------|
| Consumer lag (messages) | > 10,000 | > 100,000 | Scale consumers or investigate bottleneck |
| Consumer lag (time) | > 5 minutes | > 30 minutes | Immediate investigation required |
| Lag growth rate | Increasing for > 10 min | Increasing for > 30 min | Consumer is falling behind; scale up |
| Rebalance frequency | > 2 per hour | > 5 per hour | Investigate consumer stability |
| Commit latency | > 1 second | > 5 seconds | Consumer processing too slow |

**Rules:**
- Consumer lag MUST be monitored for every consumer group
- Alerting MUST be configured at both warning and critical thresholds
- Lag dashboards MUST show: current lag, lag trend over time, processing rate, error rate
- Review lag thresholds quarterly; adjust based on SLA requirements
- Lag alert MUST include: consumer group, topic, partition, current lag, lag velocity

## Producer Acknowledgment Levels

| Level | Durability | Latency | When to Use |
|-------|:----------:|:-------:|-------------|
| `acks=0` (fire-and-forget) | None | Lowest | Metrics, telemetry, non-critical logs |
| `acks=1` (leader only) | Medium | Low | Default for most use cases |
| `acks=all` (all replicas) | Highest | Higher | Financial events, audit logs, critical state changes |

**Rules:**
- Default to `acks=1` for general-purpose topics
- Use `acks=all` for any event where data loss is unacceptable
- When using `acks=all`, set `min.insync.replicas=2` (or N/2+1 for larger replication factors)
- NEVER use `acks=0` for business-critical events
- Document the ack level for every producer in the service configuration
- Producer retries: enable with idempotent producer (`enable.idempotence=true`) when `acks=all`

### Producer Configuration Guidelines

| Parameter | Recommended Value | Purpose |
|-----------|-------------------|---------|
| `retries` | 3 (or `Integer.MAX_VALUE` with idempotent) | Automatic retry on transient failure |
| `retry.backoff.ms` | 100-1000 | Delay between retries |
| `max.in.flight.requests.per.connection` | 5 (with idempotent) or 1 (without) | Ordering guarantee |
| `linger.ms` | 5-20 | Batching window for throughput |
| `batch.size` | 16384-65536 | Batch size in bytes |
| `compression.type` | `lz4` or `zstd` | Compress for throughput; `zstd` for best ratio |

## Message Retention Policies

| Topic Type | Retention | Reasoning |
|------------|-----------|-----------|
| Standard event topics | 7 days | Default; covers most replay needs |
| Audit/compliance topics | 365 days or more | Regulatory requirements |
| High-volume telemetry | 24 hours | Cost management; data flows to analytics |
| Integration topics | 14 days | Buffer for downstream service outages |
| DLT topics | 30 days | Investigation and replay window |
| Compacted topics | Infinite (compaction) | State snapshots; only latest per key retained |

**Rules:**
- Retention MUST be configured per topic; NEVER rely on broker defaults
- Document retention policy for every topic in the topic catalog
- Retention changes MUST be reviewed; reducing retention can cause data loss for slow consumers
- Monitor disk usage; alert when broker storage exceeds 70% capacity
- Size-based retention (`retention.bytes`) can supplement time-based as a safety cap

## Compacted Topics for State Snapshots

Compacted topics retain only the latest value per key, functioning as a distributed key-value store.

| Use Case | Key | Value |
|----------|-----|-------|
| Current entity state | Entity ID | Latest state snapshot |
| Configuration | Config key | Current config value |
| User profiles | User ID | Current profile |
| Feature flags | Flag name | Current flag state |

**Rules:**
- Compacted topics MUST have `cleanup.policy=compact` (or `compact,delete` for combined)
- Keys MUST NOT be null on compacted topics (null key = tombstone logic breaks)
- To delete an entry, produce a message with the key and null value (tombstone)
- Tombstones are retained for `delete.retention.ms` (default 24h) before physical removal
- Compacted topics are NOT suitable for event streams; use them for state only
- Segment size and min compaction lag affect how quickly stale values are removed
- Monitor compaction lag; alert if compaction falls significantly behind

### Compaction Configuration

| Parameter | Recommended | Purpose |
|-----------|-------------|---------|
| `min.cleanable.dirty.ratio` | 0.5 | Trigger compaction when 50% of log is dirty |
| `min.compaction.lag.ms` | 0 (or up to 1 hour) | Minimum time before a message is eligible for compaction |
| `delete.retention.ms` | 86400000 (24h) | How long tombstones are retained |
| `segment.ms` | 604800000 (7 days) | Force segment roll for compaction eligibility |

## Schema Evolution Rules per Broker

### Compatibility Modes

| Mode | Allowed Changes | Use Case |
|------|----------------|----------|
| BACKWARD | New schema can read old data | Default; consumers update first |
| FORWARD | Old schema can read new data | Producers update first |
| FULL | Both backward and forward compatible | Safest; both directions |
| NONE | Any change allowed | Development only; NEVER in production |

### Evolution Rules by Format

| Change | Avro | Protobuf | JSON Schema |
|--------|:----:|:--------:|:-----------:|
| Add optional field | Safe | Safe | Safe |
| Add required field with default | Safe (BACKWARD) | N/A (proto3 all optional) | Unsafe |
| Remove optional field | Safe (FORWARD) | Safe (use reserved) | Safe |
| Remove required field | Unsafe | N/A | Unsafe |
| Rename field | Unsafe | Safe (number unchanged) | Unsafe |
| Change field type | Unsafe | Unsafe | Unsafe |

**Rules:**
- Production topics MUST use BACKWARD or FULL compatibility mode
- Schema registry MUST reject incompatible schema registrations
- Test schema compatibility in CI before deploying producer changes
- NEVER use NONE compatibility in production environments
- When a breaking change is unavoidable, create a new topic (e.g., `orders.order.created.v2`)

## Anti-Patterns (FORBIDDEN)

- Topic names with environment prefixes (`prod.orders.order.created`) -- use cluster separation
- Topics without documented partition keys -- every topic MUST have a documented partitioning strategy
- Consumer groups without lag monitoring -- lag MUST be monitored and alerted
- `acks=0` for business-critical events -- use `acks=1` minimum, `acks=all` for critical data
- Relying on broker default retention -- configure retention explicitly per topic
- Compacted topics used for event streams -- compaction is for state snapshots only
- NONE compatibility mode in production -- always enforce schema compatibility
- Reducing partition count on an existing topic -- partitions can only be added
- Producers without idempotence enabled when using `acks=all` -- enables safe retries
- Topics without schema registration -- all topics MUST have registered schemas
- Consumer groups shared across services -- each service owns its consumer group
- Hard-coded broker addresses in application code -- use configuration or service discovery
- Topics with unbounded message sizes -- enforce maximum message size (1 MB default)
- Ignoring consumer rebalance frequency -- frequent rebalances indicate instability


---

# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Event-Driven Architecture Conventions

## CloudEvents Envelope Standard

All events MUST use the CloudEvents specification (v1.0) as the envelope format. This is non-negotiable for interoperability.

### Required Attributes

| Attribute | Type | Description | Example |
|-----------|------|-------------|---------|
| `specversion` | String | CloudEvents spec version | `1.0` |
| `id` | String (UUID) | Unique event identifier | `550e8400-e29b-41d4-a716-446655440000` |
| `source` | URI | Event producer identifier | `/services/order-service` |
| `type` | String | Event type (see naming below) | `com.company.orders.OrderCreated` |
| `time` | Timestamp (RFC 3339) | Event creation time in UTC | `2026-02-19T14:30:00Z` |
| `datacontenttype` | String | Payload content type | `application/json` |

### Recommended Extension Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `subject` | String | Entity the event is about | `/orders/12345` |
| `correlationid` | String (UUID) | Trace correlation across services |
| `causationid` | String (UUID) | ID of the event that caused this event |
| `dataschema` | URI | Schema reference for the data payload |
| `partitionkey` | String | Key used for ordering/partitioning |

## Event Naming

### Convention: Reverse Domain + Past Tense

| Pattern | Format | Example |
|---------|--------|---------|
| Full qualified type | `com.company.{domain}.{EventName}` | `com.company.orders.OrderCreated` |
| Event name | PascalCase, past tense | `OrderCreated`, `PaymentProcessed` |
| Domain | Lowercase bounded context | `orders`, `payments`, `inventory` |

### Naming Rules

| Rule | Correct | Incorrect |
|------|---------|-----------|
| Past tense (something happened) | `OrderCreated` | `CreateOrder`, `OrderCreate` |
| Noun + past participle | `PaymentProcessed` | `ProcessPayment` |
| Specific, not generic | `OrderShipped` | `EntityUpdated` |
| No CRUD verbs | `InventoryReserved` | `InventoryUpdated` |
| Domain-scoped | `com.company.orders.OrderCancelled` | `OrderCancelled` (missing domain) |

**Commands vs Events:** Commands are imperative (`CreateOrder`), events are facts (`OrderCreated`). Events describe something that already happened. NEVER name an event as a command.

## Schema Registry

### Supported Formats

| Format | When to Use | Tradeoffs |
|--------|-------------|-----------|
| JSON Schema | Default for JSON payloads; broadest tooling support | Verbose, no binary encoding |
| Avro | High-throughput systems with schema evolution needs | Compact binary, requires registry, less readable |
| Protobuf | When gRPC is already in the stack | Compact binary, strong typing, steeper learning curve |

### Registry Requirements

- ALL event schemas MUST be registered before producing events
- Schema registry MUST enforce compatibility checks on registration
- Producer MUST embed schema ID or version in the event (header or envelope attribute)
- Consumer MUST validate payload against the registered schema
- Schema registry MUST be highly available (treat as critical infrastructure)

## Event Versioning

### Additive Changes Only (Non-Breaking)

| Change | Safe? | Notes |
|--------|:-----:|-------|
| Add optional field | Yes | Consumers ignore unknown fields |
| Add new event type | Yes | No existing consumers subscribe to it |
| Add optional extension attribute | Yes | Consumers ignore unknown attributes |
| Widen a numeric type (int32 -> int64) | Yes | Backward compatible in most formats |

### Breaking Changes Require New Event Type

| Change | Action Required |
|--------|----------------|
| Remove a field | Create new event type (e.g., `OrderCreatedV2`) |
| Rename a field | Create new event type |
| Change a field type | Create new event type |
| Change the semantic meaning of a field | Create new event type |
| Change from required to optional | Create new event type |

**Versioning strategy:**
- Suffix with version when breaking: `com.company.orders.OrderCreatedV2`
- Produce both old and new events during migration (dual-write)
- Set sunset date for the old event type (minimum 90 days)
- Monitor consumer subscriptions; remove old event after all consumers migrate

## Ordering Guarantees

### Partition Key Strategy

| Entity Type | Partition Key | Ordering Guarantee |
|-------------|---------------|-------------------|
| Per-entity ordering | Entity ID (e.g., `orderId`) | All events for one entity arrive in order |
| Per-tenant ordering | Tenant ID | All events for one tenant arrive in order |
| Per-aggregate ordering | Aggregate root ID | All events for one aggregate arrive in order |
| Global ordering | None (single partition) | Total order; severely limits throughput |

**Rules:**
- Choose the partition key based on the smallest scope that requires ordering
- NEVER use global ordering unless the domain absolutely requires it (throughput bottleneck)
- Document the partition key for every event type
- Consumers MUST handle out-of-order events when processing across partitions
- Include a monotonic sequence number per entity for consumer-side reordering if needed

## Consumer Group Patterns

| Pattern | Description | When to Use |
|---------|-------------|-------------|
| Competing consumers | Multiple instances in same group share partitions | Scale processing horizontally |
| Fan-out | Multiple groups each receive all events | Different services process same events independently |
| Single consumer | One instance processes all partitions | Low-volume, ordering-critical |

**Rules:**
- Consumer group ID format: `{service-name}-{purpose}` (e.g., `order-service-projector`, `notification-service-mailer`)
- NEVER share a consumer group across different services
- Each consumer group MUST have its own offset/checkpoint tracking
- Rebalancing strategy: cooperative (preferred) or eager (simpler but causes pauses)

## Exactly-Once vs At-Least-Once

| Delivery | Guarantee | Implementation Cost | When to Use |
|----------|-----------|:-------------------:|-------------|
| At-most-once | Events may be lost | Low | Metrics, non-critical logs |
| At-least-once | Events may be duplicated | Medium | Default for all business events |
| Exactly-once | No loss, no duplicates | High | Financial transactions, inventory |

**At-least-once (recommended default):**
- Producer retries on failure
- Consumer commits offset AFTER successful processing
- Consumer MUST be idempotent (use event ID for deduplication)

**Exactly-once (when required):**
- Use transactional outbox pattern (write event + state in same DB transaction)
- Or use broker-native exactly-once (e.g., Kafka transactions)
- Consumer deduplicates using event ID stored in processed-events table
- Deduplication window: minimum 7 days retention of processed event IDs

## Dead Letter Topic Configuration

| Parameter | Default | Notes |
|-----------|---------|-------|
| DLT name | `{original-topic}.dlt` | Consistent naming convention |
| Maximum retries before DLT | 3 | With exponential backoff between retries |
| Retry delays | 1s, 5s, 30s | Increasing delays per attempt |
| DLT retention | 30 days | Enough time for manual investigation |
| DLT alerting threshold | > 0 messages | Alert immediately on any DLT message |

**Rules:**
- Every consumer MUST have a dead letter topic configured
- DLT messages MUST include: original event, error message, retry count, failure timestamp, consumer group ID
- DLT MUST be monitored; alert on new messages within 5 minutes
- Provide tooling to replay events from DLT back to the original topic
- NEVER silently drop failed events; DLT is mandatory

## Event Replay Capability

| Requirement | Implementation |
|-------------|---------------|
| Replay by time range | Broker retains events for configured retention period |
| Replay by event type | Consumer filters during replay |
| Replay by entity ID | Consumer filters by partition key during replay |
| Replay to new consumer group | Create new group with earliest offset |
| Idempotent replay | Consumer deduplicates by event ID |

**Rules:**
- Event retention MUST be at least 7 days (30 days recommended)
- Replay MUST NOT affect other consumer groups
- Consumers MUST be idempotent to support replay without side effects
- Document the replay procedure for each consumer
- Test replay capability at least quarterly

## Correlation ID Propagation

| Hop | Action |
|-----|--------|
| API Gateway / Entry Point | Generate `correlationId` (UUID) if not present in request headers |
| Service processing | Read `correlationId` from incoming request/event; attach to all outgoing events |
| Event producer | Include `correlationId` as CloudEvents extension attribute |
| Event consumer | Read `correlationId` from event; propagate to downstream calls and events |
| Logging | Include `correlationId` in every log line (structured logging) |

**Rules:**
- `correlationId` MUST be propagated across ALL service boundaries (HTTP, gRPC, events)
- NEVER generate a new `correlationId` mid-chain; always propagate the original
- Use `causationId` to link parent-child event relationships within the same correlation
- Include `correlationId` in distributed tracing spans for end-to-end visibility

## Anti-Patterns (FORBIDDEN)

- Events without CloudEvents envelope -- all events MUST use the standard format
- Command-style event names (`CreateOrder`) -- events are past tense facts (`OrderCreated`)
- Unregistered schemas -- all event schemas MUST be in the schema registry
- Breaking changes to existing event types -- create a new versioned event type
- Consumers without dead letter topic -- failed events MUST NOT be silently dropped
- Consumers that are not idempotent -- at-least-once delivery means duplicates will occur
- Global ordering when per-entity ordering suffices -- global ordering kills throughput
- Sharing consumer groups across different services -- each service owns its group
- Events without correlation ID -- breaks distributed tracing
- Producing events outside of a transaction (dual-write problem) -- use outbox pattern or transactions
- Events with sensitive data (PII, secrets) in the payload without encryption
- Unbounded event payloads -- set maximum payload size (1 MB recommended)
- Events that reference data instead of containing it (thin events) without a documented retrieval contract
