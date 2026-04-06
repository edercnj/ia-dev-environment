---
name: data-modeling
description: "Cross-cutting data modeling patterns: schema design (soft delete, temporal tables, audit trails, multi-tenant, SCD), concurrency (optimistic/pessimistic locking, Saga), and test data management (factories, fixtures, anonymization)."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Data Modeling

## Purpose

Provides advanced cross-cutting data modeling patterns for projects using {{DB_TYPE}}. Covers schema design patterns (soft delete, temporal tables, audit trails, multi-tenant, Slowly Changing Dimensions), concurrency control patterns (optimistic locking, pessimistic locking, Saga), and test data management (factories, fixtures, anonymization, data builders).

> **Prerequisite knowledge:** This pack assumes familiarity with database fundamentals (ACID properties, normalization, CAP considerations) covered in `knowledge/core/11-database-principles.md`. It does NOT duplicate those foundations.

> **Complementary pack:** For database-specific conventions (types, migrations, indexing, caching), see the `database-patterns` knowledge pack.

## Condition

This knowledge pack is included when `database != "none"` or `cache != "none"`.

## How to Use

Read the relevant reference files from the `references/` directory within this skill's folder.

## Schema Design Patterns

Advanced schema design patterns beyond basic table structure. Each pattern includes when to use, trade-offs, and implementation considerations for {{DB_TYPE}}.

| Pattern | Use Case | Reference |
|---------|----------|-----------|
| Soft Delete | Recoverable deletion, audit compliance | `references/schema-design-patterns.md` |
| Temporal Tables | Historical data tracking, point-in-time queries | `references/schema-design-patterns.md` |
| Audit Trails | Compliance logging, change tracking | `references/schema-design-patterns.md` |
| Multi-Tenant | SaaS data isolation strategies | `references/schema-design-patterns.md` |
| SCD Types 1/2/3 | Dimension history in analytical models | `references/schema-design-patterns.md` |

## Concurrency Patterns

Patterns for managing concurrent data access across distributed and single-node systems.

| Pattern | Use Case | Reference |
|---------|----------|-----------|
| Optimistic Locking | Low-contention reads with version checks | `references/concurrency-patterns.md` |
| Pessimistic Locking | High-contention critical sections | `references/concurrency-patterns.md` |
| Saga Pattern | Distributed transaction coordination | `references/concurrency-patterns.md` |
| Distributed Locks | Cross-service mutual exclusion | `references/concurrency-patterns.md` |

## Test Data Patterns

Strategies for creating, managing, and protecting test data across environments.

| Pattern | Use Case | Reference |
|---------|----------|-----------|
| Test Data Factories | Programmatic test data generation | `references/test-data-patterns.md` |
| Fixtures | Reusable predefined datasets | `references/test-data-patterns.md` |
| Data Builders | Fluent API for complex test objects | `references/test-data-patterns.md` |
| Anonymization | Production data sanitization | `references/test-data-patterns.md` |
| Database Seeding | Environment initialization | `references/test-data-patterns.md` |

## Quick Decision Guide

### When to Use Each Schema Pattern

- **Need recoverable deletes?** Use Soft Delete
- **Need historical queries at a point in time?** Use Temporal Tables
- **Need compliance audit logging?** Use Audit Trails
- **Need data isolation per customer?** Use Multi-Tenant
- **Need dimension history for analytics?** Use SCD Types

### When to Use Each Concurrency Pattern

- **Low contention, high read throughput?** Use Optimistic Locking
- **High contention, critical data integrity?** Use Pessimistic Locking
- **Spanning multiple services/databases?** Use Saga Pattern
- **Need cross-service mutex?** Use Distributed Locks

## Database ADR Templates

Structured Architecture Decision Record templates for common database decisions. Use these when documenting technology choices.

| Template | Decision Topic | Reference |
|----------|---------------|-----------|
| SQL vs NoSQL | Relational vs non-relational selection | `references/database-adr-templates.md` |
| Embedded vs Referenced | Document DB data model strategy | `references/database-adr-templates.md` |
| Partitioning/Sharding | Partition strategy selection | `references/database-adr-templates.md` |
| Caching Layer | Cache topology and strategy | `references/database-adr-templates.md` |
| Read Replica Topology | Replication strategy selection | `references/database-adr-templates.md` |
| Distributed Transactions | 2PC vs Saga vs Outbox | `references/database-adr-templates.md` |

## Anti-Patterns

- Implementing soft delete without filtering in all queries
- Using pessimistic locking for read-heavy workloads
- Storing audit data in the same table as operational data
- Using distributed locks without TTL (risk of deadlock)
- Generating test data with production PII
- Hardcoding test data values instead of using factories
- Mixing tenant data without row-level security enforcement

## Detailed References

| Reference | Content |
|-----------|---------|
| `references/schema-design-patterns.md` | Soft delete, temporal tables, audit trails, multi-tenant schemas, SCD Types 1/2/3 |
| `references/concurrency-patterns.md` | Optimistic locking, pessimistic locking, Saga pattern, distributed locks |
| `references/test-data-patterns.md` | Factories, fixtures, data builders, anonymization, database seeding |
| `references/database-adr-templates.md` | ADR templates: SQL vs NoSQL, embedded vs referenced, partitioning, caching, read replicas, distributed transactions |
