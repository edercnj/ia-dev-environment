# Neo4j — Modeling Patterns

## Version Matrix

| Version | Key Features | EOL |
|---------|-------------|-----|
| **4.4** | Multi-database, fabric, role-based access | LTS |
| **5.x** | Composite databases, new Cypher features, block storage | Current |
| **5.13+** | Vector indexes, CDC, improved GDS | Latest |

## Node and Label Conventions

| Convention | Rule | Example |
|-----------|------|---------|
| Label naming | PascalCase, singular | `:User`, `:Product`, `:Order` |
| Multi-label | Use for cross-cutting concerns | `(:User:Premium)`, `(:User:Admin)` |
| Max labels per node | Keep under 4 for query clarity | `:Person:Employee:Manager` |
| Avoid generic labels | Labels must convey meaning | `:Entity` is too vague |

## Relationship Conventions

| Convention | Rule | Example |
|-----------|------|---------|
| Type naming | UPPER_SNAKE_CASE, active verb | `PURCHASED`, `FOLLOWS`, `WORKS_AT` |
| Direction | Model natural direction | `(user)-[:PURCHASED]->(product)` |
| Properties | Timestamps, weights, metadata | `[:PURCHASED {date: date(), amount: 99.99}]` |
| Avoid bidirectional | Query both directions instead | `MATCH (a)-[:KNOWS]-(b)` |

## Property Graph Patterns

### Intermediate Node (Hyperedge)

When a relationship needs to connect more than two nodes or carry complex data:

```cypher
// Anti-pattern: overloaded relationship
(user)-[:REVIEWED {rating: 5, text: "Great"}]->(product)

// Pattern: intermediate node for rich relationships
(user)-[:WROTE]->(review:Review {rating: 5, text: "Great"})-[:ABOUT]->(product)
```

### Timeline / Event Chain

```cypher
(user)-[:FIRST_EVENT]->(e1:Event)-[:NEXT]->(e2:Event)-[:NEXT]->(e3:Event)
```

### Linked List for Ordered Data

```cypher
(head:ListItem)-[:NEXT]->(item2:ListItem)-[:NEXT]->(item3:ListItem)
```

### Tree / Hierarchy

```cypher
(parent:Category)-[:HAS_CHILD]->(child:Category)-[:HAS_CHILD]->(grandchild:Category)
(node:Category)-[:PARENT_OF*]->(descendant:Category)
```

## Label Strategies

| Strategy | Use Case | Example |
|----------|----------|---------|
| **Role-based** | Entity plays different roles | `(:Person:Customer)`, `(:Person:Employee)` |
| **State-based** | Track lifecycle state via label | `(:Order:Pending)`, `(:Order:Shipped)` |
| **Temporal** | Partition by time | `(:Event:Y2024)`, `(:Event:Y2025)` |

**Warning:** State-based labels require label removal on transition.
Use a `status` property instead for frequent state changes.

## Constraint and Index Definitions

```cypher
-- Unique constraint (also creates an index)
CREATE CONSTRAINT user_email_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.email IS UNIQUE;

-- Node key (composite uniqueness)
CREATE CONSTRAINT order_item_key IF NOT EXISTS
FOR (oi:OrderItem) REQUIRE (oi.orderId, oi.productId) IS NODE KEY;

-- Existence constraint (property must exist)
CREATE CONSTRAINT user_name_exists IF NOT EXISTS
FOR (u:User) REQUIRE u.name IS NOT NULL;

-- Range index for fast lookups
CREATE INDEX user_created_range IF NOT EXISTS
FOR (u:User) ON (u.createdAt);

-- Composite index
CREATE INDEX product_category_status IF NOT EXISTS
FOR (p:Product) ON (p.category, p.status);

-- Full-text index
CREATE FULLTEXT INDEX product_search IF NOT EXISTS
FOR (p:Product) ON EACH [p.name, p.description];

-- Relationship property index (Neo4j 5+)
CREATE INDEX purchased_date IF NOT EXISTS
FOR ()-[r:PURCHASED]-() ON (r.date);
```

## Hard Limits

| Limit | Value | Mitigation |
|-------|-------|------------|
| Max nodes | ~34 billion (community: limited) | Shard via composite databases |
| Max relationships | ~34 billion | Same as nodes |
| Property size | 2 GB per property (string/byte[]) | Store large data externally |
| Label count | ~65,535 per database | Consolidate labels |
| Transaction size | Limited by heap | Batch with `CALL {} IN TRANSACTIONS` |

## Fatal Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| **Dense node** | Single node with millions of rels | Add intermediate nodes, bucket by time |
| **Supernodes** | Celebrity problem: one node = bottleneck | Fan-out with typed sub-relationships |
| **Property as node** | Storing IDs as properties instead of relationships | Model as proper relationships |
| **Timestamps as labels** | Label explosion over time | Use indexed properties instead |
| **No indexes** | Full graph scan on every query | Create indexes for all lookup patterns |
| **Cartesian products** | Unconnected MATCH clauses | Always connect patterns or use WITH |

## Framework Integration

| Framework | Dependency | Repository Pattern |
|-----------|-----------|-------------------|
| **Spring Boot** | `spring-boot-starter-data-neo4j` | `Neo4jRepository<T, ID>` |
| **Quarkus** | `quarkus-neo4j` | Neo4j Java Driver (manual) |

### Spring Data Neo4j Example

```java
@Node("User")
public class UserNode {
    @Id @GeneratedValue
    private Long id;
    private String email;
    private String name;

    @Relationship(type = "PURCHASED")
    private List<ProductNode> purchases;
}

public interface UserRepository
        extends Neo4jRepository<UserNode, Long> {
    Optional<UserNode> findByEmail(String email);
}
```
