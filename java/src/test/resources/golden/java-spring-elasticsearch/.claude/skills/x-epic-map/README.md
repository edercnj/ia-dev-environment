# x-epic-map

> Generate an Implementation Map from an Epic and its Stories. This skill computes implementation phases from the dependency graph, identifies the critical path, produces ASCII phase diagrams, Mermaid dependency graphs, and strategic observations about bottlenecks and parallelism.

| | |
|---|---|
| **Category** | Planning |
| **Invocation** | `/x-epic-map [epic-directory-path]` |
| **Reads** | story-planning |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Computes the optimal implementation order from the story dependency graph. It groups stories into parallelizable phases, identifies the critical path (longest dependency chain), highlights bottleneck stories that block the most downstream work, and provides strategic observations for sprint planning and team allocation. The output includes ASCII phase diagrams, a Mermaid dependency graph with phase coloring, and per-phase deliverable breakdowns.

## Usage

```
/x-epic-map plans/epic-0012/
/x-epic-map plans/epic-0012/epic-0012.md
```

## Workflow

1. Build the dependency matrix from all story files and the Epic index
2. Validate dependency consistency (symmetry, no cycles, roots exist)
3. Compute phases by topological sort of the dependency DAG
4. Identify the critical path and convergence points
5. Generate Mermaid dependency graph with phase-based coloring
6. Write strategic observations (bottlenecks, leaf stories, parallelism, architectural checkpoints)
7. Save the implementation map

## Outputs

| Artifact | Path |
|----------|------|
| Implementation map | `plans/epic-XXXX/implementation-map-XXXX.md` |

## See Also

- [x-epic-create](../x-epic-create/) — Generate the Epic with story index and dependencies
- [x-story-create](../x-story-create/) — Generate the story files consumed by this skill
- [x-epic-implement](../x-epic-implement/) — Execute stories following the implementation map
