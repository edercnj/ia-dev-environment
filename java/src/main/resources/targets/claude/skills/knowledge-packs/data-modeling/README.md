# data-modeling

> Cross-cutting data modeling patterns: schema design (soft delete, temporal tables, audit trails, multi-tenant, SCD), concurrency (optimistic/pessimistic locking, Saga), and test data management (factories, fixtures, anonymization).

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-task-implement, x-arch-plan, x-review (Database specialist), architect agent |
| **Condition** | Included when `database` is not `none` or `cache` is not `none` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Schema design patterns: soft delete, temporal tables, audit trails, multi-tenant, Slowly Changing Dimensions (SCD Types 1/2/3)
- Concurrency patterns: optimistic locking, pessimistic locking, Saga pattern, distributed locks
- Test data patterns: factories, fixtures, data builders, anonymization, database seeding
- Database ADR templates: SQL vs NoSQL, embedded vs referenced, partitioning, caching, read replicas, distributed transactions
- Anti-patterns and quick decision guides

## Key Concepts

This pack provides advanced cross-cutting data modeling patterns that apply regardless of the specific database technology. Schema design patterns address common requirements like recoverable deletion (soft delete), historical tracking (temporal tables), compliance logging (audit trails), and SaaS data isolation (multi-tenant). Concurrency patterns guide the selection between optimistic locking for low-contention reads and pessimistic locking for high-contention critical sections, with the Saga pattern for distributed transactions. Test data management ensures safe, repeatable test data through factories and builders while protecting production data through anonymization.

## See Also

- [database-patterns](../database-patterns/) — Database-specific conventions, types, migrations, indexing
- [data-management](../data-management/) — Zero-downtime migrations, data governance, backup/restore
- [compliance](../compliance/) — Audit trail requirements and data classification enforcement
