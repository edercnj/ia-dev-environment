# EventStoreDB — Query Optimization

## Read Patterns

EventStoreDB is optimized for stream-based reads, not ad-hoc queries:

| Operation | Performance | Use Case |
|-----------|-------------|----------|
| Read stream forward | Fast (sequential I/O) | Aggregate rehydration |
| Read stream backward | Fast (index lookup) | Latest events |
| Read all (filtered) | Moderate (filtered scan) | Projections, subscriptions |
| Read category | Moderate (projection stream) | Category-wide queries |

## Subscription Tuning

### Catch-Up Subscriptions

```java
// Optimized catch-up subscription with batching
client.subscribeToStream("$ce-Order",
    SubscribeToStreamOptions.get()
        .fromStart()
        .resolveLinkTos(),
    new SubscriptionListener() {
        @Override
        public void onEvent(Subscription sub,
                ResolvedEvent event) {
            buffer.add(event);
            if (buffer.size() >= BATCH_SIZE) {
                processBatch(buffer);
                buffer.clear();
                updateCheckpoint(event.getPosition());
            }
        }
    });
```

### Subscription Tuning Parameters

| Parameter | Default | Recommendation | Purpose |
|-----------|---------|---------------|---------|
| `maxCount` (read) | 4096 | 1000-4096 per read | Events per read batch |
| `resolveLinkTos` | false | true for category streams | Resolve linked events |
| Buffer size | Varies | 500-1000 events | Client-side batch size |
| Checkpoint interval | N/A | Every 100-500 events | Reduce replay on restart |

### Read Performance

```java
// Read forward with batch size
ReadResult result = client.readStream("Order-001",
    ReadStreamOptions.get()
        .fromStart()
        .maxCount(100)
        .forwards())
    .get();
```

| Read Direction | Use Case | Index Used |
|---------------|----------|------------|
| Forward | Aggregate rebuild, full replay | Sequential scan |
| Backward | Latest N events, tail read | Reverse index |

## Persistent Subscription Optimization

### Configuration

```java
client.createToStream("$ce-Order",
    "order-processor",
    CreatePersistentSubscriptionToStreamOptions.get()
        .fromStart()
        .resolveLinkTos()
        .checkPointAfter(2000)           // ms
        .checkPointLowerBound(10)        // min events before checkpoint
        .checkPointUpperBound(1000)      // max events before forced checkpoint
        .maxRetryCount(5)
        .messageTimeout(30_000)          // ms
        .maxSubscriberCount(10)
        .consumerStrategy(
            ConsumerStrategy.ROUND_ROBIN));
```

### Consumer Strategies

| Strategy | Distribution | Use Case |
|----------|-------------|----------|
| `ROUND_ROBIN` | Events distributed evenly | Independent event processing |
| `DISPATCH_TO_SINGLE` | All events to one consumer | Ordered processing required |
| `PINNED` | Events pinned by stream | Per-stream ordering with parallelism |

### Persistent Subscription Monitoring

```bash
# Check subscription stats
curl https://localhost:2113/subscriptions/\$ce-Order/order-processor/info \
  -u admin:changeit
```

| Metric | Healthy | Unhealthy |
|--------|---------|-----------|
| `lastProcessedEventNumber` | Advancing | Stuck |
| `connectionCount` | >= 1 | 0 (no consumers) |
| `totalInFlightMessages` | < buffer size | = buffer size (backpressure) |
| `parkedMessageCount` | 0 or low | Growing (processing failures) |
| `averageItemsPerSecond` | Stable | Declining |

## Projection Optimization

### System Projection Performance

| Projection | Impact | Recommendation |
|-----------|--------|---------------|
| `$by_category` | Moderate write overhead | Enable only if category queries needed |
| `$by_event_type` | Moderate write overhead | Enable only if event-type queries needed |
| `$stream_by_category` | Low overhead | Enable for stream discovery |
| `$streams` | Low overhead | Enable for monitoring |

### Custom Projection Patterns

```javascript
// Efficient: process only needed events
fromCategory('Order')
  .when({
    OrderCreated: function(state, event) {
      // Only process creation events
      return state;
    }
  });

// Inefficient: process all events, filter in handler
fromAll()
  .when({
    $any: function(state, event) {
      if (event.streamId.startsWith('Order-')) {
        // Process
      }
    }
  });
```

### Projection Rules

| Rule | Detail |
|------|--------|
| Filter early | Use `fromCategory` or `fromStream`, not `fromAll` |
| Minimize state | Keep projection state small; use references, not copies |
| Avoid side effects | Projections must be deterministic and pure |
| Checkpoint frequently | Reduce replay time after restart |
| Separate read model updates | Use subscriptions, not projections, for external writes |

## Scavenge Optimization

```bash
# Check database size
curl https://localhost:2113/stats \
  -u admin:changeit | jq '.proc.diskIo'
```

| Parameter | Recommendation |
|-----------|---------------|
| Scavenge frequency | Weekly for active databases |
| Scavenge during | Low-traffic periods |
| Merge threshold | After multiple scavenges, chunks need merging |
| Monitor duration | Track scavenge time; increasing duration = growing database |

### Chunk Merge

```bash
# Merge chunks after scavenge (reduces file count)
curl -X POST https://localhost:2113/admin/mergeindexes \
  -u admin:changeit
```

## Memory and Connection Tuning

| Parameter | Default | Recommendation |
|-----------|---------|---------------|
| `MaxMemTableSize` | 1M entries | Increase for write-heavy workloads |
| `CachedChunks` | Auto | Set based on available RAM |
| `ReaderThreadsCount` | 4 | Match to concurrent read subscriptions |
| `StreamInfoCacheCapacity` | 100000 | Increase for many streams |

## Query Patterns for CQRS

### Aggregate Load (Write Side)

```java
// Load aggregate from stream
List<ResolvedEvent> events = client
    .readStream("Order-001",
        ReadStreamOptions.get().fromStart())
    .get()
    .getEvents();

OrderAggregate order = new OrderAggregate();
for (ResolvedEvent event : events) {
    order.apply(deserialize(event));
}
```

### Read Model Query (Read Side)

Read models are stored in external databases (PostgreSQL, MongoDB, Elasticsearch). EventStoreDB feeds them via subscriptions:

| Read Model Store | Best For | Query Type |
|-----------------|----------|------------|
| PostgreSQL | Structured queries, joins | SQL |
| MongoDB | Document queries, flexible schema | Aggregation pipeline |
| Elasticsearch | Full-text search, analytics | Search DSL |
| Redis | Fast key-value lookups | GET/SET |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Ad-hoc queries on event store | Not designed for arbitrary queries | Build read models via subscriptions |
| `fromAll()` in projections | Processes every event in the store | Use `fromCategory` or `fromStream` |
| No checkpoint storage | Full replay on consumer restart | Store checkpoint every 100-500 events |
| Large batch reads (> 10K) | Memory spike, slow response | Read in smaller batches (1000-4096) |
| Synchronous projection updates | Blocks event processing | Use async consumers with backpressure |
| No monitoring on subscriptions | Silent failures, growing lag | Monitor parked messages and lag |
