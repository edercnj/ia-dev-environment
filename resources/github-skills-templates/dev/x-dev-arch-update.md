---
name: x-dev-arch-update
description: >
  Incrementally updates the service architecture document with changes from
  architecture plans. Adds new components, integrations, flows, and ADR references
  without rewriting existing content. Use after implementation to keep architecture
  documentation current.
---

# Skill: Service Architecture Document Updater (Incremental)

## When to Use

- After a feature is implemented and an architecture plan exists
- When Phase 3 (Documentation) of x-dev-lifecycle is executing
- Standalone: `/x-dev-arch-update [STORY-ID]`
- SKIP when: no architecture plan exists for the feature

## Incremental Update Rules (RULE-008)

### Content Preservation

- NEVER remove existing content from any section
- NEVER rewrite a section — only insert or append
- If a component was refactored: mark old as deprecated, add new
- If a flow changed: add new version, mark previous as "v1"
- Merge conflicts: preserve both versions

### Section Update Protocol

For each of the 10 sections in `service-architecture.md`:

| # | Section | Update Action |
|---|---------|---------------|
| 1 | Overview | Append new interfaces or technology changes |
| 2 | C4 Diagrams | Add new nodes and edges to existing Mermaid diagrams |
| 3 | Integrations | Append new rows to the integration table |
| 4 | Data Model | Add new entities and relationships to the ER diagram |
| 5 | Critical Flows | Add new sequence diagrams as new subsections |
| 6 | NFRs | Update targets only if changed; never remove rows |
| 7 | Architectural Decisions | Append new ADR references to the table |
| 8 | Observability | Add new metrics, alerts, or dashboard entries |
| 9 | Resilience | Add new circuit breaker, retry, or fallback configurations |
| 10 | Change History | Append a new changelog entry with date, story ID, and summary |

### Change History Entry Format

| Date | Author | Description |
|------|--------|-------------|
| YYYY-MM-DD | AI (story-XXXX-YYYY) | Sections affected: [list]. Summary: [brief description] |

## Input Documents

1. **Architecture Plan:** `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
2. **Service Architecture Doc:** `docs/architecture/service-architecture.md`
3. **Service Architecture Template:** `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`

## Document Creation from Scratch

If `docs/architecture/service-architecture.md` does NOT exist:

1. Read the template from `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`
2. Replace `{{ placeholders }}` with project identity values
3. Populate sections from the architecture plan data
4. Add an initial Change History entry

## Subagent Prompt

Launch a **single** `general-purpose` subagent:

> You are a **Documentation Engineer** performing an incremental update of the
> service architecture document for {{PROJECT_NAME}}.
>
> **Step 1 — Read inputs:**
> - Read the architecture plan at the path provided as argument
> - Read the current service architecture doc at `docs/architecture/service-architecture.md`
> - If `service-architecture.md` does NOT exist, read the template from
>   `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md` and create the initial document
>   by replacing `{{ placeholders }}` with project identity values
>
> **Step 2 — Identify changes:**
> - Compare the architecture plan sections against the current service architecture doc
> - For each section, identify: new components, new integrations, new flows, new ADRs,
>   changed NFRs, new observability or resilience configurations
> - Build a diff list of changes per section
>
> **Step 3 — Apply incremental updates (RULE-008):**
> - For each changed section, INSERT new content at the appropriate location
> - NEVER remove or rewrite existing content
> - For C4 diagrams: add new Mermaid nodes and edges within existing graph blocks
> - For tables: append new rows
> - For sequence diagrams: add new subsections with new diagrams
> - For ADR references: append new rows to the ADR table
>
> **Step 4 — Update Change History:**
> - ALWAYS append a new row to Section 10 (Change History)
> - Include: today's date, story ID, list of sections modified, brief summary
>
> **Step 5 — Save:**
> - Write the updated document to `docs/architecture/service-architecture.md`
> - Report: sections updated, changes applied, new entries added
>
> **Conventions:**
> - All content in English (RULE-012)
> - Mermaid syntax for all diagrams
> - Incremental updates only — never full rewrite (RULE-008)

## Integration with x-dev-lifecycle

When invoked from `x-dev-lifecycle` Phase 3 (Documentation):

1. Check if architecture plan exists at `docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
2. If exists: invoke this skill to update `docs/architecture/service-architecture.md`
3. If not exists: skip with log `"No architecture plan found; skipping architecture doc update"`
4. This step is recommended but not mandatory. Skip does not block the phase.

When invoked standalone:

```
/x-dev-arch-update docs/stories/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
```

## Detailed References

- `.github/skills/architecture/SKILL.md` — architecture principles and patterns
- Template: `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md` (story-0004-0002)
- Sister skill: `x-dev-architecture-plan` — generates the input architecture plans
