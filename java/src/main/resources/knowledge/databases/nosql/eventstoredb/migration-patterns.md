# EventStoreDB — Migration Patterns

## Overview

EventStoreDB stores immutable events. "Migration" means evolving event schemas, rebuilding projections, and managing stream lifecycle:

| Approach | Use Case | Impact |
|----------|----------|--------|
| Event versioning + upcasting | Schema evolution | Zero downtime |
| Stream evolution | Aggregate restructuring | New streams |
| Projection rebuild | Read model changes | Catch-up replay |
| Snapshot management | Performance optimization | Per-aggregate |
| Scavenge | Disk reclamation | Background task |

## Event Versioning

### Schema Version in Metadata

```json
{
  "eventType": "OrderCreated",
  "data": {
    "orderId": "order-001",
    "totalAmount": 5998,
    "currency": "USD"
  },
  "metadata": {
    "schemaVersion": 2
  }
}
```

### Version Evolution Strategy

| Version | Change | Migration |
|---------|--------|-----------|
| v1 | Original schema | Baseline |
| v2 | Added `currency` field | Upcast v1: default `currency = "USD"` |
| v3 | Renamed `amount` to `totalAmount` | Upcast v1/v2: rename field |
| v4 | Split into `OrderCreated` + `OrderPriced` | Upcast v1-v3: emit two events |

## Upcasting Patterns

### Simple Upcaster (Add Field)

```java
public class OrderCreatedUpcaster {

    public EventData upcast(EventData event,
                            int fromVersion) {
        JsonNode data = readJson(event.getData());

        if (fromVersion < 2) {
            ((ObjectNode) data).put("currency", "USD");
        }
        if (fromVersion < 3) {
            JsonNode amount = data.get("amount");
            ((ObjectNode) data).set("totalAmount", amount);
            ((ObjectNode) data).remove("amount");
        }

        return EventData.builderAsJson(
                event.getEventType(),
                data.toString())
            .metadataAsJson(updateVersion(
                event.getMetadata(), 3))
            .build();
    }
}
```

### Upcaster Chain

```java
public class UpcasterChain {
    private final List<Upcaster> upcasters;

    public EventData upcast(EventData event) {
        int version = extractVersion(event);
        EventData current = event;
        for (Upcaster u : upcasters) {
            if (u.appliesTo(
                    current.getEventType(), version)) {
                current = u.upcast(current);
                version = u.targetVersion();
            }
        }
        return current;
    }
}
```

### Upcasting Rules

| Rule | Detail |
|------|--------|
| Never modify stored events | Upcasting happens at read time, never at write |
| Chain upcasters | Each upcaster handles one version jump |
| Default values | Missing fields get sensible defaults |
| Type changes | Map old type to new type in upcaster |
| Event splitting | One old event can produce multiple new events |

## Stream Evolution

### Aggregate Restructuring

When an aggregate's boundaries change (e.g., splitting `Order` into `Order` + `Fulfillment`):

```
// Old: single stream
Order-001 -> [OrderCreated, ItemAdded, PaymentReceived, Shipped]

// New: two streams
Order-001       -> [OrderCreated, ItemAdded, PaymentReceived]
Fulfillment-001 -> [FulfillmentCreated, Shipped]
```

### Stream Migration Process

1. Create new stream structure
2. Write migration projection that reads old stream, transforms, writes to new streams
3. Update application to write to new streams
4. Old streams remain for audit (never delete)

### Stream Linking

```javascript
// Link events from old stream to new stream
fromStream('Order-001')
  .when({
    Shipped: function(state, event) {
      linkTo('Fulfillment-001', event);
    }
  });
```

## Projection Rebuild

### Full Rebuild Process

1. Stop consumers of the read model
2. Drop/truncate the read model store (database table, cache)
3. Start catch-up subscription from the beginning of the stream
4. Process all events through the projection logic
5. Resume normal subscription after catch-up completes

```java
// Catch-up subscription for rebuild
client.subscribeToStream("$ce-Order",
    SubscribeToStreamOptions.get()
        .fromStart()
        .resolveLinkTos(),
    listener);
```

### Incremental Rebuild (Checkpoint-Based)

```java
// Store checkpoint per projection
long lastProcessedPosition =
    checkpointStore.getLastPosition("order-summary");

client.subscribeToAll(
    SubscribeToAllOptions.get()
        .fromPosition(new Position(
            lastProcessedPosition,
            lastProcessedPosition))
        .filter(
            SubscriptionFilter.newBuilder()
                .addStreamNamePrefix("Order-")
                .build()),
    new ProjectionListener(checkpointStore));
```

## Snapshot Management

### Snapshot Pattern

```java
// Save snapshot every N events
public class OrderAggregate {
    private static final int SNAPSHOT_INTERVAL = 100;

    public void saveSnapshot(EventStoreDBClient client) {
        if (version % SNAPSHOT_INTERVAL == 0) {
            EventData snapshot = EventData
                .builderAsJson("OrderSnapshot",
                    serializeState())
                .build();
            client.appendToStream(
                "OrderSnapshot-" + orderId,
                snapshot).get();
        }
    }

    public static OrderAggregate loadFromSnapshot(
            EventStoreDBClient client, String orderId) {
        // Read latest snapshot
        // Then replay events after snapshot
    }
}
```

### Snapshot Rules

| Rule | Detail |
|------|--------|
| Frequency | Snapshot every 100-500 events |
| Separate stream | Store in `{Aggregate}Snapshot-{id}` |
| Include version | Snapshot must record the event version it represents |
| Rebuild-safe | Application must work without snapshots (just slower) |

## Scavenge (Disk Reclamation)

```bash
# Trigger scavenge via API
curl -X POST https://localhost:2113/admin/scavenge \
  -u admin:changeit

# Check scavenge status
curl https://localhost:2113/admin/scavenge/current \
  -u admin:changeit
```

| Parameter | Default | Notes |
|-----------|---------|-------|
| Auto-scavenge | Disabled (24.2+: configurable) | Enable for production |
| Scavenge interval | Manual | Schedule during low-traffic periods |
| Disk freed | Depends on deleted/truncated streams | Monitor disk after scavenge |

## Soft Delete and Truncation

```java
// Soft delete stream (can be recreated)
client.deleteStream("Order-001",
    DeleteStreamOptions.get()
        .expectedRevision(ExpectedRevision.any()))
    .get();

// Truncate stream before position (keep recent events)
client.setStreamMetadata("Order-001",
    StreamMetadata.builder()
        .truncateBefore(500L)  // Remove events 0-499
        .build())
    .get();
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Modifying stored events | Breaks immutability guarantee | Use upcasting at read time |
| No schema version | Cannot evolve event schemas | Always include `schemaVersion` |
| Deleting streams for cleanup | Lose audit trail | Use truncation or soft delete |
| Rebuilding without checkpoint | Full rebuild every time | Store checkpoint position |
| No snapshot for long streams | Slow aggregate load (> 1000 events) | Implement snapshot pattern |
| Upcaster modifying writes | Side effects in read path | Upcasters must be pure functions |
