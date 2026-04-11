# x-epic-create

> Generate an Epic document from a system specification file. This skill reads a technical spec and produces an Epic file with cross-cutting business rules, global quality definitions (DoR/DoD), and a complete story index with dependency declarations.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-epic-create [spec-file-path]` |
| **Reads** | story-planning |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Reads a system specification document and generates the Epic -- the top-level artifact defining scope, cross-cutting rules, quality gates, and story index for a development effort. It extracts business rules that span multiple stories, defines global DoR/DoD criteria, and produces a dependency-aware story index organized by layers (foundation, core, extensions, compositions, cross-cutting). Optionally creates the Epic in Jira via MCP.

## Usage

```
/x-epic-create specs/payment-gateway.md
/x-epic-create steering/system-spec.md
```

## Workflow

1. Read the system specification and the Epic template
2. Extract cross-cutting business rules (rules affecting 2+ stories)
3. Identify stories using layer-by-layer decomposition from the decomposition guide
4. Define global DoR and DoD quality criteria
5. Generate the Epic file following the template structure
6. Optionally create the Epic in Jira (if MCP is available)

## Outputs

| Artifact | Path |
|----------|------|
| Epic file | `plans/epic-XXXX/epic-XXXX.md` |

## See Also

- [x-story-create](../x-story-create/) — Generate detailed story files from the Epic
- [x-epic-map](../x-epic-map/) — Compute implementation phases from the Epic's dependency graph
- [x-epic-decompose](../x-epic-decompose/) — Full orchestrator: epic + stories + implementation map
