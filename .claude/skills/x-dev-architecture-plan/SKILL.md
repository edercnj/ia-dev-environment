---
name: x-dev-architecture-plan
description: "Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or feature-name]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Architecture Plan Generator

## When to Use

Evaluate the change scope using this decision tree:

### Full Architecture Plan

Apply when **any** of these conditions is true:

- New service or module being created
- New external integration (database, message broker, third-party API)
- Change to a public contract (REST API, gRPC proto, GraphQL schema, event schema)
- Infrastructure topology change (new container, new deployment target, new network policy)

### Simplified Architecture Plan

Apply when:

- New feature in an existing service **without** contract or infrastructure changes
- Only affected sections are generated (skip unrelated diagrams)

### Skip Architecture Plan

Apply when:

- Bug fix with no architectural impact
- Internal refactoring (no public API or contract change)
- Documentation-only change

**Decision Tree Summary:**

| Condition | Plan Level |
|-----------|-----------|
| New service / new integration / contract change / infra change | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

## Knowledge Packs

Read these knowledge packs **in order** before generating the architecture plan:

| # | Knowledge Pack | Path | Purpose |
|---|---------------|------|---------|
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

## Output Structure

The architecture plan document MUST contain these sections. Mandatory sections are marked **(M)**, optional sections are marked **(O)**.

### Section Checklist

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

Launch a **single** `general-purpose` subagent with the following prompt:

> You are a **Senior Architect** generating a comprehensive architecture plan for {{PROJECT_NAME}}.
>
> **Step 1 — Read the story/feature requirements:**
> - Read the story file or feature description provided as argument
> - Extract: scope, acceptance criteria, integrations, NFRs, constraints
>
> **Step 2 — Evaluate the decision tree:**
> - Determine if this requires a Full Plan, Simplified Plan, or Skip
> - If Skip: report "Architecture plan not needed for this change" and stop
>
> **Step 3 — Read knowledge packs (in order):**
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
> **Step 4 — Review existing architecture:**
> - Check for existing architecture documents in `steering/`
> - Review current codebase structure to understand the baseline
> - Identify what is new vs. what is changing
>
> **Step 5 — Generate the architecture plan** with ALL mandatory sections:
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
> **Step 6 — Save the document:**
> ```
> plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
> ```
>
> **Conventions:**
> - All diagrams MUST use Mermaid syntax (RULE-007)
> - All text MUST be in English (RULE-012)
> - Mini-ADRs follow the inline format (Context/Decision/Rationale/Consequences/Story Reference)
> - Tables use GitHub-flavored Markdown
> - NFR targets must be measurable (e.g., "p99 latency < 200ms", not "fast")

## Integration with x-dev-lifecycle

When invoked from `x-dev-lifecycle` Phase 1:

1. The lifecycle orchestrator passes the story path as argument
2. This skill evaluates the decision tree and generates the appropriate plan level
3. The output path follows the standard convention: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
4. Phase 2 (Implementation) reads this document alongside the implementation plan

When invoked standalone:

```
/x-dev-architecture-plan plans/epic-XXXX/story-XXXX-YYYY.md
```

Or with a feature name:

```
/x-dev-architecture-plan "Payment Gateway Integration"
```

## Detailed References

For in-depth guidance on architecture patterns, consult:
- `skills/architecture/SKILL.md` — full architecture reference
- `skills/protocols/SKILL.md` — protocol conventions
- `skills/security/SKILL.md` — security standards
- `skills/observability/SKILL.md` — observability patterns
- `skills/resilience/SKILL.md` — resilience patterns
- `skills/infrastructure/SKILL.md` — infrastructure patterns
