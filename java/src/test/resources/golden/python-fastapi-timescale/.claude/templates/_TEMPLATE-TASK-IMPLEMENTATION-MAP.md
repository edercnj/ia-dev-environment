# Task Implementation Map — {{STORY_ID}}

## Dependency Graph

```mermaid
graph TD
{{MERMAID_NODES}}
{{MERMAID_EDGES}}
```

## Execution Order

| Wave | Tasks (parallelisable) | Blocks |
| :--- | :--- | :--- |
{{WAVE_ROWS}}

## Coalesced Groups

{{COALESCED_GROUPS_OR_DASH}}

## Parallelism Analysis

- Total tasks: {{TOTAL_TASKS}}
- Number of waves: {{NUM_WAVES}}
- Largest wave size: {{LARGEST_WAVE}}
- Estimated speedup vs sequential: {{SPEEDUP}}
