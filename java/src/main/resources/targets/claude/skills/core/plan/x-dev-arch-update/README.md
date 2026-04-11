# x-dev-arch-update

> Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-dev-arch-update [STORY-ID or architecture-plan-path]` |
| **Reads** | architecture |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Keeps the service architecture document (`steering/service-architecture.md`) up to date by applying incremental changes from architecture plans. It never removes or rewrites existing content -- only inserts new components, integrations, flows, ADR references, and changelog entries. If the document does not exist, it bootstraps one from a template.

## Usage

```
/x-dev-arch-update story-0012-0003
/x-dev-arch-update plans/epic-0012/plans/architecture-story-0012-0003.md
```

## Workflow

1. Read the architecture plan and current service architecture document
2. If `service-architecture.md` does not exist, create it from template
3. Identify changes by comparing plan sections against the current document
4. Apply incremental updates (insert new content, never remove existing)
5. Append a changelog entry to Section 10 (Change History)
6. Save the updated document and report sections modified

## Outputs

| Artifact | Path |
|----------|------|
| Service architecture document | `steering/service-architecture.md` |

## See Also

- [x-dev-architecture-plan](../x-dev-architecture-plan/) — Generate the architecture plan consumed by this skill
- [x-dev-adr-automation](../x-dev-adr-automation/) — Expand mini-ADRs into full ADR files
