---
name: x-dev-adr-automation
description: >
  Automates ADR generation from architecture plan mini-ADRs: extracts inline
  decisions, expands to full ADR format, assigns sequential numbering, updates
  the ADR index, and adds cross-references.
---

# Skill: ADR Automation

Automates the generation of Architecture Decision Records (ADRs) from mini-ADRs embedded in architecture plans. Extracts inline decisions, expands them to full ADR format using a standard template, assigns sequential numbering, updates the ADR index, and inserts cross-references between stories, architecture plans, and ADRs.

## When to Use

- After the architecture plan phase, when mini-ADRs exist inline in the plan document
- When `x-dev-architecture-plan` has produced an architecture plan containing `### ADR:` markers
- When architectural decisions need to be formally documented as standalone ADR files
- When the `docs/adr/` directory needs to be populated or updated with new decisions
- Do NOT use if ADRs have already been manually created for the same decisions

## Input Format

The skill reads mini-ADRs embedded in architecture plan documents. Each mini-ADR follows this structure:

```markdown
### ADR: [Title of the Decision]
- **Context:** Description of the problem or situation requiring a decision.
- **Decision:** The architectural decision that was made.
- **Rationale:** Justification for why this decision was chosen over alternatives.
```

The four fields of a mini-ADR are:

| Field | Required | Description |
|-------|----------|-------------|
| **title** | Yes | Short descriptive title of the architectural decision |
| **context** | Yes | Problem statement or situation that prompted the decision |
| **decision** | Yes | The specific decision that was made |
| **rationale** | Yes | Justification explaining why this decision was chosen |

The architecture plan path and story ID are provided as arguments:
- `architecture-plan-path`: Path to the architecture plan file (e.g., `docs/plans/architecture-plan-STORY-0004-0006.md`)
- `story-id`: The story identifier for cross-referencing (e.g., `story-0004-0006`)

## Output Format

Each mini-ADR is expanded into a full ADR file with YAML frontmatter and structured sections:

```markdown
---
status: Accepted
date: YYYY-MM-DD
story-ref: story-XXXX-YYYY
deciders: AI-assisted (via x-dev-adr-automation)
---

# ADR-NNNN: Title of the Decision

## Status

Accepted — YYYY-MM-DD

## Context

Expanded description of the problem or situation requiring a decision.
Provides background information and the forces at play.

## Decision

The architectural decision that was made, described in full detail.

## Consequences

### Positive
- Benefit 1 derived from this decision
- Benefit 2 derived from this decision

### Negative
- Trade-off or downside 1
- Trade-off or downside 2

### Neutral
- Observation that is neither positive nor negative
```

The output ADR frontmatter fields are:

| Field | Value | Description |
|-------|-------|-------------|
| `status` | `Accepted` | Initial status for auto-generated ADRs |
| `date` | Current date (`YYYY-MM-DD`) | Date of ADR generation |
| `story-ref` | Story ID from argument | Cross-reference to originating story |
| `deciders` | `AI-assisted (via x-dev-adr-automation)` | Attribution |

The output ADR sections are:

| Section | Source |
|---------|--------|
| **Status** | Set to `Accepted` with generation date |
| **Context** | Expanded from mini-ADR `context` field |
| **Decision** | Expanded from mini-ADR `decision` field |
| **Consequences** | Inferred from mini-ADR `rationale` field, split into Positive/Negative/Neutral |

## Algorithm

Follow these steps in order:

### Step 1: Parse Architecture Plan

1. Read the architecture plan file at `{architecture-plan-path}`
2. Search for `### ADR:` markers to identify mini-ADRs
3. For each marker, extract the four fields: title, context, decision, rationale
4. Build a list of mini-ADR objects to process

### Step 2: Scan Existing ADRs for Sequential Numbering

1. List all existing ADR files in `docs/adr/`
2. Extract the numeric prefix from each filename (e.g., `0003` from `ADR-0003-use-postgresql.md`)
3. Find the maximum number
4. The next ADR number starts at `max + 1`
5. If `docs/adr/` is empty or does not exist, start from `ADR-0001`

### Step 3: Check for Duplicates

For each mini-ADR, before creating a new ADR file:

1. Normalize the mini-ADR title (lowercase, strip punctuation, trim whitespace)
2. Compare against existing ADR titles in `docs/adr/`
3. If a match is found (title similarity), emit a warning and skip:
   ```
   WARNING: Duplicate ADR detected for "[title]", skipping — existing ADR found at docs/adr/ADR-NNNN-*.md
   ```
4. Do NOT overwrite existing ADRs

### Step 4: Expand Mini-ADR to Full ADR

For each non-duplicate mini-ADR:

1. Assign the next sequential number (padded to 4 digits: `ADR-0001` format)
2. Generate the full ADR content:
   - YAML frontmatter with `status`, `date`, `story-ref`, `deciders`
   - `## Status` section: `Accepted — {date}`
   - `## Context` section: expanded from mini-ADR context
   - `## Decision` section: expanded from mini-ADR decision
   - `## Consequences` section: inferred from rationale, split into Positive/Negative/Neutral
3. Convert the title to kebab-case for the filename

### Step 5: Write ADR Files

1. Ensure `docs/adr/` directory exists (create if needed)
2. Write each ADR to `docs/adr/ADR-NNNN-title-in-kebab-case.md`
3. Verify the file was written successfully

### Step 6: Update the Index

1. Open `docs/adr/README.md` (create if it does not exist)
2. If the file does not exist, create it with a table header:
   ```markdown
   # Architecture Decision Records

   | ADR | Title | Status | Date |
   |-----|-------|--------|------|
   ```
3. Append a new row for each generated ADR:
   ```markdown
   | [ADR-NNNN](ADR-NNNN-title-in-kebab-case.md) | Title of the Decision | Accepted | YYYY-MM-DD |
   ```

### Step 7: Add Cross-References

1. In each generated ADR frontmatter, ensure `story-ref: {story-id}` is present
2. Update the architecture plan file to add links to the generated ADRs:
   - After each `### ADR:` marker that was processed, add a reference:
     ```markdown
     > Generated: [ADR-NNNN](../../docs/adr/ADR-NNNN-title-in-kebab-case.md)
     ```
3. If a service architecture document exists (Section 7), update it with new ADR links

## Sequential Numbering

The sequential numbering algorithm ensures globally unique ADR identifiers:

1. **Scan**: Find all files matching `docs/adr/ADR-*.md`
2. **Extract**: Parse the 4-digit number from each filename (e.g., `ADR-0003-*.md` yields `3`)
3. **Maximum**: Find the maximum number among all existing ADRs
4. **Increment**: The next ADR number is `max + 1`
5. **Pad**: Format as 4-digit zero-padded string (`ADR-NNNN` format, e.g., `ADR-0004`)
6. **Empty directory**: If no ADR files exist, start numbering from `ADR-0001`

```
docs/adr/
  ADR-0001-use-postgresql.md
  ADR-0002-adopt-hexagonal-arch.md
  ADR-0003-event-driven-comms.md
  -> Next: ADR-0004
```

## Duplicate Detection

Before creating each ADR, check for existing ADRs with similar titles:

1. **Normalize** both the mini-ADR title and existing ADR titles:
   - Convert to lowercase
   - Remove punctuation and special characters
   - Trim leading/trailing whitespace
   - Collapse multiple spaces into one
2. **Compare** the normalized mini-ADR title against all normalized existing titles
3. **If duplicate found**:
   - Emit warning: `Duplicate ADR detected, skipping: "[original title]"`
   - Skip this mini-ADR (do not create a file)
   - Continue processing remaining mini-ADRs
4. **If no duplicate**: proceed with ADR creation

This prevents accidental overwriting of manually curated ADRs and avoids redundant documentation.

## Cross-Reference Rules

Cross-references create bidirectional traceability between stories, architecture plans, and ADRs:

### ADR to Story (in ADR frontmatter)

Each generated ADR includes a `story-ref` field in its YAML frontmatter:

```yaml
story-ref: story-0004-0006
```

### ADR to Architecture Plan (in ADR Context section)

The Context section of each ADR references the originating story and architecture plan:

```markdown
This decision was identified during the architecture planning phase for story-0004-0006.
```

### Architecture Plan to ADR (in plan document)

After processing, the architecture plan is updated with links to generated ADRs:

```markdown
### ADR: Use PostgreSQL for Persistence
- **Context:** ...
- **Decision:** ...
- **Rationale:** ...
> Generated: [ADR-0004](../../docs/adr/ADR-0004-use-postgresql-for-persistence.md)
```

### Service Architecture Document (Section 7)

If a service architecture document exists with a Section 7 (ADR references), update it with links to newly generated ADRs.

## Index Update

The ADR index at `docs/adr/README.md` serves as the master list of all ADRs:

1. **Read** `docs/adr/README.md` (or create if missing)
2. **Locate** the markdown table (identified by `| ADR |` header row)
3. **Append** one row per generated ADR:
   ```
   | [ADR-NNNN](ADR-NNNN-title-in-kebab-case.md) | Title | Accepted | YYYY-MM-DD |
   ```
4. **Preserve** existing table rows (never remove or reorder)
5. **Sort** is not required — append in generation order (sequential numbering guarantees chronological order)

## Examples

### Example: Mini-ADR Input (from architecture plan)

```markdown
### ADR: Use PostgreSQL for Persistence
- **Context:** The system requires a relational database for transactional data with ACID guarantees. The team has experience with PostgreSQL and it supports JSON columns for semi-structured data.
- **Decision:** Use PostgreSQL 15+ as the primary relational database for all transactional persistence.
- **Rationale:** PostgreSQL provides strong ACID compliance, excellent JSON support via jsonb columns, mature tooling, and the team has operational experience. Alternatives like MySQL lack native JSON indexing; NoSQL options like MongoDB sacrifice transactional consistency.
```

### Example: Full ADR Output (generated)

File: `docs/adr/ADR-0004-use-postgresql-for-persistence.md`

```markdown
---
status: Accepted
date: 2024-01-15
story-ref: story-0004-0006
deciders: AI-assisted (via x-dev-adr-automation)
---

# ADR-0004: Use PostgreSQL for Persistence

## Status

Accepted — 2024-01-15

## Context

The system requires a relational database for transactional data with ACID guarantees. The team has experience with PostgreSQL and it supports JSON columns for semi-structured data.

This decision was identified during the architecture planning phase for story-0004-0006.

## Decision

Use PostgreSQL 15+ as the primary relational database for all transactional persistence.

## Consequences

### Positive
- Strong ACID compliance ensures data integrity for transactional workloads
- Native jsonb column support enables flexible schema for semi-structured data
- Mature ecosystem with excellent tooling (pgAdmin, pg_dump, extensions)
- Team has existing operational experience, reducing ramp-up time

### Negative
- Vertical scaling limits may require sharding for very high write throughput
- Operational overhead for managing replication and backups
- Less suitable for graph-like query patterns compared to specialized databases

### Neutral
- Requires standard database migration tooling (Flyway, Liquibase, or similar)
- Connection pooling (e.g., PgBouncer) recommended for high-concurrency scenarios
```

### Example: Index Entry (appended to docs/adr/README.md)

```markdown
| [ADR-0004](ADR-0004-use-postgresql-for-persistence.md) | Use PostgreSQL for Persistence | Accepted | 2024-01-15 |
```

### Example: Architecture Plan Cross-Reference (updated in plan)

```markdown
### ADR: Use PostgreSQL for Persistence
- **Context:** The system requires a relational database...
- **Decision:** Use PostgreSQL 15+...
- **Rationale:** PostgreSQL provides strong ACID compliance...
> Generated: [ADR-0004](../../docs/adr/ADR-0004-use-postgresql-for-persistence.md)
```

## Detailed References

For in-depth guidance on related patterns, consult:
- `.github/skills/x-dev-adr-automation/SKILL.md`
- `.github/skills/architecture/SKILL.md`
- `.github/skills/coding-standards/SKILL.md`
