---
name: x-arch-plan
description: "Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or feature-name]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Architecture Plan Generator

## Purpose

Generates a comprehensive architecture plan for {{PROJECT_NAME}} with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions.

## Triggers

- `/x-arch-plan STORY-ID` — generate architecture plan from story
- `/x-arch-plan "Feature Name"` — generate from feature description
- `/x-arch-plan plans/epic-XXXX/story-XXXX-YYYY.md` — generate from story file path

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `argument` | String | required | story ID, feature name, or file path | Source for architecture plan generation |

### Decision Tree

Evaluate the change scope to determine the plan level:

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

#### Full Architecture Plan

Apply when **any** of these conditions is true:

- New service or module being created
- New external integration (database, message broker, third-party API)
- Change to a public contract (REST API, gRPC proto, GraphQL schema, event schema)
- Infrastructure topology change (new container, new deployment target, new network policy)

#### Simplified Architecture Plan

Apply when:

- New feature in an existing service **without** contract or infrastructure changes
- Only affected sections are generated (skip unrelated diagrams)

#### Skip Architecture Plan

Apply when:

- Bug fix with no architectural impact
- Internal refactoring (no public API or contract change)
- Documentation-only change

## Workflow

```
1. PRE-CHECK  -> Verify idempotency (RULE-002)
2. TEMPLATE   -> Load template or use inline fallback (RULE-007)
3. READ       -> Read story/feature requirements
4. EVALUATE   -> Determine plan level (Full/Simplified/Skip)
5. CONTEXT    -> Read knowledge packs
6. REVIEW     -> Review existing architecture
7. GENERATE   -> Generate architecture plan document
8. VALIDATE   -> Verify section completeness
```

### Step 1 — Idempotency Pre-Check (RULE-002 — Idempotency)

Before generating an architecture plan, check whether one already exists and is still fresh.

#### Pre-Check Algorithm

```
1. Resolve the expected output path:
   plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md

2. IF the file does NOT exist:
   - Log: "Generating architecture plan for {story}"
   - Proceed to generation

3. IF the file EXISTS:
   a. Compare modification times:
      - story_mtime = mtime of the story file
      - plan_mtime  = mtime of the architecture plan file
   b. IF story_mtime <= plan_mtime:
      - Log: "Reusing existing architecture plan from {date}"
      - STOP — do NOT regenerate
   c. IF story_mtime > plan_mtime:
      - Log: "Regenerating stale architecture plan for {story}"
      - Proceed to generation
```

#### Staleness Decision Table

| Condition | Action | Log Message |
|-----------|--------|-------------|
| Plan does not exist | Generate new | `Generating architecture plan for {story}` |
| Plan exists, story not modified | Reuse existing | `Reusing existing architecture plan from {date}` |
| Plan exists, story modified after plan | Regenerate | `Regenerating stale architecture plan for {story}` |

### Step 2 — Template Loading (RULE-007 — Template Reference)

```
1. Check if `.claude/templates/_TEMPLATE-ARCHITECTURE-PLAN.md` exists
2. IF template exists:
   - Read the template
   - Use its 13 sections as the required output structure
   - Log: "Using architecture plan template v1.0.0"
3. IF template does NOT exist (RULE-012 fallback):
   - Log WARNING: "Template not found, using inline format"
   - Fall back to the inline Output Structure defined below
   - Execution continues normally — no interruption
```

**Template Fallback:** When the template file is missing, the skill degrades gracefully to the inline Output Structure. No interruption occurs. A WARNING is logged.

### Step 3 — Read Story/Feature Requirements

Read the story file or feature description provided as argument. Extract: scope, acceptance criteria, integrations, NFRs, constraints.

### Step 4 — Evaluate Decision Tree

Determine if this requires a Full Plan, Simplified Plan, or Skip:
- If Skip: report "Architecture plan not needed for this change" and stop
- If Full or Simplified: proceed to knowledge pack reading

### Step 5 — Read Knowledge Packs

Read knowledge packs **in order** before generating the architecture plan. For Simplified Plan, read only Architecture KP + KPs relevant to affected sections.

### Step 6 — Review Existing Architecture

- Check for existing architecture documents in `steering/`
- Review current codebase structure to understand the baseline
- Identify what is new vs. what is changing

### Step 7 — Generate Architecture Plan

Launch a **single** `general-purpose` subagent with `model: opus` for deep architectural reasoning (RULE-009). Generate ALL mandatory sections.

### Step 8 — Validate Sections Post-Generation

```
1. Parse the generated document for H2 headings (## Section Name)
2. Count sections found vs. sections expected
3. For each conditional section:
   - IF capability is enabled AND section is missing -> WARNING
   - IF capability is disabled AND section is absent -> OK (conditional skip)
4. For each mandatory section:
   - IF section is missing -> WARNING with instruction to complete
5. Log checklist:
   - All present: "Architecture plan: 13/13 sections present"
   - Some missing: "Architecture plan: X/Y applicable sections present, Z conditional skipped"
```

## Mandatory Sections (13)

| # | Section | Mandatory | Conditional |
|---|---------|-----------|-------------|
| 1 | Header | Yes | -- |
| 2 | Executive Summary | Yes | -- |
| 3 | Component Diagram | Yes | -- |
| 4 | Sequence Diagrams | Yes | -- |
| 5 | Deployment Diagram | Yes | orchestrator != none |
| 6 | External Connections | Yes | -- |
| 7 | Architecture Decisions | Yes | -- |
| 8 | Technology Stack | Yes | -- |
| 9 | Non-Functional Requirements | Yes | -- |
| 10 | Data Model | Yes | database != none |
| 11 | Observability Strategy | Yes | -- |
| 12 | Resilience Strategy | Yes | -- |
| 13 | Impact Analysis | Yes | -- |

### Conditional Section Rules

- **Deployment Diagram**: Include when `orchestrator != none`. When not applicable, write `N/A - capability not enabled`.
- **Data Model**: Include when `database != none`. When not applicable, write `N/A - capability not enabled`.
- **Event Schema**: Include when `eventDriven: true`. When not applicable, write `N/A - capability not enabled`.

## Output Structure

| # | Section | Type | Format | Description |
|---|---------|------|--------|-------------|
| 1 | `# Architecture Plan — {title}` | M | Markdown H1 | Title with story ID or feature name |
| 2 | `## Executive Summary` | M | Paragraph | One-paragraph overview of the architecture |
| 3 | `## Component Diagram` | M | Mermaid `graph TD` | Components, layers, and dependency arrows |
| 4 | `## Sequence Diagrams` | M | Mermaid `sequenceDiagram` | Main flows (happy path + primary error path) |
| 5 | `## Deployment Diagram` | M | Mermaid `graph TD` | Infrastructure nodes, networking, load balancers |
| 6 | `## External Connections` | M | Markdown table | System, Protocol, Purpose, SLO columns |
| 7 | `## Architecture Decisions` | M | Mini-ADR format | Key decisions with Context/Decision/Rationale |
| 8 | `## Technology Stack` | M | Markdown table | Component, Technology, Version, Rationale columns |
| 9 | `## Non-Functional Requirements` | M | Markdown table | Metric, Target, Measurement columns |
| 10 | `## Data Model` | O | Mermaid `erDiagram` or table | Entities and relationships (if applicable) |
| 11 | `## Observability Strategy` | M | Markdown section | Key metrics, trace spans, log patterns, alerts |
| 12 | `## Resilience Strategy` | M | Markdown section | Circuit breakers, retries, fallbacks, degradation |
| 13 | `## Impact Analysis` | M | Markdown section | Affected services, migration plan, rollback strategy |

**Minimum mandatory sections:** 12 (all M sections).

### Output Path

```
plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
```

Where `XXXX` is the epic ID and `YYYY` is the story sequence number extracted from the story ID.

## Mini-ADR Format

Each architectural decision is documented inline using this simplified format:

```markdown
### ADR-NNN: {Decision Title}

**Status:** Proposed | Accepted | Deprecated | Superseded

**Context:**
{What is the issue or force motivating this decision? 2-3 sentences.}

**Decision:**
{What is the change being proposed or adopted? 1-2 sentences.}

**Rationale:**
{Why was this option chosen over alternatives? List key trade-offs.}

**Consequences:**
- Positive: {benefits}
- Negative: {trade-offs or risks}

**Story Reference:** {STORY-ID}
```

**Rules:**
- Number ADRs sequentially within the document (ADR-001, ADR-002, ...)
- Each ADR must have a Story Reference linking back to the originating story
- Status must be one of: Proposed, Accepted, Deprecated, Superseded
- Context and Decision are mandatory; Consequences may be brief for simple decisions

## Subagent Prompt

Launch a **single** `general-purpose` subagent with `model: opus` for deep architectural reasoning (RULE-009):

> You are a **Senior Architect** generating a comprehensive architecture plan for {{PROJECT_NAME}}.
>
> **Step 1 — Read the template (RULE-007):**
> - Read template at `.claude/templates/_TEMPLATE-ARCHITECTURE-PLAN.md` for required output format
> - If the template file does not exist, use the inline Output Structure defined in this skill as fallback (RULE-012)
> - Log which format source is being used
>
> **Step 2 — Read the story/feature requirements:**
> - Read the story file or feature description provided as argument
> - Extract: scope, acceptance criteria, integrations, NFRs, constraints
>
> **Step 3 — Evaluate the decision tree:**
> - Determine if this requires a Full Plan, Simplified Plan, or Skip
> - If Skip: report "Architecture plan not needed for this change" and stop
>
> **Step 4 — Read knowledge packs (in order):**
> - `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - `skills/architecture/references/architecture-patterns.md` — design patterns
> - `skills/protocols/references/` — protocol conventions for the project's interfaces
> - `skills/security/references/` — OWASP, headers, secrets
> - `skills/observability/references/` — tracing, metrics, logging
> - `skills/infrastructure/references/` — Docker, K8s, 12-factor
> - `skills/resilience/references/` — circuit breaker, retry, fallback
> - `skills/compliance/references/` — only if compliance frameworks are active
>
> For Simplified Plan: read only Architecture KP + KPs relevant to affected sections.
>
> **Step 5 — Review existing architecture:**
> - Check for existing architecture documents in `steering/`
> - Review current codebase structure to understand the baseline
> - Identify what is new vs. what is changing
>
> **Step 6 — Generate the architecture plan** with ALL mandatory sections:
>
> 1. **Executive Summary** — one paragraph overview
> 2. **Component Diagram** — Mermaid `graph TD` showing layers and dependencies
> 3. **Sequence Diagrams** — Mermaid `sequenceDiagram` for main flows
> 4. **Deployment Diagram** — Mermaid `graph TD` with infrastructure nodes
> 5. **External Connections** — table: System | Protocol | Purpose | SLO
> 6. **Architecture Decisions** — mini-ADRs (Context/Decision/Rationale per decision)
> 7. **Technology Stack** — table: Component | Technology | Version | Rationale
> 8. **Non-Functional Requirements** — table: Metric | Target | Measurement
> 9. **Data Model** — erDiagram or table (if applicable, skip if no data changes)
> 10. **Observability Strategy** — key metrics, trace spans, structured log fields, alerts
> 11. **Resilience Strategy** — circuit breakers, retry policies, fallback chains, graceful degradation
> 12. **Impact Analysis** — affected services, migration steps, rollback strategy, risk assessment
>
> **Step 7 — Save the document:**
> ```
> plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
> ```
>
> **Step 8 — Validate sections post-generation:**
> - Parse the generated document for all 13 expected sections
> - For conditional sections (Deployment Diagram, Data Model, Event Schema):
>   check project capabilities and mark as "N/A - capability not enabled" if not applicable
> - Log section checklist: "Architecture plan: X/Y sections present"
> - If any mandatory section is missing, log a WARNING with instruction to complete
>
> **Conventions:**
> - All diagrams MUST use Mermaid syntax
> - All text MUST be in English
> - Mini-ADRs follow the inline format (Context/Decision/Rationale/Consequences/Story Reference)
> - Tables use GitHub-flavored Markdown
> - NFR targets must be measurable (e.g., "p99 latency < 200ms", not "fast")

## Knowledge Pack References

| # | Knowledge Pack | Path | Purpose |
|---|----------------|------|---------|
| 1 | Architecture | `skills/architecture/references/architecture-principles.md` | Layer structure, dependency direction, package conventions |
| 2 | Architecture Patterns | `skills/architecture/references/architecture-patterns.md` | Design patterns, CQRS, event sourcing, saga |
| 3 | Protocols | `skills/protocols/references/` | REST, gRPC, GraphQL, WebSocket, event-driven conventions |
| 4 | Security | `skills/security/references/` | OWASP Top 10, security headers, secrets management |
| 5 | Observability | `skills/observability/references/` | Tracing, metrics, logging, health checks |
| 6 | Infrastructure | `skills/infrastructure/references/` | Docker, Kubernetes, 12-factor app |
| 7 | Resilience | `skills/resilience/references/` | Circuit breaker, retry, fallback, bulkhead |
| 8 | Compliance | `skills/compliance/references/` | GDPR, HIPAA, LGPD (if compliance is active) |

**Minimum KPs for Full Plan:** 7 (all except Compliance, which is conditional).
**Minimum KPs for Simplified Plan:** 1 (Architecture only; add others as relevant).

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort with "Story file not found: {path}" |
| Template not found | Log WARNING, fall back to inline Output Structure (RULE-012) |
| Knowledge pack not found | Log WARNING, continue with available KPs |
| Subagent fails | Abort with error details, suggest manual generation |
| Post-validation finds missing mandatory section | Log WARNING with instruction to complete the section |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-story-implement | Called by | Invoked during Phase 1 for architecture planning; pre-check ensures idempotency |
| x-threat-model | Complements | Architecture plan feeds threat model generation |
| x-task-implement | Consumed by | Implementation phase reads architecture plan alongside implementation plan |
| x-arch-update | Followed by | After implementation, architecture document is updated incrementally |

## Detailed References

For in-depth guidance on architecture patterns, consult:
- `skills/architecture/SKILL.md` — full architecture reference
- `skills/protocols/SKILL.md` — protocol conventions
- `skills/security/SKILL.md` — security standards
- `skills/observability/SKILL.md` — observability patterns
- `skills/resilience/SKILL.md` — resilience patterns
- `skills/infrastructure/SKILL.md` — infrastructure patterns
