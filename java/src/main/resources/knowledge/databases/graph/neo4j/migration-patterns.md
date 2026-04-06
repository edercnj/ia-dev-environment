# Neo4j — Migration Patterns

## Overview

Neo4j has **no built-in migration framework** like Flyway. Schema evolution requires versioned Cypher scripts or dedicated tools.

| Approach | Best For | Dependency |
|----------|----------|------------|
| **Neo4j Migrations** | Java/Kotlin projects, CI/CD | `eu.michael-simons.neo4j:neo4j-migrations` |
| **Cypher scripts + shell** | Ops-driven, simple changes | `cypher-shell` only |
| **APOC procedures** | Complex data transformations | `apoc-core` plugin |

## Neo4j Migrations (Java Migration Runner)

### Dependencies

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>eu.michael-simons.neo4j</groupId>
    <artifactId>neo4j-migrations-spring-boot-starter</artifactId>
</dependency>

<!-- Standalone -->
<dependency>
    <groupId>eu.michael-simons.neo4j</groupId>
    <artifactId>neo4j-migrations</artifactId>
</dependency>
```

### Migration File Structure

```
db/neo4j/migrations/
├── V001__Create_user_constraints.cypher
├── V002__Create_product_indexes.cypher
├── V003__Add_category_labels.cypher
├── R001__Refresh_fulltext_index.cypher
└── V004__Migrate_address_to_node.java
```

| Prefix | Type | Behavior |
|--------|------|----------|
| `V` | Versioned | Applied once, tracked in migration chain |
| `R` | Repeatable | Re-applied when content changes |

### Cypher Migration Example

```cypher
// V001__Create_user_constraints.cypher
CREATE CONSTRAINT user_email_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.email IS UNIQUE;

CREATE CONSTRAINT user_id_exists IF NOT EXISTS
FOR (u:User) REQUIRE u.userId IS NOT NULL;

CREATE INDEX user_created IF NOT EXISTS
FOR (u:User) ON (u.createdAt);
```

### Java Migration Example

```java
public class V004__MigrateAddressToNode
        implements JavaBasedMigration {

    @Override
    public void apply(MigrationContext ctx) {
        try (var session = ctx.getSession()) {
            session.executeWrite(tx -> {
                tx.run("""
                    MATCH (u:User)
                    WHERE u.street IS NOT NULL
                    CREATE (a:Address {
                        street: u.street,
                        city: u.city,
                        zip: u.zip
                    })
                    CREATE (u)-[:LIVES_AT]->(a)
                    REMOVE u.street, u.city, u.zip
                    """);
                return null;
            });
        }
    }
}
```

## Constraint Management

### Constraint Types (Neo4j 5+)

| Constraint | Syntax | Purpose |
|-----------|--------|---------|
| Unique | `REQUIRE prop IS UNIQUE` | No duplicate values |
| Node key | `REQUIRE (a, b) IS NODE KEY` | Composite uniqueness + existence |
| Existence | `REQUIRE prop IS NOT NULL` | Property must exist |
| Type | `REQUIRE prop IS :: STRING` | Property type enforcement |

### Constraint Evolution

```cypher
// Step 1: Add new constraint
CREATE CONSTRAINT user_phone_unique IF NOT EXISTS
FOR (u:User) REQUIRE u.phone IS UNIQUE;

// Step 2: Backfill data
MATCH (u:User) WHERE u.phone IS NULL
SET u.phone = 'UNKNOWN_' + toString(id(u));

// Step 3: Add existence constraint
CREATE CONSTRAINT user_phone_exists IF NOT EXISTS
FOR (u:User) REQUIRE u.phone IS NOT NULL;
```

## APOC for Data Migrations

### Batch Processing

```cypher
// Process nodes in batches of 10000
CALL apoc.periodic.iterate(
    'MATCH (u:User) WHERE u.fullName IS NOT NULL RETURN u',
    'WITH u
     SET u.firstName = split(u.fullName, " ")[0],
         u.lastName = split(u.fullName, " ")[1]
     REMOVE u.fullName',
    {batchSize: 10000, parallel: false}
);
```

### Schema Refactoring with APOC

```cypher
// Rename a relationship type
CALL apoc.refactor.rename.type(
    'BOUGHT', 'PURCHASED'
);

// Rename a label
CALL apoc.refactor.rename.label(
    'Client', 'Customer'
);

// Rename a property
CALL apoc.refactor.rename.nodeProperty(
    'userName', 'name'
);
```

## Index Migration

| Index Type | Creation | Use Case |
|-----------|----------|----------|
| Range (default) | `CREATE INDEX ... ON (n.prop)` | Equality, range, prefix |
| Text | `CREATE TEXT INDEX ...` | String contains, suffix |
| Point | `CREATE POINT INDEX ...` | Geospatial queries |
| Full-text | `CREATE FULLTEXT INDEX ...` | Multi-property text search |
| Vector (5.13+) | `CREATE VECTOR INDEX ...` | Similarity search |

```cypher
-- Drop old index, create new type
DROP INDEX old_index_name IF EXISTS;
CREATE TEXT INDEX user_name_text IF NOT EXISTS
FOR (u:User) ON (u.name);
```

## Schema Versioning

Track schema version in the graph itself:

```cypher
MERGE (sv:SchemaVersion {current: true})
ON CREATE SET sv.version = 1, sv.appliedAt = datetime()
ON MATCH SET sv.version = sv.version + 1, sv.appliedAt = datetime();
```

## Rules

| Rule | Detail |
|------|--------|
| Idempotent | Use `IF NOT EXISTS` / `IF EXISTS` on all DDL |
| Versioned in Git | Migration files live alongside application code |
| Tested | Run migrations against test instance before production |
| Batch large changes | Use `CALL {} IN TRANSACTIONS OF N ROWS` or APOC batching |
| No data loss | NEVER drop constraints/indexes without confirming zero usage |
| Constraint order | Create constraints before loading data for best performance |
