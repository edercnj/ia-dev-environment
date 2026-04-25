# Rule 09 — Data Management

> **Full reference:** Read `knowledge/database-patterns/index.md` for detailed database patterns and migration strategies.

## Database: {DATABASE_NAME}

## Migration Strategy (Non-Negotiable)

| Requirement | Detail |
|-------------|--------|
| Direction | Forward-only in production (no rollback migrations) |
| Pattern | Expand/contract for breaking schema changes |
| Tool | Managed by project migration tool |
| Naming | Sequential versioned files: `V001__description.sql` |
| Review | Every migration MUST be peer-reviewed before merge |

- Migrations MUST be idempotent when possible
- Destructive operations (DROP, DELETE) require explicit approval
- Data backfill migrations MUST be separate from schema migrations

## Expand/Contract Pattern

| Phase | Action | Backward Compatible |
|-------|--------|---------------------|
| Expand | Add new column/table alongside old | Yes |
| Migrate | Copy/transform data to new structure | Yes |
| Contract | Remove old column/table | No (next release) |

- Breaking schema changes MUST follow expand/contract across at least 2 releases
- NEVER rename or remove columns in a single migration

## Backup and Recovery

| Requirement | Detail |
|-------------|--------|
| Backup frequency | Defined per environment (production: daily minimum) |
| Backup verification | Regular restore tests (at least monthly) |
| Point-in-time recovery | Required for production databases |
| Retention policy | Defined and documented per environment |

## Data Classification (Non-Negotiable)

| Classification | Examples | Handling |
|----------------|----------|----------|
| Public | API docs, changelogs | No restrictions |
| Internal | Logs, metrics | Access-controlled |
| Confidential | User emails, addresses | Encrypted at rest and in transit |
| Restricted | Passwords, tokens, PII | Encrypted, audited, minimal retention |

- Every data field MUST have a classification
- Restricted data MUST have defined retention and deletion policies
- Data at rest MUST be encrypted for Confidential and Restricted classifications

## Query Performance

| Requirement | Detail |
|-------------|--------|
| Indexes | Required for all WHERE, JOIN, and ORDER BY columns |
| N+1 queries | Forbidden — use batch/join queries |
| Connection pooling | Required for all database access |
| Query timeout | Configurable per query type (default: 5s) |

## Forbidden

- Running DDL statements directly in production
- Migrations that mix schema changes with data manipulation
- Storing sensitive data without encryption
- Database connections without pooling
- Queries without timeout configuration
- Orphaned data (foreign key references to deleted records)

> Read `knowledge/database-patterns/index.md` for ORM patterns, connection pool tuning, and query optimization.
