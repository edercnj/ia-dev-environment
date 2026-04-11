# Skill README Templates — Internal Reference

> This file defines the 6 standardized README templates for skills.
> Each skill directory MUST have a `README.md` following the appropriate template.
> READMEs complement SKILL.md (executable spec) with human-readable documentation.

> **Skill taxonomy — EPIC-0036.**
> The source-of-truth skills directory is organized into category subfolders. Skills live as `core/{category}/{skill-name}/SKILL.md` and `conditional/{category}/{skill-name}/SKILL.md` where `{category}` is one of 10 taxonomy buckets (`plan/`, `dev/`, `test/`, `review/`, `security/`, `code/`, `git/`, `pr/`, `ops/`, `jira/`). `core/lib/` retains its legacy nested layout. `knowledge-packs/` remains flat.
>
> **Invocation is unaffected:** the generated output `.claude/skills/` remains flat. `SkillsAssembler` flattens the hierarchy at assembly time, so users invoke skills as `/{skill-name}` without a category prefix. All user-invocable skills follow the `x-{subject}-{action}` naming convention.
>
> **When adding a new skill:** place it under the matching category subfolder for either `core/` or `conditional/`. See [`adr/ADR-0003-skill-taxonomy-and-naming.md`](../../../../../../../adr/ADR-0003-skill-taxonomy-and-naming.md) for the authoritative taxonomy and naming conventions.

---

## Template 1: Orchestrator

**Applies to:** Skills that delegate to multiple sub-skills via subagents (4 skills).
**Target length:** 80-150 lines. Mermaid diagrams mandatory.

```markdown
# {skill-name}

> {one-line description}

| | |
|---|---|
| **Category** | Orchestrator |
| **Invocation** | `/{skill-name} {args}` |
| **Delegates to** | {comma-separated list of delegated skills} |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Overview

{2-3 sentence overview of what this orchestrator does and why it exists}

## Execution Flow

` ` `mermaid
{high-level flowchart with max 15-20 nodes}
` ` `

## Phases

| # | Phase | Description | Delegated To |
|---|-------|-------------|--------------|
| 0 | {name} | {description} | {skill or "inline"} |

## Flags

| Flag | Default | Effect |
|------|---------|--------|
| --{flag} | {default} | {effect} |

## Prerequisites

- {prerequisite 1}
- {prerequisite 2}

## Outputs

| Artifact | Path | Description |
|----------|------|-------------|
| {name} | {path} | {description} |

## See Also

- [{related-skill}](../{related-skill}/) — {one-line description}
```

---

## Template 2: Execution Skill

**Applies to:** User-invocable, single-purpose skills (31+ skills).
**Target length:** 30-60 lines. No Mermaid — workflow as numbered list.

```markdown
# {skill-name}

> {one-line description}

| | |
|---|---|
| **Category** | {subcategory: Planning, Implementation, Review, Testing, Security, Operations, Git/Release, Jira Integration, Documentation} |
| **Invocation** | `/{skill-name} {args}` |
| **Reads** | {knowledge packs referenced, or omit row if none} |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

{2-4 sentences focusing on user value, not mechanics}

## Usage

` ` `
/{skill-name}
/{skill-name} {example-arg-1}
/{skill-name} {example-arg-2}
` ` `

## Workflow

1. {step 1 — one line}
2. {step 2 — one line}
3. {step 3 — one line}

## Outputs

| Artifact | Path |
|----------|------|
| {name} | {path} |

## See Also

- [{related-skill}](../{related-skill}/) — {one-line description}
```

**Notes:**
- `Outputs` section: OPTIONAL. Only include if the skill produces files. Omit for terminal-only output.
- `Reads` row in metadata: OPTIONAL. Only include if the skill reads knowledge packs.
- Subcategory values: Planning, Implementation, Review, Testing, Security, Operations, Git/Release, Jira Integration, Documentation.

---

## Template 3: Library Skill

**Applies to:** Internal utilities called by other skills, never user-invoked (3 skills).
**Target length:** 25-40 lines. No Mermaid.

```markdown
# {skill-name}

> {one-line description}

| | |
|---|---|
| **Category** | Library (internal) |
| **Called by** | {comma-separated list of caller skills} |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Purpose

{1-2 sentences explaining what this utility provides}

## Integration Points

| Caller | Context | Input | Output |
|--------|---------|-------|--------|
| {skill} | {when/why it calls this} | {what it passes} | {what it receives} |

## Procedure

1. {step 1}
2. {step 2}
3. {step 3}

## See Also

- [{related-skill}](../{related-skill}/) — {one-line description}
```

---

## Template 4: Conditional Skill

**Applies to:** Feature-gated skills included based on project configuration (22 skills).
**Target length:** 25-50 lines.

```markdown
# {skill-name}

> {one-line description}

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `{config.flag.name}` |
| **Invocation** | `/{skill-name} {args}` |
| **Reads** | {knowledge packs referenced, or omit row if none} |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `{condition description}` in the project configuration.

## What It Does

{2-4 sentences focusing on user value}

## Usage

` ` `
/{skill-name}
/{skill-name} {example-arg}
` ` `

## See Also

- [{related-skill}](../{related-skill}/) — {one-line description}
```

---

## Template 5: Knowledge Pack

**Applies to:** Non-invocable reference material loaded by agents and skills (29 packs).
**Target length:** 25-40 lines. No Mermaid.

```markdown
# {pack-name}

> {one-line description}

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | {comma-separated list of skills/agents that read this pack} |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- {topic 1}
- {topic 2}
- {topic 3}

## Key Concepts

{3-5 sentence summary of the most important concepts in this pack}

## See Also

- [{related-pack}](../{related-pack}/) — {one-line description}
```

---

## Template 6: Stack/Infra Pattern

**Applies to:** Framework-specific or infrastructure-specific knowledge packs (18 packs).
**Target length:** 15-25 lines. Minimal — these are highly repetitive.

```markdown
# {pattern-name}

> {one-line description}

| | |
|---|---|
| **Category** | {Stack or Infrastructure} Pattern |
| **Supplements** | {parent knowledge pack this specializes} |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- {pattern 1}
- {pattern 2}
- {pattern 3}
- {pattern 4}

## See Also

- [{related-pattern}](../{related-pattern}/) — {one-line description}
```

---

## Conventions

1. **No frontmatter** — README.md files do not use YAML frontmatter (SKILL.md has metadata).
2. **No duplication** — Do not repeat SKILL.md content. Summarize and reference.
3. **Relative links** — Use `../skill-name/` for cross-references within the skills directory.
4. **English only** — All README content in English.
5. **Template placeholders** — Use `{{PLACEHOLDER}}` syntax for values resolved by the template engine at generation time.
6. **Mermaid styling** — Orchestrator diagrams use consistent color classes:
   - Green (`#2d6a4f`): success/complete states
   - Red (`#e94560`): failure/error states
   - Purple (`#533483`): skip/special states
   - Yellow (`#e9c46a`): partial/warning states
   - Dark blue (`#16213e`): normal skill nodes
