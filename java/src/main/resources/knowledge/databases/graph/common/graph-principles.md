# Graph Database Principles

## Property Graph vs RDF

| Aspect | Property Graph | RDF (Resource Description Framework) |
|--------|---------------|--------------------------------------|
| Data model | Nodes + relationships with properties | Subject-predicate-object triples |
| Schema | Flexible, optional constraints | Ontology-driven (RDFS/OWL) |
| Query language | Cypher (Neo4j), Gremlin (TinkerPop) | SPARQL |
| Identity | Internal node/edge IDs | URIs (globally unique) |
| Use case | Application data, recommendations | Knowledge graphs, linked data, semantic web |
| Tooling | Neo4j, Neptune (Gremlin), JanusGraph | Neptune (SPARQL), Virtuoso, Blazegraph |
| Learning curve | Lower (intuitive graph model) | Higher (RDF/OWL semantics) |

**Decision shortcut:**
- Property graph: application-centric queries, social networks, fraud detection, recommendations
- RDF: standards-based integration, knowledge management, regulatory/compliance graphs, multi-source data federation

## Traversal Paradigms

| Paradigm | Language | Style | Strengths |
|----------|----------|-------|-----------|
| **Declarative** | Cypher, SPARQL | Pattern matching ("find all paths matching X") | Readable, optimizer-driven |
| **Imperative** | Gremlin | Step-by-step traversal ("start here, go there") | Fine-grained control, streaming |

### Cypher (Declarative)

```cypher
MATCH (u:User)-[:PURCHASED]->(p:Product)<-[:PURCHASED]-(other:User)
WHERE u.id = $userId AND NOT (u)-[:PURCHASED]->(p)
RETURN p.name, count(other) AS buyers
ORDER BY buyers DESC LIMIT 10
```

### Gremlin (Imperative)

```groovy
g.V().has('User', 'id', userId)
  .out('PURCHASED').aggregate('owned')
  .in('PURCHASED').where(neq('u'))
  .out('PURCHASED').where(without('owned'))
  .groupCount().by('name')
  .order(local).by(values, desc).limit(local, 10)
```

## CAP Positioning

| System | CAP Choice | Consistency Model | Notes |
|--------|-----------|-------------------|-------|
| Neo4j (single) | CA | ACID transactions | Single-instance: full ACID |
| Neo4j (cluster) | CP | Causal consistency | Leader-based writes, follower reads |
| Neptune | CP | Read-after-write | Strongly consistent reads from writer endpoint |

## When to Use Graph vs Relational

### Choose Graph Database When

| Criterion | Threshold |
|-----------|-----------|
| Relationship depth | Queries traverse 3+ hops regularly |
| Fan-out ratio | Entities have highly variable connection counts |
| Query pattern | "Find all paths," "shortest path," "connected components" |
| Schema evolution | Relationships and node types change frequently |
| Data shape | Network/graph structure is the primary concern |
| Join complexity | Relational queries require 4+ JOINs on the same data |

### Choose Relational Database When

| Criterion | Threshold |
|-----------|-----------|
| Data structure | Tabular, well-defined schema |
| Transactions | Multi-table ACID with complex constraints |
| Aggregations | Heavy GROUP BY, SUM, COUNT across large datasets |
| Reporting | Ad-hoc analytical queries dominate |
| Relationships | Shallow (1-2 hops), well-defined foreign keys |
| Tooling | Team expertise in SQL, existing BI integrations |

### Anti-Patterns (When NOT to Use Graph)

| Scenario | Why Graph is Wrong | Better Alternative |
|----------|-------------------|-------------------|
| Simple CRUD with FK | Overhead without benefit | PostgreSQL, MySQL |
| Time-series data | Append-heavy, no traversal | TimescaleDB, InfluxDB |
| Document storage | Hierarchical, not networked | MongoDB, DynamoDB |
| Full-text search | Graph indexes not optimized for text | Elasticsearch, OpenSearch |
| Large-scale analytics | Scan-heavy, not traversal-heavy | ClickHouse, BigQuery |
| Blob/file storage | Binary data, no relationships | S3, MinIO |

## Graph Data Modeling Fundamentals

### Node Design

| Principle | Rule |
|-----------|------|
| Single responsibility | One node type = one domain concept |
| Labels as types | Use labels for categorization: `(:User)`, `(:Product)` |
| Properties as attributes | Scalar values on nodes: `name`, `createdAt` |
| Avoid property overload | If a property group is reused, extract to a separate node |

### Relationship Design

| Principle | Rule |
|-----------|------|
| Direction matters | Relationships have a direction; query both ways if needed |
| Verbs for types | `PURCHASED`, `FOLLOWS`, `WORKS_AT` (past tense or present) |
| Properties on edges | Timestamp, weight, metadata on the relationship itself |
| Avoid generic types | `RELATED_TO` is useless; be specific: `AUTHORED`, `REVIEWED` |

### Index Strategies

| Index Type | Use Case | Example |
|------------|----------|---------|
| Node property | Equality/range lookups | Find user by email |
| Composite | Multi-property lookups | Find product by category + status |
| Full-text | Text search within properties | Search product descriptions |
| Spatial/point | Geolocation queries | Find nearby stores |

## Graph Algorithm Categories

| Category | Algorithms | Use Case |
|----------|-----------|----------|
| **Pathfinding** | Shortest path, A*, Dijkstra | Navigation, network routing |
| **Centrality** | PageRank, betweenness, closeness | Influence detection, ranking |
| **Community** | Louvain, label propagation | Fraud rings, customer segments |
| **Similarity** | Jaccard, cosine, overlap | Recommendations, deduplication |
| **Link prediction** | Common neighbors, Adamic-Adar | Suggested connections |

## Framework Integration

| Database | Java Driver | Spring Integration |
|----------|------------|-------------------|
| Neo4j | `org.neo4j.driver:neo4j-java-driver` | `spring-boot-starter-data-neo4j` |
| Neptune (Gremlin) | `org.apache.tinkerpop:gremlin-driver` | Manual configuration |
| Neptune (SPARQL) | Apache Jena / Eclipse RDF4J | Manual configuration |
| JanusGraph | `org.janusgraph:janusgraph-driver` | Manual configuration |

## Sensitive Data in Graph Databases

| Rule | Detail |
|------|--------|
| PII in properties | Encrypt or tokenize PII stored as node/relationship properties |
| Traversal leakage | Relationship existence can reveal information even without property access |
| Access control | Implement node-level or subgraph-level access control |
| Audit trail | Log all graph mutations with user context |
