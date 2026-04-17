# x-epic-decompose

> Complete decomposition of a system specification into an Epic, individual Story files, and an Implementation Map with dependency graph and phased execution plan.

| | |
|---|---|
| **Category** | Orchestrator |
| **Invocation** | `/x-epic-decompose <spec-file>` |
| **Delegates to** | `/x-epic-create`, `/x-story-create`, `/x-epic-map` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Overview

This skill orchestrates the full spec-to-stories decomposition pipeline in a single pass. It reads a system specification document, extracts cross-cutting business rules, identifies stories using a layer-by-layer approach (foundation, core domain, extensions, compositions, cross-cutting), computes dependency phases, and produces three deliverables: an Epic file, individual Story files, and an Implementation Map. Optional Jira integration creates corresponding issues and dependency links automatically.

## Execution Flow

```mermaid
flowchart TD
    START(["/x-epic-decompose spec-file"]) --> PA

    subgraph PA["Phase A -- Analysis"]
        PA1[Read spec file] --> PA2[Read decomposition guide]
        PA2 --> PA3[Extract cross-cutting rules]
        PA3 --> PA4[Identify stories by layer]
        PA4 --> PA5[Map dependencies + compute phases]
        PA5 --> PA6[Identify critical path]
    end

    PA --> PA5J

    subgraph PA5J["Phase A.5 -- Jira Integration Decision"]
        JC1{MCP available?}
        JC1 -->|No| NOJIRA["jiraContext.enabled = false"]
        JC1 -->|Yes| JC2["Ask user: Jira integration?"]
        JC2 -->|All in Jira| FULL["Epic + Stories in Jira"]
        JC2 -->|Epic only| PARTIAL["Epic only in Jira"]
        JC2 -->|Markdown only| NOJIRA
    end

    PA5J --> PB

    subgraph PB["Phase B -- Generate Epic"]
        PB1["Delegate to /x-epic-create"] --> PB2[Generate epic-XXXX.md]
        PB2 --> PB3{Jira enabled?}
        PB3 -->|Yes| PB4["Create Epic issue in Jira"]
        PB3 -->|No| PB5["Set Jira key to --"]
    end

    PB --> PC

    subgraph PC["Phase C -- Generate Stories"]
        PC1["Delegate to /x-story-create"] --> PC2["Generate story-XXXX-YYYY.md (per story)"]
        PC2 --> PC3{Cascade to Jira?}
        PC3 -->|Yes| PC4["Create Story issues in Jira"]
        PC3 -->|No| PC5["Set Jira keys to --"]
    end

    PC --> PD

    subgraph PD["Phase D -- Generate Implementation Map"]
        PD1["Delegate to /x-epic-map"] --> PD2[Dependency matrix + phase diagram]
        PD2 --> PD3[Critical path analysis]
        PD3 --> PD4[Generate IMPLEMENTATION-MAP.md]
    end

    PD --> PD5

    subgraph PD5["Phase D.5 -- Jira Dependency Linking"]
        PD5A{Jira stories exist?}
        PD5A -->|Yes| PD5B["Create Blocks/Blocked-by links"]
        PD5A -->|No| PD5C["Skip linking"]
    end

    PD5 --> PE

    subgraph PE["Phase E -- Validate + Report"]
        PE1["Quality validation (8-point gate)"] --> PE2{All pass?}
        PE2 -->|No| PE3["Fix violations"]
        PE3 --> PE1
        PE2 -->|Yes| PE4["Save artifacts + print summary"]
    end

    PE4 --> DONE(["COMPLETE"])

    style NOJIRA fill:#533483,color:#fff
    style DONE fill:#2d6a4f,color:#fff
    style FULL fill:#16213e,color:#fff
    style PARTIAL fill:#16213e,color:#fff
    style PB5 fill:#533483,color:#fff
    style PC5 fill:#533483,color:#fff
    style PD5C fill:#533483,color:#fff
```

## Phases

| # | Phase | Description | Delegated To |
|---|-------|-------------|--------------|
| A | Analysis | Read spec, extract rules, identify stories by layer, map dependencies, compute phases, find critical path | Inline |
| A.5 | Jira Integration Decision | Check MCP availability, ask user preference, build jiraContext | Inline |
| B | Generate Epic | Determine epic number, extract rules table, build story index, define DoR/DoD, create Jira issue (optional) | `/x-epic-create` |
| C | Generate Stories | For each story: dependencies, data contracts, Mermaid diagrams, Gherkin scenarios, sub-tasks; create Jira issues (optional) | `/x-story-create` |
| D | Generate Implementation Map | Dependency matrix, phase diagram, critical path, Mermaid dependency graph, strategic observations | `/x-epic-map` |
| D.5 | Jira Dependency Linking | Create Blocks/Blocked-by links between Jira issues (best-effort) | Inline (MCP) |
| E | Validate and Report | 8-point quality gate, fix violations, save all artifacts, print summary with metrics | Inline |

## Prerequisites

- Specification file accessible and following the expected template format
- Templates present in `.claude/templates/`: `_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`
- Decomposition guide at `references/decomposition-guide.md` (bundled with this skill)
- Jira MCP tool available (optional -- degrades gracefully if absent)

## Outputs

| Artifact | Path | Description |
|----------|------|-------------|
| Epic | `plans/epic-XXXX/epic-XXXX.md` | Scope, cross-cutting rules, story index, DoR/DoD |
| Stories | `plans/epic-XXXX/story-XXXX-YYYY.md` | One file per story with contracts, Gherkin, diagrams, sub-tasks |
| Implementation Map | `plans/epic-XXXX/IMPLEMENTATION-MAP.md` | Phases, critical path, dependency graph, strategic analysis |
| Jira Issues | Jira project (remote) | Epic and Story issues with parent links and dependency links (optional) |

## Decomposition Layers

The skill follows a layer-by-layer decomposition philosophy when identifying stories from the spec:

| Layer | Name | Content |
|-------|------|---------|
| 0 | Foundation | Infrastructure -- servers, schemas, APIs, protocol adapters |
| 1 | Core Domain | Central operation establishing architectural patterns |
| 2 | Extensions | Additional operations reusing core patterns |
| 3 | Compositions | Stories combining multiple extension capabilities |
| 4 | Cross-Cutting | Testing, observability, security, tech debt |

## See Also

- [x-epic-create](../x-epic-create/SKILL.md) -- Epic generation from a spec
- [x-story-create](../x-story-create/SKILL.md) -- Individual story file generation
- [x-epic-map](../x-epic-map/SKILL.md) -- Implementation map with dependency analysis
- [x-epic-implement](../x-epic-implement/SKILL.md) -- Executes the stories produced by this skill
