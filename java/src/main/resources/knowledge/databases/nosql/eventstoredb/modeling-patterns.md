# EventStoreDB — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **23.10** | Persistent subscriptions v2, improved projections | LTS |
| **24.2** | Auto-scavenge, improved clustering | Current |
| **24.6+** | Enhanced connectors, improved read performance | Latest |

## Core Concepts

EventStoreDB is an event-native database designed for Event Sourcing and CQRS:

| Concept | Detail |
|---------|--------|
| Event | Immutable fact that happened (past tense verb) |
| Stream | Ordered sequence of events for an aggregate |
| Projection | Transformation of events into read models |
| Subscription | Real-time event notification mechanism |
| Category | Logical grouping of streams by prefix |

## Stream Design

### Stream Naming Convention

```
{category}-{aggregate_id}
```

| Element | Convention | Example |
|---------|-----------|---------|
| Category | PascalCase, singular noun | `Order`, `Customer`, `Payment` |
| Aggregate ID | UUID or meaningful ID | `550e8400-e29b-41d4-a716-446655440000` |
| Full stream name | `{Category}-{Id}` | `Order-550e8400-...` |
| System stream | `$` prefix | `$ce-Order` (category projection) |

### Stream Examples

```
Order-001       -> [OrderCreated, ItemAdded, ItemAdded, OrderConfirmed, OrderShipped]
Customer-042    -> [CustomerRegistered, AddressUpdated, EmailVerified]
Payment-abc     -> [PaymentInitiated, PaymentAuthorized, PaymentCaptured]
```

## Event Schema Design

### Event Structure

```json
{
  "eventType": "OrderCreated",
  "data": {
    "orderId": "order-001",
    "customerId": "customer-042",
    "items": [
      {"productId": "prod-100", "quantity": 2, "unitPrice": 2999}
    ],
    "totalAmount": 5998,
    "currency": "USD",
    "createdAt": "2024-01-15T10:30:00Z"
  },
  "metadata": {
    "correlationId": "corr-abc-123",
    "causationId": "cmd-xyz-789",
    "userId": "user-007",
    "timestamp": "2024-01-15T10:30:00Z",
    "schemaVersion": 1
  }
}
```

### Event Naming Conventions

| Convention | Rule | Example |
|-----------|------|---------|
| Past tense | Events describe what happened | `OrderCreated`, `PaymentCaptured` |
| Domain language | Use ubiquitous language | `InvoiceIssued`, not `InvoiceSaved` |
| Specific | Avoid generic names | `PriceAdjusted`, not `OrderUpdated` |
| No CRUD verbs | Events are not CRUD operations | `ItemAddedToCart`, not `CartUpdated` |

### Event Types per Aggregate

| Pattern | Use Case |
|---------|----------|
| Lifecycle events | `Created`, `Activated`, `Suspended`, `Closed` |
| State change events | `AddressUpdated`, `StatusChanged` |
| Domain action events | `PaymentCaptured`, `OrderShipped` |
| Error/compensation events | `PaymentDeclined`, `RefundIssued` |

## Metadata Standards

| Field | Required | Purpose |
|-------|----------|---------|
| `correlationId` | Yes | Links related events across aggregates |
| `causationId` | Yes | ID of the command/event that caused this event |
| `userId` | Yes | Who triggered the action |
| `timestamp` | Yes | When the event was created |
| `schemaVersion` | Yes | Event schema version for upcasting |
| `source` | Optional | Service/module that produced the event |

## Projections

### System Projections

| Projection | Stream | Purpose |
|-----------|--------|---------|
| `$by_category` | `$ce-{Category}` | All events for a category |
| `$by_event_type` | `$et-{EventType}` | All events of a specific type |
| `$stream_by_category` | `$category-{Category}` | Stream names per category |
| `$streams` | `$streams` | All stream names |

### Custom Projections

```javascript
// Project order totals by customer
fromCategory('Order')
  .foreachStream()
  .when({
    $init: function() {
      return { totalAmount: 0, orderCount: 0 };
    },
    OrderCreated: function(state, event) {
      state.totalAmount += event.data.totalAmount;
      state.orderCount += 1;
      return state;
    }
  });
```

## Subscription Patterns

| Type | Use Case | Delivery | Checkpointing |
|------|----------|----------|---------------|
| Catch-up | Read model rebuild, event replay | Pull (read forward) | Client-managed |
| Persistent | Competing consumers, work distribution | Push (server-managed) | Server-managed |
| Filtered | Selective event processing | Pull with server filter | Client-managed |

### Persistent Subscription

```java
// Create persistent subscription
client.createToStream("Order",
    "order-processor",
    CreatePersistentSubscriptionToStreamOptions.get()
        .fromStart()
        .resolveLinkTos()
        .maxRetryCount(5)
        .messageTimeout(30_000));
```

## Framework Integration

| Framework | Dependency | Pattern |
|-----------|-----------|---------|
| Java client | `com.eventstore:db-client-java` | gRPC-based client |
| Spring Boot | Manual `@Bean` configuration | No official starter |

### Java Client Example

```java
EventStoreDBClient client = EventStoreDBClient.create(
    EventStoreDBConnectionString.parseOrThrow(
        "esdb://localhost:2113?tls=false"));

// Append events
EventData event = EventData.builderAsJson(
        "OrderCreated", orderCreatedData)
    .metadataAsJson(metadata)
    .build();

client.appendToStream("Order-001",
    AppendToStreamOptions.get()
        .expectedRevision(ExpectedRevision.noStream()),
    event).get();
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| CRUD-style events | Lose business intent | Use domain-specific event names |
| Large events (> 1 MB) | Storage and network overhead | Keep events small, reference external data |
| Mutable event data | Breaks event sourcing guarantee | Events are immutable; use compensating events |
| No correlation/causation IDs | Cannot trace event chains | Always include both in metadata |
| Stream per entity field | Excessive stream count | One stream per aggregate instance |
| Missing schema version | Cannot evolve event schemas | Always include `schemaVersion` in metadata |
