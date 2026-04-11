# x-arch-plan

> Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-arch-plan [STORY-ID or feature-name]` |
| **Reads** | architecture, protocols, security, observability, infrastructure, resilience, compliance |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Produces a structured architecture plan with 13 sections covering component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience strategies. It evaluates change scope via a decision tree (Full/Simplified/Skip) and includes an idempotency pre-check to avoid redundant regeneration when the story has not changed.

## Usage

```
/x-arch-plan plans/epic-0012/story-0012-0003.md
/x-arch-plan "Payment Gateway Integration"
```

## Workflow

1. Check for existing architecture plan and assess staleness (idempotency pre-check)
2. Read template for output format, then evaluate decision tree (Full/Simplified/Skip)
3. Subagent reads 7-8 knowledge packs and the story requirements
4. Review existing architecture documents and codebase structure
5. Generate the architecture plan with all mandatory sections (Mermaid diagrams, mini-ADRs, NFRs)
6. Validate section completeness post-generation

## Outputs

| Artifact | Path |
|----------|------|
| Architecture plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` |

## See Also

- [x-arch-update](../x-arch-update/) — Incrementally update service architecture doc from plans
- [x-adr-generate](../x-adr-generate/) — Expand mini-ADRs into full ADR files
- [x-task-implement](../x-task-implement/) — Implement the feature using the architecture plan
