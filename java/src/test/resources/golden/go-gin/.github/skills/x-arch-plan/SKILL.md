---
name: x-arch-plan
description: >
  Generates a comprehensive architecture plan with component diagrams, sequence
  diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability
  strategies. Use before implementation to document design decisions. Evaluates
  a decision tree (Full/Simplified/Skip) based on change scope.
---

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
| 1 | Architecture | `.github/skills/architecture/SKILL.md` | Layer structure, dependency direction, package conventions |
| 2 | Protocols | `.github/skills/protocols/SKILL.md` | REST, gRPC, GraphQL, WebSocket, event-driven conventions |
| 3 | Security | `.github/skills/security/SKILL.md` | OWASP Top 10, security headers, secrets management |
| 4 | Observability | `.github/skills/observability/SKILL.md` | Tracing, metrics, logging, health checks |
| 5 | Infrastructure | `.github/skills/dockerfile/SKILL.md` | Dockerfile authoring, image structure, multi-stage builds |
| 6 | Resilience | `.github/skills/resilience/SKILL.md` | Circuit breaker, retry, fallback, bulkhead |
| 7 | Compliance | `.github/skills/compliance/SKILL.md` | GDPR, HIPAA, LGPD (if compliance is active) |

**Minimum KPs for Full Plan:** 6 (all except Compliance, which is conditional).
**Minimum KPs for Simplified Plan:** 1 (Architecture only; add others as relevant).

## Output Structure

The architecture plan document MUST contain these sections:

| # | Section | Required | Format |
|---|---------|----------|--------|
| 1 | `# Architecture Plan — {title}` | Yes | Markdown H1 |
| 2 | `## Executive Summary` | Yes | Paragraph |
| 3 | `## Component Diagram` | Yes | Mermaid `graph TD` |
| 4 | `## Sequence Diagrams` | Yes | Mermaid `sequenceDiagram` |
| 5 | `## Deployment Diagram` | Yes | Mermaid `graph TD` |
| 6 | `## External Connections` | Yes | Markdown table |
| 7 | `## Architecture Decisions` | Yes | Mini-ADR format |
| 8 | `## Technology Stack` | Yes | Markdown table |
| 9 | `## Non-Functional Requirements` | Yes | Markdown table |
| 10 | `## Data Model` | No | Mermaid erDiagram |
| 11 | `## Observability Strategy` | Yes | Markdown section |
| 12 | `## Resilience Strategy` | Yes | Markdown section |
| 13 | `## Impact Analysis` | Yes | Markdown section |

### Output Path

```
plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md
```

## Mini-ADR Format

Each architectural decision is documented inline:

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

## Subagent Prompt

Launch a **single** `general-purpose` subagent:

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
> - `.github/skills/architecture/SKILL.md` — architecture principles
> - `.github/skills/protocols/SKILL.md` — protocol conventions
> - `.github/skills/security/SKILL.md` — security standards
> - `.github/skills/observability/SKILL.md` — observability patterns
> - `.github/skills/resilience/SKILL.md` — resilience patterns
> - `.github/skills/dockerfile/SKILL.md` — infrastructure and deployment topology
> - `.github/skills/compliance/SKILL.md` — only if compliance frameworks are active
>
> For Simplified Plan: read only Architecture KP + KPs relevant to affected sections.
>
> **Step 4 — Review existing architecture** in `steering/` and current codebase structure.
>
> **Step 5 — Generate the architecture plan** with ALL mandatory sections:
> 1. Executive Summary
> 2. Component Diagram (Mermaid graph TD)
> 3. Sequence Diagrams (Mermaid sequenceDiagram)
> 4. Deployment Diagram (Mermaid graph TD)
> 5. External Connections (table)
> 6. Architecture Decisions (mini-ADRs)
> 7. Technology Stack (table)
> 8. Non-Functional Requirements (table)
> 9. Data Model (if applicable)
> 10. Observability Strategy
> 11. Resilience Strategy
> 12. Impact Analysis
>
> **Step 6 — Save** to `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`

## Integration with x-story-implement

When invoked from `x-story-implement` Phase 1, the lifecycle orchestrator passes the story path.
When invoked standalone: `/x-arch-plan [STORY-ID or feature-name]`

## Detailed References

For in-depth guidance on architecture patterns, consult:
- `.github/skills/architecture/SKILL.md`
- `.github/skills/protocols/SKILL.md`
- `.github/skills/security/SKILL.md`
- `.github/skills/observability/SKILL.md`
- `.github/skills/resilience/SKILL.md`
