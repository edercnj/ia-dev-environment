# data-management

> Data management lifecycle patterns: zero-downtime migrations, expand/contract pattern, schema versioning, data governance, backup/restore strategies, partitioning, CDC, and data quality validation.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-story-implement, x-review (Database specialist), architect agent |
| **Condition** | Included when `database` is not `none` or `cache` is not `none` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Zero-downtime migrations with the expand/contract pattern
- Schema versioning: timestamp-based numbering, idempotent migrations, environment-specific migrations
- Data governance: data classification (PUBLIC/INTERNAL/CONFIDENTIAL/RESTRICTED), retention policies, data lineage, PII handling
- Backup and restore: full/incremental/differential backups, RTO/RPO targets, verification testing
- Partitioning strategies: horizontal (range, hash, list), sharding keys, rebalancing
- Change Data Capture (CDC): log-based, trigger-based, timestamp-based patterns
- Data quality: validation rules, profiling metrics, anomaly detection

## Key Concepts

This pack covers the complete data management lifecycle from schema evolution through data retirement. The expand/contract pattern enables zero-downtime schema changes by separating DDL, DML, and cleanup into distinct migration phases. Data governance enforces a four-level classification scheme with corresponding access controls and encryption requirements. Backup strategies are selected based on RTO/RPO targets with mandatory periodic restore drills. CDC patterns (particularly log-based with outbox integration) enable reliable event publishing with at-least-once delivery guarantees.

## See Also

- [database-patterns](../database-patterns/) — Database-specific conventions, indexing, query optimization
- [data-modeling](../data-modeling/) — Schema design patterns, concurrency control, test data management
- [compliance](../compliance/) — Regulatory retention requirements and PII handling rules
