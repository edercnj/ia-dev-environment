---
name: x-dev-arch-update
description: "Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or architecture-plan-path]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Service Architecture Document Updater

## Purpose

Incrementally updates the service architecture document (`steering/service-architecture.md`) with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content.

## Triggers

- `/x-dev-arch-update [STORY-ID]` — update architecture doc from the architecture plan for the given story
- `/x-dev-arch-update [architecture-plan-path]` — update architecture doc from the specified plan file
- After a feature is implemented and an architecture plan exists
- When Phase 3 (Documentation) of `x-dev-lifecycle` is executing
- SKIP when no architecture plan exists for the feature

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `STORY-ID` | String | Yes (or path) | Story identifier to locate the architecture plan |
| `architecture-plan-path` | String | Yes (or ID) | Direct path to the architecture plan file |

## Workflow

### Step 1 — Read Inputs

1. Read the architecture plan at the path provided as argument
2. Read the current service architecture doc at `steering/service-architecture.md`
3. If `service-architecture.md` does NOT exist, read the template from `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md` and create the initial document by replacing `{{ placeholders }}` with project identity values

### Step 2 — Identify Changes

1. Compare the architecture plan sections against the current service architecture doc
2. For each section, identify: new components, new integrations, new flows, new ADRs, changed NFRs, new observability or resilience configurations
3. Build a diff list of changes per section

### Step 3 — Apply Incremental Updates (RULE-008 — Content Preservation)

For each changed section, INSERT new content at the appropriate location. NEVER remove or rewrite existing content.

| # | Section Heading | Update Action |
|---|-----------------|---------------|
| 1 | 1. Overview | Append new interfaces or technology changes |
| 2 | 2. C4 Diagrams | Add new nodes and edges to existing Mermaid diagrams |
| 3 | 3. Integrations | Append new rows to the integration table |
| 4 | 4. Data Model | Add new entities and relationships to the ER diagram |
| 5 | 5. Critical Flows | Add new sequence diagrams as new subsections |
| 6 | 6. NFRs | Update targets only if changed; never remove rows |
| 7 | 7. Architectural Decisions | Append new ADR references to the table |
| 8 | 8. Observability | Add new metrics, alerts, or dashboard entries |
| 9 | 9. Resilience | Add new circuit breaker, retry, or fallback configurations |
| 10 | 10. Change History | Append a new changelog entry with date, story ID, and summary |

#### Content Preservation Rules

- NEVER remove existing content from any section
- NEVER rewrite a section — only insert or append
- If a component was refactored: mark old as deprecated, add new
- If a flow changed: add new version, mark previous as "v1"
- Merge conflicts: preserve both versions

#### Change History Entry Format

| Date | Author | Description |
|------|--------|-------------|
| YYYY-MM-DD | AI (story-XXXX-YYYY) | Sections affected: [list]. Summary: [brief description] |

### Step 4 — Update Change History

ALWAYS append a new row to Section 10 (Change History). Include: today's date, story ID, list of sections modified, brief summary.

### Step 5 — Save

1. Write the updated document to `steering/service-architecture.md`
2. Report: sections updated, changes applied, new entries added

## Document Creation from Scratch

If `steering/service-architecture.md` does NOT exist:

1. Read the template from `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`
2. Replace `{{ placeholders }}` with project identity values
3. Populate sections from the architecture plan data
4. Add an initial Change History entry

## Template Fallback

If `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md` is not found, create a minimal service architecture document with 10 numbered sections and populate from the architecture plan. Log a warning: `"Template not found; using inline fallback structure."`

## Subagent Prompt

Launch a **single** `general-purpose` subagent:

> You are a **Documentation Engineer** performing an incremental update of the
> service architecture document for {{PROJECT_NAME}}.
>
> **Step 1 — Read inputs:**
> - Read the architecture plan at the path provided as argument
> - Read the current service architecture doc at `steering/service-architecture.md`
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
> - Write the updated document to `steering/service-architecture.md`
> - Report: sections updated, changes applied, new entries added
>
> **Conventions:**
> - All content in English (RULE-012)
> - Mermaid syntax for all diagrams
> - Incremental updates only — never full rewrite (RULE-008)

## Error Handling

| Scenario | Action |
|----------|--------|
| Architecture plan not found | Skip with log: "No architecture plan found; skipping architecture doc update" |
| Service architecture doc not found | Create from template (see Document Creation from Scratch) |
| Template not found | Use inline fallback structure, log warning |
| Section heading mismatch | Match by numeric prefix, warn if heading text differs |
| Merge conflict in Mermaid diagram | Preserve both versions, add comment marking conflict |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-lifecycle` | called-by | Invoked during Phase 3 (Documentation) to update architecture doc |
| `x-dev-architecture-plan` | reads | Generates the input architecture plans consumed by this skill |
| `x-dev-adr-automation` | calls | May trigger ADR generation for new architectural decisions |
| `architecture` | reads | References architecture principles and patterns KP |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| architecture | `skills/architecture/SKILL.md` | Architecture principles and patterns |
