# Amazon Neptune — Modeling Patterns

## Version Matrix

| Engine | Version | Key Features |
|--------|---------|-------------|
| **Neptune** | 1.2.x | Gremlin, SPARQL, OpenCypher support, serverless |
| **Neptune** | 1.3.x | Neptune Analytics, vector search, graph algorithms |
| **Neptune Serverless** | GA | Auto-scaling, pay-per-use, min/max NCU configuration |

## Gremlin vs SPARQL: When to Use Each

| Criterion | Gremlin (Property Graph) | SPARQL (RDF) |
|-----------|-------------------------|--------------|
| Data model | Vertices + edges with properties | Subject-predicate-object triples |
| Query style | Imperative (step-by-step traversal) | Declarative (pattern matching) |
| Schema | Schema-free, flexible | Ontology-driven (RDFS/OWL) |
| Best for | Application data, recommendations, fraud | Knowledge graphs, linked data, compliance |
| Bulk load format | CSV (vertices + edges files) | N-Triples, N-Quads, Turtle, RDF/XML |
| Driver | Apache TinkerPop Gremlin driver | Apache Jena, Eclipse RDF4J |
| Standards body | Apache TinkerPop | W3C |

**Decision shortcut:**
- Gremlin: application-centric, team knows imperative programming, variable properties per vertex
- SPARQL: standards-required, multi-source federation, ontology-based reasoning needed

## Vertex and Edge Design (Gremlin)

### Vertex Conventions

| Convention | Rule | Example |
|-----------|------|---------|
| Label naming | lowercase_with_underscores | `user`, `product`, `order_item` |
| ID generation | Application-generated UUIDs | `~id: "usr-550e8400-..."` |
| Property types | String, number, date | Neptune supports limited types |
| Multi-value properties | Use Set cardinality | `g.V().property(set, 'tag', 'java')` |

### Edge Conventions

| Convention | Rule | Example |
|-----------|------|---------|
| Label naming | lowercase_with_underscores | `purchased`, `follows`, `works_at` |
| Direction | Model natural relationship direction | `user --purchased--> product` |
| Properties on edges | Timestamps, weights, metadata | `purchased {date: '2024-01-15', amount: 99.99}` |
| Edge ID | Auto-generated or application-specified | Prefer auto for simplicity |

### Gremlin Vertex/Edge Creation

```groovy
// Create vertex
g.addV('user')
  .property(id, 'usr-001')
  .property('name', 'Alice')
  .property('email', 'alice@example.com')
  .property('createdAt', datetime('2024-01-15T10:30:00Z'))

// Create edge
g.V('usr-001').addE('purchased')
  .to(g.V('prod-001'))
  .property('date', datetime('2024-03-20T14:00:00Z'))
  .property('amount', 99.99)
```

## Neptune-Specific Constraints

| Constraint | Value | Impact |
|-----------|-------|--------|
| Max vertex/edge properties | No hard limit, but payload size matters | Keep property count reasonable |
| Property value size | 55 MB per property value | Store large data in S3, reference by URI |
| Batch mutation size | 1500 statements per request | Batch larger writes via bulk loader |
| Concurrent queries | Depends on instance size | Scale up instance or use read replicas |
| Transaction isolation | Read committed | No serializable isolation |
| OpenCypher support | Subset of Cypher (read + limited write) | Use Gremlin for full write capability |

## Property Limits

| Type | Supported | Notes |
|------|-----------|-------|
| String | Yes | UTF-8, up to 55 MB |
| Integer | Yes | 64-bit signed |
| Float/Double | Yes | IEEE 754 |
| Boolean | Yes | true/false |
| DateTime | Yes | ISO 8601 format |
| Byte array | No | Store as Base64 string or S3 reference |
| Nested objects | No | Flatten or encode as JSON string |
| Arrays | Set/List cardinality | `property(set, 'key', 'val')` |

## RDF Modeling (SPARQL)

### Triple Conventions

```turtle
@prefix ex: <http://example.com/> .
@prefix schema: <http://schema.org/> .

ex:user-001 a schema:Person ;
    schema:name "Alice" ;
    schema:email "alice@example.com" ;
    ex:purchasedAt "2024-01-15"^^xsd:date .
```

### Named Graphs for Context

```sparql
GRAPH <http://example.com/graph/orders> {
    ex:user-001 ex:purchased ex:product-001 .
}
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| **Supernode** | Single vertex with millions of edges | Partition with intermediate vertices |
| **Property as vertex** | Storing IDs as properties instead of edges | Model as proper edges |
| **No ID strategy** | Auto-generated IDs are opaque | Use application-meaningful IDs |
| **Large payloads** | Storing blobs in properties | Reference S3 objects by URI |
| **Ignoring cardinality** | Duplicate multi-value properties | Use `set` cardinality for uniqueness |
| **Cross-region queries** | High latency to Neptune endpoint | Deploy application in same VPC/region |

## Framework Integration

| Framework | Dependency | Connection |
|-----------|-----------|------------|
| **Java (Gremlin)** | `org.apache.tinkerpop:gremlin-driver` | Cluster builder with Neptune endpoint |
| **Java (SPARQL)** | `org.apache.jena:jena-arq` | SPARQL HTTP endpoint |
| **Spring Boot** | Manual `@Bean` configuration | No official starter |

### Gremlin Java Connection

```java
Cluster cluster = Cluster.build()
    .addContactPoint("your-neptune-endpoint")
    .port(8182)
    .enableSsl(true)
    .serializer(Serializers.GRAPHBINARY_V1)
    .create();

GraphTraversalSource g =
    traversal().withRemote(
        DriverRemoteConnection.using(cluster));
```
