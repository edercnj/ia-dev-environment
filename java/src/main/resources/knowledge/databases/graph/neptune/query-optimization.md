# Amazon Neptune — Query Optimization

## Query Hints

Neptune supports query hints to guide the query planner.

### Gremlin Query Hints

```groovy
// Use Neptune-specific hints via withSideEffect
g.withSideEffect('Neptune#useDFE', true)
  .V().hasLabel('user')
  .has('email', 'alice@example.com')
  .out('purchased')
  .valueMap('name', 'price')

// Repeat step with emit for BFS traversal
g.V('usr-001')
  .repeat(out('follows').simplePath())
  .until(loops().is(3))
  .emit()
  .dedup()
  .valueMap('name')
```

### SPARQL Query Hints

```sparql
PREFIX hint: <http://aws.amazon.com/neptune/vocab/v01/QueryHints#>

SELECT ?person ?name WHERE {
    hint:Query hint:joinOrder "Ordered" .
    ?person a <http://schema.org/Person> .
    ?person <http://schema.org/name> ?name .
    ?person <http://example.com/purchased> ?product .
}
```

| Hint | Effect | Use Case |
|------|--------|----------|
| `joinOrder: Ordered` | Respect triple pattern order | When planner chooses wrong order |
| `queryTimeout` | Set per-query timeout | Long-running analytical queries |
| `Neptune#useDFE` | Force DFE engine | Complex multi-hop traversals |

## DFE (Data Flow Engine)

Neptune's DFE is an alternative query execution engine optimized for complex traversals.

### When DFE Helps

| Scenario | DFE Benefit |
|----------|-------------|
| Multi-hop traversals (3+ hops) | Parallel execution across hops |
| Aggregations over large result sets | Streaming aggregation |
| UNION / OPTIONAL patterns (SPARQL) | Better join strategies |
| Pattern matching with filters | Pushdown filter optimization |

### When DFE Does Not Help

| Scenario | Use TinkerPop Engine Instead |
|----------|------------------------------|
| Simple single-hop lookups | DFE overhead exceeds benefit |
| Small graphs (< 10K vertices) | Engine startup cost dominates |
| Write-heavy workloads | DFE is read-optimized |
| Mutations (add/drop) | DFE does not execute mutations |

### Enable/Disable DFE

```groovy
// Per-query DFE enable
g.withSideEffect('Neptune#useDFE', true)
  .V().hasLabel('user').out('follows').count()

// Per-query DFE disable
g.withSideEffect('Neptune#useDFE', false)
  .V('usr-001').valueMap()
```

## Instance Sizing

| Instance Type | vCPU | RAM | Storage | Best For |
|--------------|------|-----|---------|----------|
| `db.t3.medium` | 2 | 4 GB | Up to 64 GB | Dev/test |
| `db.r5.large` | 2 | 16 GB | Up to 64 TB | Small production |
| `db.r5.xlarge` | 4 | 32 GB | Up to 64 TB | Medium production |
| `db.r5.4xlarge` | 16 | 128 GB | Up to 64 TB | Large production |
| `db.r5.12xlarge` | 48 | 384 GB | Up to 64 TB | Heavy analytical |
| Serverless (NCU) | Auto | Auto | Auto | Variable workloads |

### Serverless Configuration

| Parameter | Range | Default | Purpose |
|-----------|-------|---------|---------|
| Min NCU | 1-128 | 2.5 | Minimum capacity (cost floor) |
| Max NCU | 2.5-128 | 128 | Maximum capacity (cost ceiling) |

**Rule:** Set min NCU to handle baseline query load. Set max NCU for peak load with headroom.

## Caching

### Neptune Buffer Pool

Neptune automatically caches frequently accessed data in its buffer pool (similar to page cache).

| Metric | Monitor Via | Target |
|--------|-----------|--------|
| Buffer cache hit ratio | CloudWatch `BufferCacheHitRatio` | > 99% |
| Gremlin requests/sec | CloudWatch `GremlinRequestsPerSec` | Baseline + 20% headroom |
| SPARQL requests/sec | CloudWatch `SparqlRequestsPerSec` | Baseline + 20% headroom |

### Application-Level Caching

```java
// Cache frequent traversal results
@Cacheable(value = "userConnections",
           key = "#userId")
public List<String> getFriendIds(String userId) {
    return g.V(userId)
        .out("follows")
        .id()
        .toList()
        .stream()
        .map(Object::toString)
        .toList();
}
```

## Query Timeouts

| Level | Configuration | Default |
|-------|--------------|---------|
| Cluster-wide | `neptune_query_timeout` parameter | 120000 ms |
| Per-query (Gremlin) | `evaluationTimeout` in request | Cluster default |
| Per-query (SPARQL) | `neptune_query_timeout` header | Cluster default |

```groovy
// Set per-query timeout (Gremlin)
g.with('evaluationTimeout', 30000)
  .V().hasLabel('user').count()
```

```bash
# Set per-query timeout (SPARQL)
curl -X POST https://endpoint:8182/sparql \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'query=SELECT (COUNT(*) as ?c) WHERE { ?s ?p ?o }' \
  -d 'neptune_query_timeout=30000'
```

## Monitoring

### Key CloudWatch Metrics

| Metric | Alarm Threshold | Action |
|--------|----------------|--------|
| `CPUUtilization` | > 70% sustained | Scale up instance |
| `BufferCacheHitRatio` | < 99% | Scale up (more RAM) |
| `VolumeBytesUsed` | > 80% of limit | Monitor growth rate |
| `GremlinRequestsPerSec` | Baseline + 50% | Check for runaway queries |
| `GremlinErrors` | > 0 sustained | Investigate error types |
| `MainRequestQueuePendingRequests` | > 0 sustained | Scale up or optimize queries |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No query timeout | Runaway queries consume all resources | Set cluster-wide and per-query timeouts |
| Fetching all properties | Wastes bandwidth | Use `valueMap('prop1', 'prop2')` |
| Unbounded traversal | Traverses entire graph | Always limit depth with `until()` or `loops()` |
| Ignoring DFE | Slow complex queries | Enable DFE for multi-hop reads |
| Wrong instance size | Constant cache misses | Right-size based on graph size and query load |
| No read replicas | Writer saturated by reads | Add read replicas for read-heavy workloads |
