# x-adr-generate

> Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-adr-generate [architecture-plan-path] [story-id]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Extracts mini-ADRs embedded in architecture plans (marked with `### ADR:`) and expands each into a full Architecture Decision Record with YAML frontmatter, context, decision, and consequences sections. It handles sequential numbering, duplicate detection, index maintenance, and bidirectional cross-referencing between stories, plans, and ADRs.

## Usage

```
/x-adr-generate plans/epic-0012/plans/architecture-story-0012-0003.md story-0012-0003
```

## Workflow

1. Parse the architecture plan for `### ADR:` markers and extract mini-ADR fields
2. Scan existing ADRs in `adr/` to determine next sequential number
3. Check for duplicates by comparing normalized titles against existing ADRs
4. Expand each non-duplicate mini-ADR to full ADR format with YAML frontmatter
5. Write ADR files and update `adr/README.md` index
6. Add cross-references between ADRs, architecture plan, and service architecture document

## Outputs

| Artifact | Path |
|----------|------|
| ADR files | `adr/ADR-NNNN-title-in-kebab-case.md` |
| ADR index | `adr/README.md` |

## See Also

- [x-arch-plan](../x-arch-plan/) — Generates architecture plans containing mini-ADRs
- [x-arch-update](../x-arch-update/) — Updates service architecture doc with ADR references
