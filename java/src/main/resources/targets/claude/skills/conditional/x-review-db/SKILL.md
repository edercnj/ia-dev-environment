---
name: x-review-db
description: "Database specialist review: validates schema design, migration safety, query optimization, connection management, transaction boundaries, and data integrity patterns."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Database Specialist Review

## Purpose

Review code changes for database best practices: schema design, migration safety, query optimization, connection pool management, transaction boundaries, data integrity constraints, index strategy, and zero-downtime migration patterns.

## Activation Condition

Include this skill when `database != "none"` in the project configuration.

## When to Use

- Pre-PR quality validation for database changes
- Reviewing migration scripts
- Checking query performance patterns
- Validating transaction boundaries

## Triggers

- `/x-review-db 42` -- review PR #42 for database patterns
- `/x-review-db src/main/resources/db/migration/` -- review migration files
- `/x-review-db` -- review all current database changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| database-patterns | `skills/database-patterns/SKILL.md` | Schema design, migration patterns, query optimization |

## Checklist (20 Items, Max Score: /40)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Schema Design (DB-01 to DB-05)

| # | Item | Score |
|---|------|-------|
| DB-01 | Tables have primary keys (no heap tables) | /2 |
| DB-02 | Foreign keys defined for referential integrity | /2 |
| DB-03 | Column types appropriate (no VARCHAR for dates, no TEXT for short strings) | /2 |
| DB-04 | NOT NULL constraints on required columns | /2 |
| DB-05 | Naming convention followed (snake_case, singular table names, descriptive columns) | /2 |

### Migration Safety (DB-06 to DB-10)

| # | Item | Score |
|---|------|-------|
| DB-06 | Migration is reversible (rollback script provided) | /2 |
| DB-07 | Zero-downtime migration (expand/contract pattern for breaking changes) | /2 |
| DB-08 | Migration tested against production-like data volume | /2 |
| DB-09 | No data loss in migration (data preserved or migrated) | /2 |
| DB-10 | Migration idempotent (safe to re-run) | /2 |

### Query Optimization (DB-11 to DB-14)

| # | Item | Score |
|---|------|-------|
| DB-11 | Indexes exist for columns used in WHERE, JOIN, ORDER BY | /2 |
| DB-12 | No SELECT * (explicit column selection) | /2 |
| DB-13 | Pagination for large result sets (LIMIT/OFFSET or cursor-based) | /2 |
| DB-14 | No N+1 queries (batch fetching or JOIN used) | /2 |

### Connection & Transaction Management (DB-15 to DB-18)

| # | Item | Score |
|---|------|-------|
| DB-15 | Connection pool configured with appropriate size | /2 |
| DB-16 | Transaction boundaries at use case level (not repository) | /2 |
| DB-17 | Read-only transactions for queries (no unnecessary write locks) | /2 |
| DB-18 | Connection timeout and validation query configured | /2 |

### Data Integrity (DB-19 to DB-20)

| # | Item | Score |
|---|------|-------|
| DB-19 | Unique constraints for business-unique fields | /2 |
| DB-20 | Soft delete or audit trail for critical data (no hard delete without justification) | /2 |

## Workflow

### Step 1 -- Gather Context

Read the database patterns knowledge pack:
- `skills/database-patterns/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: migration files, entity classes, repository classes, configuration.

### Step 3 -- Schema Review

Check table design, constraints, naming conventions.

### Step 4 -- Migration Safety Review

Verify reversibility, zero-downtime compatibility, data preservation.

### Step 5 -- Query Performance Review

Check for N+1, missing indexes, unbounded queries.

### Step 6 -- Transaction Review

Verify transaction boundaries, read-only flags, connection management.

### Step 7 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: Database
STORY: [story-id or change description]
SCORE: XX/40

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [DB-XX] [Item description]

### FAILED
- [DB-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [DB-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No database code found | Report INFO: no database code discovered |
| No migration files found | Skip DB-06 to DB-10 and note N/A |
| Database type not recognized | Warn and proceed with generic patterns |
