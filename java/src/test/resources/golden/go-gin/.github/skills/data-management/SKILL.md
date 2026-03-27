---
name: data-management
description: >
  Knowledge Pack: Data Management -- Zero-downtime migrations, expand/contract pattern,
  schema versioning, data governance, backup/restore, partitioning, CDC, and data quality
  validation for my-go-service.
---

# Knowledge Pack: Data Management

## Summary

Data management lifecycle patterns for my-go-service using go 1.22 with gin.

### Zero-Downtime Migrations

- Expand/contract pattern: add new -> migrate data -> remove old
- Backward-compatible changes: add column (nullable/default), add table, add index
- Dangerous changes: rename column, change type, drop column (require expand/contract)
- Migration ordering: schema first, then data, then cleanup
- Rollback strategies: reverse migration script, dual-write during transition

### Schema Versioning

- Timestamp-based naming: V20260326_001__description.sql (recommended for teams)
- Idempotent migrations: IF NOT EXISTS, CREATE OR REPLACE
- Test migrations against production-like data volumes
- Never modify already-applied migrations

### Data Governance

- Classification: PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED
- Retention policies: regulatory (GDPR, HIPAA, SOX), automated archival
- Data lineage: source -> transformation -> destination tracking
- PII handling: encrypt at rest, mask in logs, audit trail, DSAR capability

### Backup & Restore

- Backup types: full (complete), incremental (since last), differential (since last full)
- RTO/RPO targets: critical (< 1h / < 15min), standard (< 4h / < 1h)
- Verification: monthly restore drills, integrity checks, PITR testing
- Restore: point-in-time recovery, selective restore, cross-region DR

### Partitioning Strategies

- Range: time-series data (partition by month/year)
- Hash: even distribution (partition by user_id hash)
- List: categorical data (partition by region/status)
- Partition when > 100M rows or > 100GB; do not partition small tables

### Change Data Capture (CDC)

- Log-based (Debezium): near real-time, minimal database impact
- Outbox pattern: transactional outbox -> CDC -> event bus, at-least-once delivery
- Avoid dual-write: use CDC or event sourcing, not both for same data
- CDC as bridge during event sourcing migration

### Data Quality

- Validation: schema, referential integrity, business rules, uniqueness
- Profiling: completeness (> 99%), uniqueness (100% for unique fields), consistency
- Anomaly detection: statistical outliers, data drift, volume anomalies
- Quality metrics: accuracy, completeness, timeliness, consistency

## References

- `references/migration-safety-checklist.md` -- Pre-flight checklist for production migrations
- `references/backup-strategy-matrix.md` -- Backup type x DB type x RTO/RPO matrix
- `references/partitioning-decision-tree.md` -- Flowchart for selecting partition strategy
