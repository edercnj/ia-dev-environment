# Neo4j — Query Optimization

## Index Types

| Type | Syntax | Use Case | Notes |
|------|--------|----------|-------|
| **Range** | `CREATE INDEX ... ON (n.prop)` | Equality, range, ordering, prefix | Default since Neo4j 5 |
| **Text** | `CREATE TEXT INDEX ...` | String CONTAINS, ENDS WITH | Not for equality |
| **Point** | `CREATE POINT INDEX ...` | Geospatial distance/bbox queries | 2D and 3D coordinates |
| **Full-text** | `CREATE FULLTEXT INDEX ...` | Multi-property text search | Lucene-based, separate API |
| **Vector** | `CREATE VECTOR INDEX ...` | Similarity search (5.13+) | Cosine, euclidean distance |
| **Token lookup** | Auto-created | Label/type lookups | Always present, do not drop |

## PROFILE and EXPLAIN

### EXPLAIN (Plan Only)

```cypher
EXPLAIN
MATCH (u:User {email: $email})-[:PURCHASED]->(p:Product)
RETURN p.name, p.price
```

Shows the query plan without executing. Use for quick plan validation.

### PROFILE (Plan + Metrics)

```cypher
PROFILE
MATCH (u:User {email: $email})-[:PURCHASED]->(p:Product)
RETURN p.name, p.price
```

| Metric | Good | Bad |
|--------|------|-----|
| `Rows` | Close to result count | Orders of magnitude higher |
| `DbHits` | Low, proportional to result | Very high (full scan) |
| Operator | `NodeIndexSeek` | `AllNodesScan`, `NodeByLabelScan` |
| `EstimatedRows` vs `Rows` | Close match | Wildly different (stale stats) |
| Page cache hits | > 95% | < 80% (memory pressure) |

**Rule:** Every production query MUST show `NodeIndexSeek` or `DirectedRelationshipIndexSeek` in PROFILE output. `AllNodesScan` on graphs with more than 10,000 nodes is PROHIBITED.

## Traversal Optimization

### Pattern: Anchor + Traverse

Always start traversals from an indexed node (anchor):

```cypher
// Good: starts from indexed user
MATCH (u:User {email: $email})-[:PURCHASED]->(p:Product)
RETURN p

// Bad: starts from unindexed scan
MATCH (p:Product)<-[:PURCHASED]-(u:User {email: $email})
RETURN p
```

The planner usually reorders, but explicit anchor-first is clearer.

### Limit Traversal Depth

```cypher
// Bounded variable-length path
MATCH path = (start:User {id: $id})-[:FOLLOWS*1..3]->(end:User)
RETURN end

// NEVER use unbounded: -[:FOLLOWS*]-> (can traverse entire graph)
```

| Depth | Performance | Use Case |
|-------|-------------|----------|
| 1-2 hops | Fast | Direct connections |
| 3-4 hops | Moderate | Recommendations, friend-of-friend |
| 5+ hops | Expensive | Use graph algorithms (GDS) instead |

### Avoid Cartesian Products

```cypher
// Anti-pattern: disconnected patterns
MATCH (u:User), (p:Product)
WHERE u.id = $userId
RETURN u, p

// Fix: always connect patterns
MATCH (u:User {id: $userId})-[:PURCHASED]->(p:Product)
RETURN u, p
```

## Query Patterns

### Efficient Aggregation

```cypher
// Count relationships without loading properties
MATCH (u:User {id: $userId})-[:PURCHASED]->(p)
RETURN count(p) AS purchaseCount
```

### Batch Processing

```cypher
// Process in transactions (Neo4j 5+)
CALL {
    MATCH (u:User)
    WHERE u.lastLogin < datetime() - duration('P90D')
    SET u:Inactive
} IN TRANSACTIONS OF 10000 ROWS
```

### Subquery for Filtering

```cypher
MATCH (u:User)
WHERE EXISTS {
    MATCH (u)-[:PURCHASED]->(p:Product)
    WHERE p.category = 'Electronics'
}
RETURN u.name
```

## Memory Tuning

| Parameter | Default | Recommendation | Purpose |
|-----------|---------|---------------|---------|
| `server.memory.heap.initial_size` | 512m | 1/4 of RAM (max 31g) | JVM heap |
| `server.memory.heap.max_size` | 512m | 1/4 of RAM (max 31g) | JVM heap ceiling |
| `server.memory.pagecache.size` | auto | Store size + 20% | Off-heap page cache |
| `db.tx_state.memory_allocation` | ON_HEAP | ON_HEAP | Transaction state |

### Page Cache Sizing

```
page_cache = sum(store_files) * 1.2
heap = min(total_ram * 0.25, 31g)
os_reserved = 1g
total_ram >= page_cache + heap + os_reserved
```

### Monitor Memory

```cypher
// Check page cache hit ratio
CALL dbms.queryJmx('org.neo4j:*,name=Page cache')
YIELD attributes
RETURN attributes.HitRatio.value AS hitRatio
```

Target: page cache hit ratio > 98%.

## Cache Warming

### Pre-warm on Startup

```cypher
// Touch all nodes to load into page cache
CALL apoc.warmup.run(true, true, true)
// Arguments: (nodes, rels, properties)
```

### Targeted Warming

```cypher
// Warm specific label
MATCH (u:User) RETURN count(u);
MATCH (p:Product) RETURN count(p);
MATCH ()-[r:PURCHASED]->() RETURN count(r);
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| No indexes on lookup props | AllNodesScan for every query | Create index for every lookup pattern |
| Unbounded traversal `*` | Traverses entire graph | Always set upper bound: `*1..5` |
| Eager operators | Materializes all rows in memory | Rewrite to avoid `COLLECT` on large sets |
| Returning full paths | Serializes all nodes/rels | Return only needed properties |
| String comparison without TEXT index | Slow CONTAINS/ENDS WITH | Create TEXT index |
| Stale statistics | Bad query plans | Run `CALL db.stats.retrieve('GRAPH COUNTS')` |
