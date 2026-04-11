# x-story-create

> Generate detailed User Story files from an Epic and system specification. This skill reads an Epic file and the original system spec, then produces one file per story with full data contracts, Gherkin acceptance criteria, Mermaid sequence diagrams, dependency declarations, and tagged sub-tasks.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-story-create [epic-file-path] [spec-file-path]` |
| **Reads** | story-planning |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Generates self-contained story files that developers can implement without referencing the original spec. Each story includes precise data contracts (typed fields, M/O flags, error codes per RFC 7807), Gherkin acceptance criteria in TPP order (degenerate cases first), Mermaid sequence diagrams, and granular sub-tasks. Stories go through a quality gate (default threshold: 70/100) with automatic refinement before being saved. Optionally creates stories in Jira with dependency links.

## Usage

```
/x-story-create plans/epic-0012/epic-0012.md specs/payment-gateway.md
```

## Workflow

1. Read the Epic (story index, rules table, DoD) and the system specification
2. Generate each story file following the template, in dependency order
3. Build sections: dependencies, data contracts, Gherkin scenarios, diagrams, sub-tasks
4. Run quality gate validation (6 dimensions, weighted scoring)
5. Auto-refine stories below threshold (up to 2 attempts)
6. Save story files and optionally create them in Jira with dependency links

## Outputs

| Artifact | Path |
|----------|------|
| Story files | `plans/epic-XXXX/story-XXXX-YYYY.md` |

## See Also

- [x-epic-create](../x-epic-create/) — Generate the Epic consumed by this skill
- [x-epic-map](../x-epic-map/) — Compute implementation phases from story dependencies
- [x-epic-decompose](../x-epic-decompose/) — Full orchestrator: epic + stories + implementation map
