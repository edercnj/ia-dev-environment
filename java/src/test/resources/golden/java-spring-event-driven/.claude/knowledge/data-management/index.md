---
name: data-management
description: "Data management lifecycle patterns: zero-downtime migrations, expand/contract pattern, schema versioning, data governance, backup/restore strategies, partitioning, CDC, and data quality validation for {{DATABASE_TYPE}} with {{MIGRATION_TOOL}}."
---

# Knowledge Pack: Data Management

## Purpose

Provides comprehensive data management lifecycle patterns for {{LANGUAGE}} projects using {{DATABASE_TYPE}} with {{MIGRATION_TOOL}}. Covers zero-downtime migrations, expand/contract pattern, schema versioning, data governance, backup/restore strategies, partitioning, Change Data Capture (CDC), and data quality validation.

## Quick Reference (always in context)

See `references/migration-safety-checklist.md` for pre-flight checklist, `references/backup-strategy-matrix.md` for backup type decisions, and `references/partitioning-decision-tree.md` for partition strategy selection.

## Detailed References

Read these files for comprehensive data management guidance:

| Reference | Content |
|-----------|---------|
| `references/migration-safety-checklist.md` | Pre-flight checklist for production migrations: schema review, backup verification, rollback plan, monitoring setup, communication plan |
| `references/backup-strategy-matrix.md` | Backup type x database type x RTO/RPO matrix with recommended strategies per environment |
| `references/partitioning-decision-tree.md` | Decision flowchart: data volume x query patterns x growth rate leading to partition strategy recommendation |

## Zero-Downtime Migrations

### Expand/Contract Pattern

The expand/contract pattern enables schema changes without downtime:

1. **EXPAND** -- Add new column (nullable or with default), create new table, add new index
2. **MIGRATE** -- Copy data from old structure to new, dual-write during transition
3. **CONTRACT** -- Remove old structure after all consumers migrated

### Backward-Compatible Changes (Safe)

- Add column with nullable or default value
- Add new table or index
- Add new enum value (append only)
- Widen column type (e.g., VARCHAR(50) to VARCHAR(100))

### Dangerous Changes (Require Expand/Contract)

- Rename column or table
- Change column type (narrowing)
- Drop column or table
- Remove enum value
- Add NOT NULL constraint to existing column

### Migration Ordering

1. Schema changes first (DDL)
2. Data migration (DML)
3. Cleanup (drop old structures)
4. Each phase in a separate migration file

### Rollback Strategies

- Reverse migration script for every forward migration
- Dual-write during transition enables instant rollback
- Feature flags to switch read/write targets
- Blue-green database pattern for major changes

## Schema Versioning

### Migration Numbering

- Timestamp-based: `V20260326_001__description.sql` (recommended for teams)
- Sequential: `V001__description.sql` (simpler, risk of conflicts)
- Always use descriptive names: `V20260326_001__add_user_email_column.sql`

### Idempotent Migrations

- Use `IF NOT EXISTS` for CREATE statements
- Use `CREATE OR REPLACE` where supported
- Guard DROP statements with existence checks
- Test idempotency by running migration twice

### Migration Testing

- Test migrations against production-like data volumes
- Verify rollback scripts work correctly
- Measure migration execution time on representative data
- Test concurrent access during migration

### Environment-Specific Migrations

- Seed data for development (realistic but anonymized)
- Test fixtures for staging (edge cases, boundary values)
- Never include test data in production migrations
- Use repeatable migrations for reference data

## Data Governance

### Data Classification

| Level | Description | Examples | Handling |
|-------|-------------|----------|----------|
| PUBLIC | No restrictions | Product catalog, public APIs | Standard storage |
| INTERNAL | Internal use only | Employee directories, metrics | Access control |
| CONFIDENTIAL | Business-sensitive | Financial data, contracts | Encryption at rest |
| RESTRICTED | Highest sensitivity | PII, credentials, health data | Encryption + audit trail |

### Retention Policies

- Define retention period per data classification
- Regulatory requirements: GDPR (right to erasure), HIPAA (6 years), SOX (7 years)
- Implement automated archival for expired data
- Soft delete with scheduled hard delete after retention period

### Data Lineage

- Track source system, transformation, and destination
- Document data flow: ingestion, processing, storage, consumption
- Maintain lineage metadata for audit and debugging
- Version transformations alongside schema changes

### PII Handling

- Encrypt PII at rest (AES-256 minimum)
- Mask PII in application logs (show only last 4 characters)
- Maintain access audit trail for PII reads
- Implement data subject access requests (DSAR) capability
- Anonymize PII in non-production environments

## Backup & Restore

### Backup Types

| Type | Description | RTO | Storage Cost |
|------|-------------|-----|--------------|
| Full | Complete database snapshot | Fastest restore | Highest |
| Incremental | Changes since last backup | Medium restore | Lowest |
| Differential | Changes since last full backup | Medium restore | Medium |

### RTO/RPO Targets

- **RTO** (Recovery Time Objective): Maximum acceptable downtime
- **RPO** (Recovery Point Objective): Maximum acceptable data loss
- Critical systems: RTO < 1 hour, RPO < 15 minutes
- Standard systems: RTO < 4 hours, RPO < 1 hour

### Verification Testing

- Periodic restore drills (monthly minimum)
- Verify data integrity after restore (checksums, row counts)
- Test point-in-time recovery to specific timestamps
- Document restore procedures with step-by-step runbooks

### Restore Procedures

- Point-in-time recovery using WAL/binlog replay
- Selective restore for specific tables or schemas
- Cross-region restore for disaster recovery
- Parallel restore for large databases

## Partitioning Strategies

### Horizontal Partitioning

| Strategy | Best For | Example |
|----------|----------|---------|
| Range | Time-series data | Partition by month/year |
| Hash | Even distribution | Partition by user_id hash |
| List | Categorical data | Partition by region/status |

### Sharding Keys

- Choose high-cardinality columns for even distribution
- Align shard key with most frequent query patterns
- Avoid hotspots (e.g., sequential IDs, timestamps)
- Consider composite shard keys for complex access patterns

### When to Partition

- Table exceeds 100M rows or 100GB
- Query performance degrades despite proper indexing
- Maintenance operations (VACUUM, REINDEX) take too long
- Need to archive old data while keeping recent data fast

### Rebalancing

- Online rebalancing with minimal impact on reads
- Consistent hashing for predictable key distribution
- Monitor partition sizes and split when skewed
- Plan rebalancing during low-traffic windows

## Change Data Capture (CDC)

### CDC Patterns

| Pattern | Mechanism | Latency | Impact |
|---------|-----------|---------|--------|
| Log-based | Database WAL/binlog | Near real-time | Minimal |
| Trigger-based | Database triggers | Real-time | Medium |
| Timestamp-based | Polling changed rows | Configurable | Low |

### Outbox Pattern Integration

1. Write domain event to outbox table in same transaction as entity
2. CDC reads outbox table changes from WAL
3. CDC publishes events to message broker
4. Guarantees at-least-once delivery with exactly-once semantics

### Event Sourcing Alignment

- CDC complements event store by capturing state changes
- Use CDC for integrating legacy systems with event-driven architecture
- Avoid dual-write: use CDC or event sourcing, not both for same data
- CDC as bridge during event sourcing migration

## Data Quality

### Validation Rules

- Schema validation: column types, constraints, NOT NULL
- Referential integrity: foreign keys, orphan detection
- Business rules: value ranges, format patterns, cross-field consistency
- Uniqueness: duplicate detection, deduplication strategies

### Data Profiling Metrics

| Metric | Description | Target |
|--------|-------------|--------|
| Completeness | Non-null values / total values | > 99% for required fields |
| Uniqueness | Distinct values / total values | 100% for unique fields |
| Consistency | Values matching expected format | > 99.9% |
| Timeliness | Data freshness vs SLA | Within SLA window |

### Anomaly Detection

- Statistical outliers: values beyond 3 standard deviations
- Data drift: schema changes, value distribution shifts
- Volume anomalies: unexpected row count changes
- Freshness alerts: stale data beyond expected update window

## Migration Tool Patterns

### {{LANGUAGE}} with {{MIGRATION_TOOL}}

Follow the migration tool conventions specific to your project stack. Key principles:

- One migration per logical change
- Never modify already-applied migrations
- Test migrations in CI before production
- Keep migrations small and focused
- Include rollback script for every migration

## Related Knowledge Packs

- `skills/compliance/` — data classification, PII handling, and retention policies
- `skills/disaster-recovery/` — backup/restore strategies and RPO/RTO targets
- `skills/infrastructure/` — database container configuration and resource management
