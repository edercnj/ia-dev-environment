---
name: x-story-map
description: "Generate an Implementation Map from an Epic and its Stories with dependency matrix, phase computation, critical path analysis, ASCII phase diagrams, Mermaid dependency graphs, phase summary tables, and strategic observations."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "<EPIC_FILE>"
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for technical terms (cache, timeout, handler, endpoint) and code identifiers.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Implementation Map Generator

## Purpose

Take the Epic and all Story files and compute the implementation plan: which stories can run in parallel, what the minimum implementation time is, where the bottlenecks are, and how to optimize team allocation. Produce ASCII phase diagrams, Mermaid dependency graphs, and strategic observations for sprint planning.

## Triggers

- `/x-story-map <epic_file>` — generate implementation map from epic and stories
- User asks to create an implementation map, generate a dependency graph, or compute implementation phases
- User mentions sequencing stories, finding bottlenecks, computing parallel work streams, or building a roadmap from an epic

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `<EPIC_FILE>` | Path | Yes | — | Path to the Epic file (with story index and dependency declarations) |

## Prerequisites

Read the following files before starting:

**Template (output structure):**
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` — The exact structure to follow

**Required inputs:**
- The Epic file (with story index and dependency declarations)
- All Story files (with their Blocked By / Blocks tables)

## Workflow

### Step 1 — Build the Dependency Matrix

Read every story's Section 1 (Dependencias) and the Epic's story index. Also read each
story's `**Chave Jira:**` field (if present). Build a complete matrix:

| Story | Titulo | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |

The `Chave Jira` column is placed between `Titulo` and `Blocked By`. If a story does not
have a Jira key (field is `—` or `<CHAVE-JIRA>`), set the column value to `—`.

**Validation checks:**
- Every story in the Epic's index must appear in the matrix
- Dependencies must be symmetric: if A blocks B, then B must list A as blocker
- No circular dependencies (A->B->C->A is invalid)
- Root stories (no blockers) must exist — if every story has a blocker, something is wrong

If inconsistencies are found, fix them and note the corrections.

Add a `> **Nota:**` block for any implicit dependencies not declared in the stories but
functionally required (e.g., "story-0001-0009 provides configuration data that story-0001-0002 needs even
though story-0001-0002 does not explicitly declare it").

### Step 2 — Compute Phases

Group stories into phases using the dependency DAG:

1. **Phase 0**: All stories with no dependencies (roots)
2. **Phase 1**: Stories whose dependencies are all in Phase 0
3. **Phase N**: Stories whose dependencies are all in Phase 0..N-1

Within each phase, all stories can run in parallel.

Create the ASCII phase diagram using box-drawing characters. Follow the exact style from
the template — `+=+`, `|`, `+=+` for phase boxes, `+-+`, `|`, `+-+` for story boxes,
`|-->`, `v` for arrows.

Each story box shows: ID + short scope description (max ~20 chars).
Each phase box shows: phase number + name + "(paralelo)" if multiple stories.

### Step 3 — Identify the Critical Path

The critical path is the longest chain of dependencies from any root to any leaf.
Count phases, not individual stories.

Render as a simple ASCII diagram:
```
story-0001-0001 -+
            +---> story-0001-0002 -> story-0001-0003 --+
story-0001-0009 -+                              +---> story-0001-0011
                 story-0001-0002 -> story-0001-0010 --+
   Fase 0           Fase 1       Fase 2            Fase 3
```

State: **N phases in the critical path, M stories in the longest chain**.
Explain the impact: any delay in a critical path story directly delays the final delivery.

### Step 4 — Generate the Mermaid Dependency Graph

Create a full `graph TD` with all stories and their dependency edges.

**Naming convention**: `SXXXX_YYYY["story-XXXX-YYYY<br/>Short Title"]`

If Jira keys are available, include them in node labels:
`SXXXX_YYYY["story-XXXX-YYYY (PROJ-123)<br/>Short Title"]`

**Phase coloring** (use these exact classDef values for consistency):
```
classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff
classDef fase2 fill:#533483,stroke:#e94560,color:#fff
classDef fase3 fill:#e94560,stroke:#fff,color:#fff
classDef faseQE fill:#0d7377,stroke:#14ffec,color:#fff
classDef faseTD fill:#2d3436,stroke:#fdcb6e,color:#fff
classDef faseCR fill:#6c5ce7,stroke:#a29bfe,color:#fff
```

Assign classDef by phase. Group edges by phase transition (comment with `%% Fase N -> N+1`).

### Step 5 — Create the Phase Summary Table

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | story-0001-0001, story-0001-0009 | Infra + API | 2 paralelas | — |

Include total count: **N historias em M fases**.

Add notes about transversal phases (QE, Tech Debt) that can execute independently of
business phases.

### Step 6 — Detail Each Phase

For each phase, create a subsection with:

**Table**: Story | Escopo Principal | Artefatos Chave

**Entregas da Fase N** (bullet list of concrete deliverables — what exists after this
phase that did not exist before).

Be specific about artifacts: class names, table names, endpoints, configurations, test
infrastructure.

### Step 7 — Write Strategic Observations

These are the highest-value part of the map. Analyze:

**Gargalo Principal**: Which story blocks the most others? Investing extra time in
it pays off. This is usually the Layer 1 core story.

**Historias Folha (sem dependentes)**: Stories that do not block anything. They can absorb
delays without impacting the critical path. Good candidates for junior developers or
parallel streams.

**Otimizacao de Tempo**: Where is parallelism maximized? Which stories can start immediately?
How should teams be allocated across phases?

**Dependencias Cruzadas**: Stories in later phases that depend on stories from different
branches of the dependency tree. Identify convergence points.

**Marco de Validacao Arquitetural**: Which story should serve as the architectural
checkpoint before expanding scope? What does it validate (patterns, pipeline, integration)?

### Step 8 — Save and Report

Save as `implementation-map-XXXX.md` in the same directory as the Epic and Stories (inside `plans/epic-XXXX/`).
The XXXX is the epic number extracted from the Epic file.
Report: total stories, phases, critical path length, maximum parallelism, main bottleneck.

## Error Handling

| Scenario | Action |
|----------|--------|
| Template file missing | Abort with message: "Template _TEMPLATE-IMPLEMENTATION-MAP.md not found" |
| Epic file missing or unparseable | Abort with message: "Epic file not found or invalid format" |
| Story files missing | Abort with message: "No story files found in epic directory" |
| Circular dependency detected | Warn with cycle details; break the cycle and proceed |
| Asymmetric dependency (A blocks B but B does not list A) | Fix automatically and note the correction |
| No root stories (all have blockers) | Warn: "All stories have blockers — review for missing root stories" |

## Common Mistakes

- **Phase computation error**: A story can only enter a phase when ALL its dependencies (not just some) are in earlier phases
- **Missing convergence analysis**: When story-0001-0011 depends on story-0001-0003 AND story-0001-0010 from different branches, this creates a convergence point that deserves a callout
- **Generic observations**: "story-0001-0002 is important" says nothing. "story-0001-0002 blocks 6 stories and establishes the decision engine pattern that all Phase 2 handlers reuse — investing extra design time here prevents refactoring 6 handlers later" is useful
- **Inconsistent status**: If a story is marked Done in the matrix but Pending in the phase diagram, that is a bug
- **Missing leaf analysis**: Leaf stories (no dependents) are strategically important because they can absorb schedule variance. Always identify them

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-story-epic | reads | Reads the Epic file with story index and dependencies |
| x-story-create | reads | Reads generated story files for dependency details |
| x-story-epic-full | called-by | Orchestrator invokes x-story-map in Phase D |
| x-dev-epic-implement | reads | Epic implementation reads the map for execution order |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| story-planning | Phase computation, dependency DAG, critical path analysis |
