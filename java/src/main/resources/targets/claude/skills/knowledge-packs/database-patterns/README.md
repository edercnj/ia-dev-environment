# database-patterns

> Database conventions: schema design, migrations, indexing, query optimization, caching patterns. References loaded based on project database and cache configuration.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-lifecycle, x-review (Database specialist), x-review-pr, x-codebase-audit, architect agent |
| **Condition** | Included when `database` is not `none` or `cache` is not `none` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Universal schema naming conventions and mandatory columns
- Database-specific references: SQL (PostgreSQL, Oracle, MySQL), NoSQL (MongoDB, Cassandra), Graph (Neo4j), Columnar (ClickHouse), NewSQL (YugabyteDB, CockroachDB), Time-series (TimescaleDB), Search (Elasticsearch)
- Cache patterns: cache-aside, TTL, key naming conventions
- Connection pool sizing, timeout configuration, and monitoring metrics
- Index management: creation strategy, unused index detection, partial/covering indexes, maintenance
- Data governance: classification enforcement, retention automation, audit trails, data masking
- Backup strategy patterns with point-in-time recovery

## Key Concepts

This pack provides database-technology-aware conventions covering the full spectrum from SQL to NoSQL, graph, columnar, NewSQL, time-series, and search engines. Universal rules enforce schema naming (snake_case, descriptive index names), mandatory audit columns (created_at, updated_at), data security (hashed passwords, masked PANs, no stored PINs), and parameterized queries. Connection pool sizing follows database-specific formulas with framework-aware configuration. Index management emphasizes measuring before creating, detecting unused indexes, and using partial/covering indexes for optimization. Anti-patterns are explicitly documented to prevent common mistakes like using FLOAT for monetary values or caching without TTL.

## See Also

- [data-modeling](../data-modeling/) — Schema design patterns, concurrency control, test data management
- [data-management](../data-management/) — Zero-downtime migrations, data governance, backup/restore
- [architecture-patterns](../architecture-patterns/) — Repository pattern, unit of work, cache-aside pattern
