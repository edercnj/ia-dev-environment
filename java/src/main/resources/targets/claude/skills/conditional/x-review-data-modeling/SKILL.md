---
name: x-review-data-modeling
description: "Data modeling specialist review: validates entity design, aggregate boundaries, value objects, repository patterns, domain event persistence, and DDD tactical patterns."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Data Modeling Specialist Review

## Purpose

Review code changes for data modeling best practices in DDD/hexagonal architectures: entity identity and lifecycle, aggregate boundaries and consistency, value object immutability, repository pattern compliance, domain event persistence, and mapping between domain and persistence models.

## Activation Condition

Include this skill when `database != "none"` AND `architecture.style` is one of `hexagonal`, `ddd`, `cqrs`, or `clean` in the project configuration.

## When to Use

- Pre-PR quality validation for domain model changes
- Reviewing entity and aggregate design
- Checking domain-persistence mapping
- Validating DDD tactical patterns

## Triggers

- `/x-review-data-modeling 42` -- review PR #42 for data modeling
- `/x-review-data-modeling src/main/java/com/example/domain/` -- review domain model files
- `/x-review-data-modeling` -- review all current domain model changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| data-modeling | `skills/data-modeling/SKILL.md` | Entity design, aggregate boundaries, value objects, DDD tactical patterns |

## Checklist (10 Items, Max Score: /20)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Entity & Identity (DM-01 to DM-03)

| # | Item | Score |
|---|------|-------|
| DM-01 | Entities have explicit identity (typed ID, not raw Long/String) | /2 |
| DM-02 | Entity equality based on identity, not attribute comparison | /2 |
| DM-03 | Entity lifecycle managed through aggregate root (no orphan entity creation) | /2 |

### Aggregate Design (DM-04 to DM-06)

| # | Item | Score |
|---|------|-------|
| DM-04 | Aggregate boundaries defined (one aggregate = one consistency boundary) | /2 |
| DM-05 | Cross-aggregate references use IDs, not direct object references | /2 |
| DM-06 | Aggregate invariants enforced in domain layer (not in persistence layer) | /2 |

### Value Objects & Mapping (DM-07 to DM-09)

| # | Item | Score |
|---|------|-------|
| DM-07 | Value objects are immutable (records or final classes with no setters) | /2 |
| DM-08 | Domain model separated from persistence model (dedicated mapper classes) | /2 |
| DM-09 | Repository returns domain entities (not persistence entities or DTOs) | /2 |

### Domain Events (DM-10)

| # | Item | Score |
|---|------|-------|
| DM-10 | Domain events published through aggregate root (not scattered across services) | /2 |

## Workflow

### Step 1 -- Gather Context

Read the data modeling knowledge pack:
- `skills/data-modeling/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: domain model, entities, value objects, repositories, mappers.

### Step 3 -- Entity & Identity Review

Check identity patterns, equality implementation, lifecycle management.

### Step 4 -- Aggregate Review

Verify aggregate boundaries, cross-aggregate references, invariant enforcement.

### Step 5 -- Value Object & Mapping Review

Check immutability, domain-persistence separation, repository return types.

### Step 6 -- Domain Event Review

Verify event publishing patterns through aggregate roots.

### Step 7 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: Data Modeling
STORY: [story-id or change description]
SCORE: XX/20

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [DM-XX] [Item description]

### FAILED
- [DM-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [DM-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No domain model found | Report INFO: no domain model code discovered |
| No aggregate pattern detected | Warn and check if architecture uses aggregates |
| No persistence model found | Skip DM-08, DM-09 and note N/A |
